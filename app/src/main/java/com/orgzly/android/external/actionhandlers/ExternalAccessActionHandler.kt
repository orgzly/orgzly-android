package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.orgzly.android.external.types.Response

abstract class ExternalAccessActionHandler {
    abstract val actions: Map<String, (Intent, Context) -> Response>
    private val fullNameActions by lazy {
        actions.mapKeys { (key, _) -> "com.orgzly.android.$key" }
    }

    fun handle(intent: Intent, context: Context) =
            fullNameActions[intent.action!!]?.let { it(intent, context) }
}