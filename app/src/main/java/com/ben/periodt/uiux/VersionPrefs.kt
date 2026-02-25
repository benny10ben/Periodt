package com.ben.periodt.uiux

import android.content.Context

object VersionPrefs {
    private const val NAME = "version_prefs"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version"

    fun getLastSeenVersion(context: Context): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_SEEN_VERSION, -1)

    fun setLastSeenVersion(context: Context, versionCode: Int) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_LAST_SEEN_VERSION, versionCode).apply()
    }
}