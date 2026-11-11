package com.sky22333.skyadb.apps

import org.junit.Assert.*
import org.junit.Test

class RemoteAppLabelsTest {
    @Test fun parsesUnicodeAndIgnoresRuntimeNoise() {
        val labels = RemoteAppLabels.parse("WARNING: linker\nSKYAPP\tcom.tcl.settings\t\u8bbe\u7f6e\r\nSKYAPP\tapp.orchard.tv\tAerial\nSKYEND\n")
        assertEquals(mapOf("com.tcl.settings" to "\u8bbe\u7f6e", "app.orchard.tv" to "Aerial"), labels)
    }
    @Test fun rejectsTruncatedOutputRatherThanCachingPartialLabels() {
        assertTrue(RemoteAppLabels.parse("SKYAPP\tcom.tcl.settings\t\u8bbe\u7f6e").isEmpty())
    }
    @Test fun rejectsMalformedRows() {
        assertTrue(RemoteAppLabels.parse("SKYAPP\tbad;name\tBad\nSKYAPP\tcom.blank\t  \nSKYEND").isEmpty())
    }
}
