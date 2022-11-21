package com.orgzly.android.usecase

import android.content.Context
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.NoteProperty
import com.orgzly.android.util.AttachmentUtils

/**
 * An [UseCase] that finds the attachment directory path with the given [noteId].
 * Corresponds to `org-attach-dir`. Currently checks the ID property of the given node.
 *
 * Note that this finds the expected directory path, even if the directory doesn't exist.
 *
 * TODO: Also check DIR property.
 * TODO: Also check inherited property, based on a preference as in `org-attach-use-inheritance`
 */
class FindAttachmentPath(val noteId: Long) : UseCase() {
    val context: Context = App.getAppContext();

    override fun run(dataRepository: DataRepository): UseCaseResult {
        val noteProperties = dataRepository.getNoteProperties(noteId)
        val idStr = getProperty(noteProperties, "ID")

        val path = if (idStr == null) null else AttachmentUtils.getAttachDir(context, idStr)

        return UseCaseResult(
                userData = path
        )
    }

    private fun getProperty(noteProperties: List<NoteProperty>, propertyName: String): String? {
        for (property: NoteProperty in noteProperties) {
            if (property.name == propertyName) {
                return property.value
            }
        }
        return null
    }
}