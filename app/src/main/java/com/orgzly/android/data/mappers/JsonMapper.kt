package com.orgzly.android.data.mappers

import com.orgzly.android.db.entity.Note
import org.json.JSONArray
import org.json.JSONObject

object JsonMapper {
    private const val INDENT = 2

    fun toJson(notes: Array<Note>): String {
        return toArray(notes).toString(INDENT)
    }

    private fun toArray(notes: Array<Note>): JSONArray {
        return JSONArray().apply {
            notes.forEach {
                put(JsonMapper.toObject(it))
            }
        }
    }

    private fun toObject(note: Note): JSONObject {
        return JSONObject().apply {

            put("title", note.title)

            put("state", note.state)

            put("level", note.position.level)

            if (note.hasTags()) {
                put("tags", JSONArray().apply {
                    note.getTagsList().forEach { tag ->
                        put(tag)
                    }
                })
            }
        }
    }
}