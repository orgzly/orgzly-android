package com.orgzly.android.ui.note

import androidx.lifecycle.MutableLiveData
import com.orgzly.android.db.entity.Note
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent

class TableViewModel(
        val noteViewModel: NoteViewModel,
        private val tableStartOffset: Int,
        private val tableEndOffset: Int
) : CommonViewModel() {

    private val TAG = TableViewModel::class.java.name

    val tableView: MutableLiveData<String> = MutableLiveData("")

    val tableUpdatedEvent: SingleLiveEvent<Note> = noteViewModel.noteUpdatedEvent

    lateinit var tableTextBeforeEditing: String

    fun loadNoteData() {
        noteViewModel.loadData()
    }

    fun loadTableData() {
        val content = noteViewModel.notePayload!!.content!!

        tableTextBeforeEditing = content.substring(tableStartOffset, tableEndOffset)

        tableView.postValue(tableTextBeforeEditing)
    }

    fun isNoteModified(): Boolean {
        val unchanged: Boolean? = tableView.value?.equals(tableTextBeforeEditing)
        if (unchanged == null) {
            return false
        } else {
            return !unchanged
        }
    }

    fun updateNote(postSave: ((note: Note) -> Unit)?) {
        updatePayload()
        noteViewModel.updateNote(postSave)
    }

    private fun updatePayload() {

        val np = noteViewModel.notePayload!!

        val updatedContent = np.content

        val beforeTable = updatedContent!!.substring(0, tableStartOffset)
        val table = tableView.value
        val afterTable = updatedContent.substring(tableEndOffset, noteViewModel.notePayload!!.content!!.length)

        noteViewModel.notePayload =
                np.copy(
                        title = np.title,
                        content = beforeTable + table + afterTable,
                        state = np.state,
                        priority = np.priority,
                        scheduled = np.scheduled,
                        deadline = np.deadline,
                        closed = np.closed,
                        tags = np.tags,
                        properties = np.properties)

    }


}
