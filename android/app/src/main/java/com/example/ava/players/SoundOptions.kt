package com.example.ava.players

import android.content.Context
import java.io.File

object SoundOptions {
    data class Option(val label: String, val uri: String)

    const val NO_SOUND_LABEL = "No Sound"
    const val NO_SOUND_URI = "no-sound"

    val DEFAULT_BUNDLED_SOUNDS = listOf(
        Option(NO_SOUND_LABEL, NO_SOUND_URI),
        Option("Continuous Prompt", "asset:///sounds/continuous_prompt.wav"),
        Option("Start Listening Button", "asset:///sounds/start_listening_button.wav"),
        Option("Timer Finished", "asset:///sounds/timer_finished.wav"),
        Option("Wake Word Triggered", "asset:///sounds/wake_word_triggered.wav"),
        Option("Stop Sound", "asset:///stopWords/stop_sound.wav"),
    )

    private val bundledDirs = listOf("sounds", "stopWords")

    fun labels(context: Context, directory: File = File(ExternalSoundResolver.DEFAULT_EXTERNAL_SOUND_DIR)) =
        options(context, directory).map { it.label }

    fun uriForLabel(
        context: Context,
        label: String,
        directory: File = File(ExternalSoundResolver.DEFAULT_EXTERNAL_SOUND_DIR)
    ) = options(context, directory).firstOrNull { it.label == label }?.uri

    fun labelForUri(
        context: Context,
        uri: String,
        directory: File = File(ExternalSoundResolver.DEFAULT_EXTERNAL_SOUND_DIR)
    ) = if (isNoSound(uri)) NO_SOUND_LABEL
        else options(context, directory).firstOrNull { it.uri == uri }?.label ?: labelForMissingUri(uri)

    fun isNoSound(uri: String?) = uri.isNullOrBlank() || uri == NO_SOUND_URI

    fun options(context: Context, directory: File = File(ExternalSoundResolver.DEFAULT_EXTERNAL_SOUND_DIR)) =
        (DEFAULT_BUNDLED_SOUNDS + bundledOptions { path -> context.assets.list(path)?.toList().orEmpty() } + externalOptions(directory))
            .distinctBy { it.uri }
            .sortedWith(compareBy<Option> { if (it.uri == NO_SOUND_URI) 0 else 1 }.thenBy { it.label.lowercase() })

    internal fun bundledOptions(listAssets: (String) -> List<String>) = bundledDirs.flatMap { dir ->
        listAssets(dir)
            .filter(ExternalSoundResolver::isSafeWavName)
            .map { fileName -> Option(displayName(fileName), "asset:///$dir/$fileName") }
    }

    internal fun externalOptions(directory: File) = directory.listFiles()
        ?.filter { it.isFile && it.canRead() && ExternalSoundResolver.isSafeWavName(it.name) }
        ?.map { Option("${displayName(it.name)} (external)", "file://${it.absolutePath}") }
        .orEmpty()

    internal fun displayName(fileName: String) = fileName
        .substringBeforeLast('.')
        .replace(Regex("[_-]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }

    internal fun labelForMissingUri(uri: String): String {
        val fileName = uri.substringAfterLast('/').takeIf(ExternalSoundResolver::isSafeWavName) ?: return uri
        val suffix = if (uri.startsWith("file://")) " (external)" else ""
        return displayName(fileName) + suffix
    }
}
