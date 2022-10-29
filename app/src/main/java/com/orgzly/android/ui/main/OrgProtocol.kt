package com.orgzly.android.ui.main

import android.content.Intent
import com.orgzly.BuildConfig
import com.orgzly.android.query.Condition
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.DottedQueryBuilder
import com.orgzly.android.util.LogUtils

// TODO: Complete
object OrgProtocol {
    interface Listener {
        fun onNoteWithId(id: String)

        fun onQuery(query: String)

        // FIXME: Extract string resources
        fun onError(str: String)
    }

    private const val ORG_PROTOCOL = "org-protocol"

    private const val ORG_ID_GOTO = "org-id-goto"
    private const val ORG_SEARCH = "org-search"
    private const val ORGZLY_SEARCH = "orgzly-search"

    private const val ID_PARAM = "id"
    private const val Q_PARAM = "q"

    @JvmStatic
    fun handleOrgProtocol(intent: Intent, listener: Listener) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        val uri = intent.data ?: return

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri)

        if (!Intent.ACTION_VIEW.equals(intent.action, ignoreCase = true)) {
            return
        }

        if (!ORG_PROTOCOL.equals(uri.scheme, ignoreCase = true)) {
            return
        }

        when (uri.host?.lowercase()) {
            ORG_ID_GOTO -> {
                val id = uri.getQueryParameter(ID_PARAM)

                if (id != null) {
                    if (BuildConfig.LOG_DEBUG)
                        LogUtils.d(TAG, "Open note with property ID $id")

                    listener.onNoteWithId(id)

                } else {
                    listener.onError("Missing “$ID_PARAM” param in $uri")
                }
            }

            ORG_SEARCH -> {
                val conditions = mutableListOf<Condition>()
                for (param in uri.queryParameterNames) {
                    uri.getQueryParameter(param)?.let { value ->
                        when (param) {
                            "tag" -> conditions.add(Condition.HasTag(value, false))
                            "text" -> conditions.add(Condition.HasText(value, false))
                            "state" -> conditions.add(Condition.HasState(value, false))
                            else -> { }
                        }
                    }
                }

                if (conditions.isNotEmpty()) {
                    val builder = DottedQueryBuilder()
                    val query = builder.build(Query(Condition.And(conditions)))

                    if (BuildConfig.LOG_DEBUG)
                        LogUtils.d(TAG, "Open query $query")

                    listener.onQuery(query)

                } else {
                    listener.onError("No supported parameters found in $uri")
                }
            }

            ORGZLY_SEARCH -> {
                val query = uri.getQueryParameter(Q_PARAM)

                if (query != null) {
                    if (BuildConfig.LOG_DEBUG)
                        LogUtils.d(TAG, "Open query $query")

                    listener.onQuery(query)

                } else {
                    listener.onError("Missing “${Q_PARAM}” parameter in $uri")
                }
            }

            else -> {
                listener.onError("Unsupported handler in $uri")
            }
        }
    }

    private val TAG = OrgProtocol::class.java.name
}