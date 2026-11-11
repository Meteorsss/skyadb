package com.sky22333.skyadb.apps

import com.sky22333.skyadb.AppServices
import com.sky22333.skyadb.model.AdbOperationResult
import com.sky22333.skyadb.model.operationCatching
import com.sky22333.skyadb.repository.AdbRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

object RemoteAppLabels {
    fun parse(output: String): Map<String, String> {
        if (output.lineSequence().none { it.trim() == "SKYEND" }) return emptyMap()
        return output.lineSequence().mapNotNull { line ->
            val fields = line.trimEnd('\r').split('\t', limit = 3)
            if (fields.size == 3 && fields[0] == "SKYAPP" &&
                fields[1].matches(Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*")) && fields[2].isNotBlank()
            ) fields[1] to fields[2] else null
        }.toMap()
    }

    suspend fun load(repository: AdbRepository): Map<String, String> = withContext(Dispatchers.IO) {
        // Unique per call; never reuse phone/previous-TV labels for another device.
        val id = UUID.randomUUID().toString()
        val local = File(AppServices.context.cacheDir, "tv-labels-$id.dex")
        val remote = "/data/local/tmp/skyadb-labels-$id.dex"
        try {
            operationCatching {
                AppServices.context.assets.open("classes.dex").use { input ->
                    local.outputStream().use { input.copyTo(it) }
                }
                if (repository.push(local, remote) !is AdbOperationResult.Success) return@operationCatching emptyMap()
                // Read-only DEX also works on Android versions enforcing dynamic-code permissions.
                when (val result = repository.runShell(
                    "chmod 444 '$remote' && CLASSPATH='$remote' app_process /system/bin com.sky22333.skyadb.helper.TvAppLabels"
                )) {
                    is AdbOperationResult.Success -> if (result.data.exitCode == 0) parse(result.data.output) else emptyMap()
                    is AdbOperationResult.Failure -> emptyMap()
                }
            }.getOrDefault(emptyMap())
        } finally {
            local.delete()
            withContext(NonCancellable) { repository.runShell("rm -f '$remote'") }
        }
    }
}
