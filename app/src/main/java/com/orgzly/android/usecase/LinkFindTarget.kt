package com.orgzly.android.usecase

import com.orgzly.android.App
import com.orgzly.android.BookName
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import java.io.File

class LinkFindTarget(val path: String) : UseCase() {
    val context = App.getAppContext();

    override fun run(dataRepository: DataRepository): UseCaseResult {
        val target = openLink(dataRepository, path)

        return UseCaseResult(
                userData = target
        )
    }

    private fun openLink(dataRepository: DataRepository, path: String): Any {
        return if (isAbsolute(path)) {
            File(AppPreferences.fileAbsoluteRoot(context), path)
        } else {
            isMaybeBook(path)?.let { bookName ->
                dataRepository.getBook(bookName.name)?.let {
                    return it
                }
            }

            File(AppPreferences.fileRelativeRoot(context), path)
        }
    }

    private fun isAbsolute(path: String): Boolean {
        return path.startsWith('/')
    }

    private fun isMaybeBook(path: String): BookName? {
        val file = File(path)

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