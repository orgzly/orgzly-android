package com.orgzly.android.repos

import android.net.Uri
import android.os.Build
import com.orgzly.android.BookName
import com.orgzly.android.util.TLSSocketFactory
import com.orgzly.android.util.UriUtils
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okio.Buffer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.Arrays
import javax.net.ssl.*


class WebdavRepo(
        private val repoId: Long,
        private val uri: Uri,
        username: String,
        password: String,
        certificates: String? = null
) : SyncRepo {

    private val sardine = client(certificates).apply {
        setCredentials(username, password)
    }

    private fun client(certificates: String?): OkHttpSardine {
        return OkHttpSardine(okHttpClient(certificates))
    }

    private fun okHttpClient(certificates: String?): OkHttpClient {
        val trustManager = x509TrustManager(certificates)
        val builder = OkHttpClient.Builder()
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            builder.sslSocketFactory(TLSSocketFactory(trustManager), trustManager)
                .connectionSpecs(arrayListOf(
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .allEnabledTlsVersions()
                        .allEnabledCipherSuites()
                        .build(),
                    ConnectionSpec.COMPATIBLE_TLS,
                    ConnectionSpec.CLEARTEXT
                ))
        } else {
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), null)
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustManager)
        }
        return builder.build()
    }

    private fun x509TrustManager(certificates: String?): X509TrustManager {
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore(certificates))
            if (trustManagers.isNotEmpty() && trustManagers[0] is X509TrustManager) {
                return trustManagers[0] as X509TrustManager
            }
            throw IllegalStateException("Unexpected default trust manager: " + Arrays.toString(trustManagers))
        }
    }

    private fun keyStore(certificates: String?): KeyStore? {
        if (certificates.isNullOrEmpty()) {
            return null
        }
        val certs = Buffer().writeUtf8(certificates).inputStream().use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificates(stream)
        }
        val password = "password".toCharArray() // Any password will work
        val keyStore = newEmptyKeyStore(password)
        for ((index, cert) in certs.withIndex()) {
            val certificateAlias = index.toString()
            keyStore.setCertificateEntry(certificateAlias, cert)
        }
        return keyStore
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
