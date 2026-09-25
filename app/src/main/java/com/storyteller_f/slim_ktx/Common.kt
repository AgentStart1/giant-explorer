package com.storyteller_f.slim_ktx

val Throwable.exceptionMessage: String
    get() = localizedMessage ?: message ?: javaClass.simpleName
