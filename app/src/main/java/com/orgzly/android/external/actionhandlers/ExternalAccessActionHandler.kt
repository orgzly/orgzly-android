package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.external.types.Response
import javax.inject.Inject

abstract class ExternalAccessActionHandler {
    @Inject
    lateinit var dataRepository: DataRepository

    init {
        App.appComponent.inject(this)
    }

    abstract val actions: Map<String, (Intent, Context) -> Response>
    private val fullNameActions by lazy {
        actions.mapKeys { (key, _) -> "com.orgzly.android.$key" }
    }

    fun getBook(intent: Intent) =
            dataRepository.getBook(intent.getLongExtra("BOOK_ID", -1))
                    ?: dataRepository.getBook(intent.getStringExtra("BOOK_NAME") ?: "")

    fun handle(intent: Intent, context: Context) =
            fullNameActions[intent.action!!]?.let { it(intent, context) }
}