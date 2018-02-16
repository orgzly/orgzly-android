package com.orgzly.android.filter

import android.content.Context
import android.net.Uri
import com.orgzly.R
import com.orgzly.android.LocalStorage
import com.orgzly.android.provider.clients.FiltersClient
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.util.MiscUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


class FileFilterStore(val context: Context) : FilterStore {

    override fun importFilters(uri: Uri) {
        val json = parseJson(uri) ?: return

        val filters = (0 until json.length()).map { i ->
            val filterJson = json.getJSONObject(i)
            Filter(filterJson.getString("name"), filterJson.getString("query"))
        }

        val count = FiltersClient.replaceAll(context, filters)

        val msg = context.resources.getQuantityString(R.plurals.imported_searches, count, count)

        CommonActivity.showSnackbar(context, msg)
    }

    /**
     * Parse JSON content.
     */
    private fun parseJson(uri: Uri): JSONArray? {
        return try {

            val fileContent = context.contentResolver.openInputStream(uri).use { stream ->
                MiscUtils.readStream(stream)
            }

            JSONArray(JSONTokener(fileContent))

        } catch (e: FileNotFoundException) {
            CommonActivity.showSnackbar(context, e.localizedMessage)
            return null

        } catch (e: JSONException) {
            CommonActivity.showSnackbar(context, e.localizedMessage)
            return null
        }
    }

    override fun exportFilters() {
        file().bufferedWriter().use { out ->
            val json = exportToJson()

            out.write(json.toString(2))

            val count = json.length()

            val msg = context.resources.getQuantityString(R.plurals.exported_searches, count, count)

            CommonActivity.showSnackbar(context, msg)
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

    companion object {
        val FILE_NAME = "Orgzly Search Queries.json"
    }
}