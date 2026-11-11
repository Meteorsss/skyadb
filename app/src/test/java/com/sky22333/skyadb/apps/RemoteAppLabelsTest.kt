package com.sky22333.skyadb.apps

import org.junit.Assert.*
import org.junit.Test

class RemoteAppLabelsTest {
    @Test fun parsesUnicodeAndIgnoresRuntimeNoise() {
        val labels = RemoteAppLabels.parse("WARNING: linker\nSKYAPP\tcom.tcl.settings\t??\r\nSKYAPP\tapp.orchard.tv\tAerial\nSKYEND\n")
        assertEquals(mapOf("com.tcl.settings" to "??", "app.orchard.tv" to "Aerial"), labels)
    }
    @Test fun rejectsTruncatedOutputRatherThanCachingPartialLabels() {
        assertTrue(RemoteAppLabels.parse("SKYAPP\tcom.tcl.settings\t??").isEmpty())
    }
    @Test fun rejectsMalformedRows() {
        assertTrue(RemoteAppLabels.parse("SKYAPP\tbad;name\tBad\nSKYAPP\tcom.blank\t  \nSKYEND").isEmpty())
    }
}
