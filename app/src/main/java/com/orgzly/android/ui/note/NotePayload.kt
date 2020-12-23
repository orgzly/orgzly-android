package com.orgzly.android.ui.note

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.util.AttachmentUtils
import com.orgzly.org.OrgProperties

data class NotePayload @JvmOverloads constructor(
        val title: String = "",
        val content: String? = null,
        val state: String? = null,
        val priority: String? = null,
        val scheduled: String? = null,
        val deadline: String? = null,
        val closed: String? = null,
        val tags: List<String> = emptyList(),
        val properties: OrgProperties = OrgProperties(),
        val attachmentUri: Uri? = null
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(title)
        out.writeString(content)

        out.writeString(state)
        out.writeString(priority)

        out.writeString(scheduled)
        out.writeString(deadline)
        out.writeString(closed)

        out.writeStringList(tags)

        out.writeInt(properties.size())
        properties.all.let {
            for (property in it) {
                out.writeString(property.name)
                out.writeString(property.value)
            }
        }

        out.writeString(attachmentUri.toString())
    }

    /** Returns the path to store the attachment. */
    fun attachDir(context: Context): String {
        val idStr = properties.get("ID")
        // TODO idStr could be null. Throw a warning exception, show a toast, don't attach anything
        when(AppPreferences.attachMethod(context)) {
            ShareActivity.ATTACH_METHOD_LINK -> return ""
            ShareActivity.ATTACH_METHOD_COPY_DIR -> return AppPreferences.attachDirDefaultPath(context)
            ShareActivity.ATTACH_METHOD_COPY_ID -> {
                return AttachmentUtils.getAttachDir(context, idStr)
            }
        }
        return ""
    }

    companion object {
        fun getInstance(title: String, content: String? = null): NotePayload {
            return NotePayload(title, content)
        }

        @JvmStatic
        fun fromParcel(parcel: Parcel): NotePayload {
            val title = parcel.readString()
            val content = parcel.readString()

            val state = parcel.readString()
            val priority = parcel.readString()

            val scheduled = parcel.readString()
            val deadline = parcel.readString()
            val closed = parcel.readString()

            val tags = mutableListOf<String>()
            parcel.readStringList(tags)

            val properties = OrgProperties()
            repeat(parcel.readInt()) {
                val name = parcel.readString()
                val value = parcel.readString()
                properties.put(name!!, value!!)
            }

            val attachmentUri: Uri? = parcel.readString()?.let { Uri.parse(it) }

            return NotePayload(
                    title!!,
                    content,
                    state,
                    priority,
                    scheduled,
                    deadline,
                    closed,
                    tags,
                    properties,
                    attachmentUri
            )
        }

        @JvmField
        val CREATOR: Parcelable.Creator<NotePayload> = object : Parcelable.Creator<NotePayload> {
            override fun createFromParcel(parcel: Parcel): NotePayload {
                return fromParcel(parcel)
            }

            override fun newArray(size: Int): Array<NotePayload?> {
                return arrayOfNulls(size)
            }
        }
    }
}
