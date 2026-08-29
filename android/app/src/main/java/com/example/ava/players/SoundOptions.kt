package com.example.ava.players

import android.content.Context
import java.io.File

object SoundOptions {
    data class Option(val label: String, val uri: String)

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
    ) = options(context, directory).firstOrNull { it.uri == uri }?.label
        ?: labelForMissingUri(uri)

    fun options(context: Context, directory: File = File(ExternalSoundResolver.DEFAULT_EXTERNAL_SOUND_DIR)) =
        (bundledOptions { path -> context.assets.list(path)?.toList().orEmpty() } + externalOptions(directory))
            .distinctBy { it.uri }
            .sortedBy { it.label.lowercase() }

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
