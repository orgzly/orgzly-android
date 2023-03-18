package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.external.types.Response
import javax.inject.Inject

abstract class ExternalAccessActionHandler : ExternalIntentParser {
    @Inject
    override lateinit var dataRepository: DataRepository

    init {
        @Suppress("LeakingThis")
        App.appComponent.inject(this)
    }

    abstract val actions: List<List<Pair<String, (Intent, Context) -> Any>>>
    private val fullNameActions by lazy {
        actions.flatten().toMap().mapKeys { (key, _) -> "com.orgzly.android.$key" }
    }

    fun action(f: (Intent, Context) -> Any, vararg names: String) = names.map { it to f }

    @JvmName("intentAction")
    fun action(f: (Intent) -> Any, vararg names: String) =
            action({ i, _ -> f(i) }, *names)

    @JvmName("contextAction")
    fun action(f: (Context) -> Any, vararg names: String) =
            action({ _, c -> f(c) }, *names)

    fun action(f: () -> Any, vararg names: String) =
            action({ _, _ -> f() }, *names)


    fun handle(intent: Intent, context: Context) = try {
        fullNameActions[intent.action!!]
                ?.let { it(intent, context) }
                ?.let { Response(true, if (it is Unit) null else it) }
    } catch (e: Exception) {
        Response(false, e.message)
    }
}
