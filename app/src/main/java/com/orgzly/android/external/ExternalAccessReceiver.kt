package com.orgzly.android.external

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.GsonBuilder
import com.orgzly.android.external.actionhandlers.*
import com.orgzly.android.external.types.Response

class ExternalAccessReceiver : BroadcastReceiver() {
    val actionHandlers = listOf(
            GetOrgInfo(),
            RunSearch(),
            EditNotes(),
            EditSavedSearches(),
            ManageWidgets()
    )

    override fun onReceive(context: Context?, intent: Intent?) {
        val response = actionHandlers.asSequence()
                .mapNotNull { it.handle(intent!!, context!!) }
                .firstOrNull()
                ?: Response(false, "Invalid action")
        val gson = GsonBuilder().serializeNulls().create()
        resultData = gson.toJson(response)
    }
}
