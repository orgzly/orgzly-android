package com.orgzly.android.data.mappers

import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NotePosition
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonMapperTest {
    @Test
    fun toJson() {
        val notes = arrayOf(
                Note(
                        1,
                        title = "Note",
                        state = "TODO",
                        tags = "tag1",
                        position = NotePosition(0, level = 1)),
                Note(
                        2,
                        title = "Note",
                        state = "TODO",
                        tags = "tag2 tag3",
                        position = NotePosition(0, level = 2, parentId = 1))
        )

        assertEquals("""
            [
              {
                "title": "Note",
                "state": "TODO",
                "level": 1,
                "tags": [
                  "tag1"
                ]
              },
              {
                "title": "Note",
                "state": "TODO",
                "level": 2,
                "tags": [
                  "tag2",
                  "tag3"
                ]
              }
            ]
            """.trimIndent(), JsonMapper.toJson(notes))
    }
}