package com.orgzly.android.external.types

import java.io.Serializable

data class Response(val success: Boolean, val result: Any?) {
    constructor(success: Boolean, result: List<Serializable>) :
            this(success, result.toTypedArray())
}