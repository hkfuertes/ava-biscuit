package com.example.ava.players

import java.io.File

object ExternalSoundResolver {
    const val DEFAULT_EXTERNAL_SOUND_DIR = "/sdcard/sounds"
    private val safeSoundName = Regex("[A-Za-z0-9._-]+\\.(wav|mp3)", RegexOption.IGNORE_CASE)

    fun resolve(soundUrl: String?, directory: File = File(DEFAULT_EXTERNAL_SOUND_DIR)): String? {
        if (soundUrl.isNullOrBlank() || !soundUrl.startsWith("asset:///")) return soundUrl
        val fileName = soundUrl.substringAfterLast('/')
        if (!isSafeSoundName(fileName)) return soundUrl
        val external = File(directory, fileName)
        return if (external.isFile && external.canRead()) "file://${external.absolutePath}" else soundUrl
    }

    internal fun isSafeSoundName(fileName: String) =
        fileName.isNotBlank() && !fileName.startsWith(".") && safeSoundName.matches(fileName)
}
