package com.sky22333.skyadb.processes

import org.junit.Assert.*
import org.junit.Test

class ProcessSnapshotParserTest {
    @Test fun modernMemoryUsesAvailableNotJustFree() {
        val memory = ProcessSnapshotParser.memory("MemTotal: 2048000 kB\nMemFree: 100000 kB\nMemAvailable: 512000 kB")
        assertEquals(1536000L, memory.usedKiB)
        assertEquals(512000L, memory.availableKiB)
    }
    @Test fun oldKernelIncludesReclaimableCache() {
        val memory = ProcessSnapshotParser.memory("MemTotal: 1000 kB\nMemFree: 100 kB\nBuffers: 20 kB\nCached: 300 kB\nSReclaimable: 50 kB\nShmem: 10 kB")
        assertEquals(460L, memory.availableKiB)
    }
    @Test fun clampsBadAvailableValues() {
        assertEquals(100L, ProcessSnapshotParser.memory("MemTotal: 100 kB\nMemAvailable: 200 kB").availableKiB)
    }
    @Test(expected = IllegalStateException::class) fun rejectsMissingMemory() { ProcessSnapshotParser.memory("Permission denied") }
    @Test fun parsesOldAndNewPssAndIgnoresOtherSections() {
        val output = """
            Applications Memory Usage (in Kilobytes):
            Total PSS by process:
                93,600K: com.tcl.settings (pid 1234 / activities)
                140,400 kB: com.tcl.cyberui (pid 99)
                200K: com.tcl.settings:remote (pid 1235)
                400K: surfaceflinger (pid 45)
            Total PSS by OOM adjustment:
                999,999K: bogus (pid 600)
        """.trimIndent()
        val processes = ProcessSnapshotParser.processes(output)
        assertEquals(listOf(99, 1234, 45, 1235), processes.map { it.pid })
        assertEquals(140400L, processes.first().pssKiB)
        assertEquals("com.tcl.settings", processes.last().packageName(setOf("com.tcl.settings")))
        assertNull(processes[2].packageName(setOf("com.tcl.settings")))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsPermissionFailure() { ProcessSnapshotParser.processes("Permission Denial") }
}
