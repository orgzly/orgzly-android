package com.orgzly.android.filter

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.LocalStorage
import com.orgzly.android.provider.clients.FiltersClient
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File


class FileFilterStore(val context: Context) : FilterStore {

    override fun importFilters() {
        val file = file()

        if (fileExists(file)) {
            val json = JSONArray(JSONTokener(file.readText()))

            val filters = (0 until json.length()).map { i ->
                val filterJson = json.getJSONObject(i)
                Filter(filterJson.getString("name"), filterJson.getString("query"))
            }

            val imported = FiltersClient.replaceAll(context, filters)

            notifyUser(context.getString(R.string.imported_filters, imported))
        }
    }

    override fun exportFilters() {
        file().bufferedWriter().use { out ->
            val json = exportToJson()

            out.write(json.toString(2))

            notifyUser(context.getString(R.string.exported_filters, json.length()))
        }
    }

    private fun exportToJson(): JSONArray {
        val filters = JSONArray()
        FiltersClient.forEach(context) {
            val json = JSONObject()

            json.put("name", it.name)
            json.put("query", it.query)

            filters.put(json)
        }
        return filters
    }

    fun file(): File {
        val dir = LocalStorage(context).downloadsDirectory()
        return File(dir, FILE_NAME)
    }

    private fun fileExists(file: File): Boolean {
        val fileExists = file.exists()

        if (!fileExists) {
            notifyUser(context.getString(R.string.file_does_not_exist, file))
        }

        return fileExists
    }

    private fun notifyUser(msg: String) {
        val intent = Intent(AppIntent.ACTION_DISPLAY_MESSAGE)
        intent.putExtra(AppIntent.EXTRA_MESSAGE, msg)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    companion object {
        val FILE_NAME = "orgzly.queries.json"
    }
}