package io.github.cnissler.levelpitch.profiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** JSON format of the stored [AppData]; tolerant of fields added by later versions. */
val appDataJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

/**
 * Holds [AppData] and persists every change to [file]. The file is a few kB, so writes happen
 * synchronously on the caller's thread.
 */
class ProfileRepository(private val file: File) {

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    @Synchronized
    fun update(change: (AppData) -> AppData) {
        val next = change(_data.value)
        if (next == _data.value) return
        write(next)
        _data.value = next
    }

    private fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            appDataJson.decodeFromString(AppData.serializer(), file.readText())
        } catch (e: SerializationException) {
            keepCorrupt()
        } catch (e: IllegalArgumentException) {
            keepCorrupt()
        }
    }

    /** Moves an unreadable file aside, so it can be inspected, and starts empty. */
    private fun keepCorrupt(): AppData {
        file.renameTo(File(file.path + ".corrupt"))
        return AppData()
    }

    /** Write to a temp file, then rename, so a crash never leaves a half-written file. */
    private fun write(data: AppData) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(appDataJson.encodeToString(AppData.serializer(), data))
        if (!tmp.renameTo(file)) throw IOException("could not replace $file")
    }
}
