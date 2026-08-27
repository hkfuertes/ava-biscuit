package com.example.ava.utils

import com.example.ava.BuildConfig

object RuntimeIdentity {
    val packageName: String
        get() = BuildConfig.APPLICATION_ID

    fun action(name: String) = "$packageName.$name"
}
