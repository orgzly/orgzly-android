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

    abstract val actions: List<List<Pair<String, (Intent, Context) -> Response>>>
    private val fullNameActions by lazy {
        actions.flatten().toMap().mapKeys { (key, _) -> "com.orgzly.android.$key" }
    }

    fun getBook(intent: Intent) =
            dataRepository.getBook(intent.getLongExtra("BOOK_ID", -1))
                    ?: dataRepository.getBook(intent.getStringExtra("BOOK_NAME") ?: "")

    fun action(f: (Intent, Context) -> Response, vararg names: String) = names.map { it to f }

    @JvmName("intentAction")
    fun action(f: (Intent) -> Response, vararg names: String) =
            action({ i, _ -> f(i) }, *names)

    @JvmName("contextAction")
    fun action(f: (Context) -> Response, vararg names: String) =
            action({ _, c -> f(c) }, *names)

    fun action(f: () -> Response, vararg names: String) =
            action({ _, _ -> f() }, *names)


    fun handle(intent: Intent, context: Context) =
            fullNameActions[intent.action!!]?.let { it(intent, context) }
}