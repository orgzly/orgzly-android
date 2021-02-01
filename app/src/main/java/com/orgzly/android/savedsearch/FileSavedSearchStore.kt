package com.orgzly.android.savedsearch

import android.content.Context
import android.net.Uri
import com.orgzly.android.data.DataRepository
import com.orgzly.android.LocalStorage
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.util.MiscUtils
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.IOException


class FileSavedSearchStore(
        private val context: Context,
        private val dataRepository: DataRepository) : SavedSearchStore {

    override fun importSearches(uri: Uri): Int {
        val json = parseJson(uri)

        val savedSearches = (0 until json.length()).map { i ->
            with(json.getJSONObject(i)) {
                SavedSearch(0, getString("name"), getString("query"), i + 1)
            }
        }

        return dataRepository.replaceSavedSearches(savedSearches)
    }

    /**
     * Parse JSON content.
     */
    private fun parseJson(uri: Uri): JSONArray {
        val fileContent = context.contentResolver.openInputStream(uri).use { stream ->
            MiscUtils.readStream(stream)
        }

        return JSONArray(JSONTokener(fileContent))
    }

    override fun exportSearches(uri: Uri?): Int {
        val stream = if (uri != null) {
            context.contentResolver.openOutputStream(uri, "rwt")
                    ?: throw IOException("Cannot open output stream for $uri")
        } else {
            file().outputStream()
        }

        stream.bufferedWriter().use { out ->
            val json = exportToJson()

            out.write(json.toString(2))

            return json.length()
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
        return File(dir, "$FILE_NAME.json")
    }

    companion object {
        const val FILE_NAME = "Orgzly Search Queries"
    }
}