package com.orgzly.android.usecase

import android.os.Environment
import com.orgzly.android.BookName
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.NoteView
import java.io.File

class LinkFindTarget(val path: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val target = openLink(dataRepository, path)

        return UseCaseResult(
                userData = target
        )
    }

    private fun openLink(dataRepository: DataRepository, path: String): Any {
        val regex = """(.*)\:\:(.*)""".toRegex()
        return if (isAbsolute(path)) {
            File(path)
        } else if (regex.matches(path)) {
            val matchResults = regex.matchEntire(path)
            val (_, notebook, heading) = matchResults!!.groupValues
            isMaybeBook(notebook)?.let { bookName ->
                dataRepository.getBook(bookName.name)?.let {
                    val allNotes = dataRepository.getNotes(it.name)
                    for (note in allNotes) {
                        if (note.note.title == heading.substring(1)) { // fist char of heading is '*'
                            return Pair<Book, NoteView>(it, note);
                        }
                    }
                }
            }
            File(Environment.getExternalStorageDirectory(), path)
        } else {
            isMaybeBook(path)?.let { bookName ->
                dataRepository.getBook(bookName.name)?.let {
                    return it
                }
            }

            File(Environment.getExternalStorageDirectory(), path)
        }
    }

    private fun isAbsolute(bookPath: String): Boolean {
        return bookPath.startsWith('/')
    }

    private fun isMaybeBook(bookPath: String): BookName? {
        val file = File(bookPath)

        return if (!hasParent(file) && BookName.isSupportedFormatFileName(file.name)) {
            BookName.fromFileName(file.name)
        } else {
            null
        }
    }

    private fun hasParent(file: File): Boolean {
        val parentFile = file.parentFile
        return parentFile != null && parentFile.name != "."
    }
}