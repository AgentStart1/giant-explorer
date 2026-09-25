package com.storyteller_f.giant_explorer.control.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginManagerTest {
    @Test
    fun gepUsesFragmentPluginLoader() {
        assertEquals(PluginType.fragment, pluginTypeForExtension("gep"))
        assertEquals(PluginType.fragment, pluginTypeForExtension("GEP"))
    }

    @Test
    fun otherArchivesUseHtmlPluginLoader() {
        assertEquals(PluginType.html, pluginTypeForExtension("zip"))
    }
}
