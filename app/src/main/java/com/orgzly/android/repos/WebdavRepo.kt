package com.orgzly.android.repos

import android.net.Uri
import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.DispatchingAuthenticator
import com.burgstaller.okhttp.basic.BasicAuthenticator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.Credentials
import com.burgstaller.okhttp.digest.DigestAuthenticator
import com.orgzly.android.BookName
import com.orgzly.android.util.UriUtils
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration


class WebdavRepo(
        private val repoId: Long,
        private val uri: Uri,
        private val username: String,
        private val password: String,
        private val certificates: String? = null
) : SyncRepo {

    private val sardine by lazy {
        OkHttpSardine(okHttpClient())
    }

    private fun okHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        // Use certificate if specified
        if (!certificates.isNullOrEmpty()) {
            val trustManager = trustManagerForCertificates(certificates)

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), null)
            }

            val sslSocketFactory = sslContext.socketFactory

            builder.sslSocketFactory(sslSocketFactory, trustManager)
        }

        val credentials = Credentials(username, password)

        val basicAuthenticator = BasicAuthenticator(credentials)
        val digestAuthenticator = DigestAuthenticator(credentials)

        val authenticator = DispatchingAuthenticator.Builder()
            .with("digest", digestAuthenticator)
            .with("basic", basicAuthenticator)
            .build()

        val authCache: Map<String, CachingAuthenticator> = ConcurrentHashMap()

        builder.authenticator(CachingAuthenticatorDecorator(authenticator, authCache))
        builder.addInterceptor(AuthenticationCacheInterceptor(authCache))

        // Double the values as some users are seeing timeouts.
        // Make configurable if needed (https://github.com/orgzly/orgzly-android/issues/870).
        builder.connectTimeout(20, TimeUnit.SECONDS)
        builder.readTimeout(20, TimeUnit.SECONDS)
        builder.writeTimeout(20, TimeUnit.SECONDS)

        return builder.build()
    }

    private fun trustManagerForCertificates(str: String): X509TrustManager {
        // Read certificates
        val certificates = Buffer().writeUtf8(str).inputStream().use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificates(stream)
        }

//        require(!certificates.isEmpty()) {
//            "Expected non-empty set of trusted certificates"
//        }

        // Create new key store
        val password = "password".toCharArray() // Any password will work
        val keyStore = newEmptyKeyStore(password)
        for ((index, certificate) in certificates.withIndex()) {
            val certificateAlias = index.toString()
            keyStore.setCertificateEntry(certificateAlias, certificate)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
        ).apply {
            init(keyStore)
        }

        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Unexpected default trust managers: ${Arrays.toString(trustManagers)}"
        }

        return trustManagers[0] as X509TrustManager
    }

    private fun newEmptyKeyStore(password: CharArray): KeyStore {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        val inputStream: InputStream? = null // By convention, 'null' creates an empty key store.
        keyStore.load(inputStream, password)
        return keyStore
    }

    companion object {
        const val USERNAME_PREF_KEY = "username"
        const val PASSWORD_PREF_KEY = "password"
        const val CERTIFICATES_PREF_KEY = "certificates"

        fun getInstance(repoWithProps: RepoWithProps): WebdavRepo {
            val id = repoWithProps.repo.id

            val uri = Uri.parse(repoWithProps.repo.url)

            val username = checkNotNull(repoWithProps.props[USERNAME_PREF_KEY]) {
                "Username not found"
            }.toString()

            val password = checkNotNull(repoWithProps.props[PASSWORD_PREF_KEY]) {
                "Password not found"
            }.toString()

            val certificates = repoWithProps.props[CERTIFICATES_PREF_KEY]

            return WebdavRepo(id, uri, username, password, certificates)
        }
    }

    override fun isConnectionRequired(): Boolean {
        return true
    }

    override fun isAutoSyncSupported(): Boolean {
        return true
    }

    override fun getUri(): Uri {
        return uri
    }

    override fun getBooks(): MutableList<VersionedRook> {
        val url = uri.toUrl()

        if (!sardine.exists(url)) {
            sardine.createDirectory(url)
        }

        return sardine
                .list(url)
                .mapNotNull {
                    if (it.isDirectory || !BookName.isSupportedFormatFileName(it.name)) {
                        null
                    } else {
                        it.toVersionedRook()
                    }
                }
                .toMutableList()
    }

    override fun retrieveBook(fileName: String?, destination: File?): VersionedRook {
        val fileUrl = Uri.withAppendedPath(uri, fileName).toUrl()

        sardine.get(fileUrl).use { inputStream ->
            FileOutputStream(destination).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return sardine.list(fileUrl).first().toVersionedRook()
    }

    override fun storeBook(file: File?, fileName: String?): VersionedRook {
        val fileUrl = Uri.withAppendedPath(uri, fileName).toUrl()

        sardine.put(fileUrl, file, null)

        return sardine.list(fileUrl).first().toVersionedRook()
    }

    override fun renameBook(from: Uri, name: String?): VersionedRook {
        val destUrl = UriUtils.getUriForNewName(from, name).toUrl()
        sardine.move(from.toUrl(), destUrl)
        return sardine.list(destUrl).first().toVersionedRook()
    }

    override fun delete(uri: Uri) {
        sardine.delete(uri.toUrl())
    }

    private fun DavResource.toVersionedRook(): VersionedRook {
        return VersionedRook(
                repoId,
                RepoType.WEBDAV,
                uri,
                Uri.withAppendedPath(uri, this.name),
                this.name + this.modified.time.toString(),
                this.modified.time
        )
    }

    private fun Uri.toUrl(): String {
        return this.toString().replace("^(?:web)?dav(s?://)".toRegex(), "http$1")
    }
}
