package com.sky22333.skyadb.processes

/** Values are KiB (Android's /proc and dumpsys units), not decimal kB. */
data class DeviceMemory(val totalKiB: Long, val availableKiB: Long) {
    val usedKiB: Long get() = totalKiB - availableKiB
}

data class DeviceProcess(val pid: Int, val name: String, val pssKiB: Long) {
    fun packageName(installed: Set<String>): String? = name.substringBefore(':').takeIf { it in installed }
}

object ProcessSnapshotParser {
    fun memory(output: String): DeviceMemory {
        val fields = output.lineSequence().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 2) null else parts[1].toLongOrNull()?.let { parts[0].removeSuffix(":") to it }
        }.toMap()
        val total = fields["MemTotal"]?.takeIf { it > 0 } ?: error("Missing MemTotal")
        val available = fields["MemAvailable"] ?: (
            (fields["MemFree"] ?: error("Missing MemFree")) + (fields["Buffers"] ?: 0) +
                (fields["Cached"] ?: 0) + (fields["SReclaimable"] ?: 0) - (fields["Shmem"] ?: 0)
            )
        return DeviceMemory(total, available.coerceIn(0, total))
    }

    fun processes(output: String): List<DeviceProcess> {
        val section = output.substringAfter("Total PSS by process:", "")
            .substringBefore("Total PSS by OOM adjustment:")
            .substringBefore("Total PSS by category:")
        require(section.isNotBlank()) { "Missing per-process PSS section" }
        val row = Regex("^\\s*([\\d,]+)\\s*(?:K|kB):\\s+(.+?)\\s+\\(pid\\s+(\\d+)(?:\\s|\\))")
        val processes = section.lineSequence().mapNotNull { line ->
            val match = row.find(line) ?: return@mapNotNull null
            val pss = match.groupValues[1].replace(",", "").toLongOrNull() ?: return@mapNotNull null
            val pid = match.groupValues[3].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            DeviceProcess(pid, match.groupValues[2], pss)
        }.distinctBy { it.pid }.sortedByDescending { it.pssKiB }.toList()
        require(processes.isNotEmpty()) { "No readable process memory data" }
        return processes
    }
}
