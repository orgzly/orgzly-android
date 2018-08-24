package com.orgzly.android.savedsearch

import android.content.Context
import android.net.Uri
import com.orgzly.R
import com.orgzly.android.data.DataRepository
import com.orgzly.android.LocalStorage
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.util.MiscUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileNotFoundException


class FileSavedSearchStore(
        private val context: Context,
        private val dataRepository: DataRepository) : SavedSearchStore {

    override fun importSearches(uri: Uri) {
        val json = parseJson(uri) ?: return

        val savedSearches = (0 until json.length()).map { i ->
            with(json.getJSONObject(i)) {
                SavedSearch(0, getString("name"), getString("query"), i + 1)
            }
        }

        val count = dataRepository.replaceSavedSearches(savedSearches)

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

    override fun exportSearches() {
        file().bufferedWriter().use { out ->
            val json = exportToJson()

            out.write(json.toString(2))

            val count = json.length()

            val msg = context.resources.getQuantityString(R.plurals.exported_searches, count, count)

            CommonActivity.showSnackbar(context, msg)
        }
    }

    private fun exportToJson(): JSONArray {
        val jsonArray = JSONArray()

        dataRepository.getSavedSearches().forEach {
            val json = JSONObject()

            json.put("name", it.name)
            json.put("query", it.query)

            jsonArray.put(json)
        }

        return jsonArray
    }

    fun file(): File {
        val dir = LocalStorage(context).downloadsDirectory()
        return File(dir, FILE_NAME)
    }

    companion object {
        val FILE_NAME = "Orgzly Search Queries.json"
    }
}