package com.burton.builds.data.parse

import com.burton.builds.data.parse.TinyJson.bool
import com.burton.builds.data.parse.TinyJson.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TinyJsonTest {
    @Test
    fun parsesNestedObjectAndArray() {
        val parsed = TinyJson.parseObject("""{"name":"Ada","ok":true,"n":2,"peers":[{"id":"a"}]}""")
        assertEquals("Ada", parsed.str("name"))
        assertTrue(parsed.bool("ok"))
        assertEquals(2L, parsed["n"])
        assertEquals(1, TinyJson.run { parsed.objList("peers") }.size)
    }

    @Test
    fun stringifiesAndEscapes() {
        val json = TinyJson.stringify(mapOf("q" to "say \"hi\"", "on" to false))
        val parsed = TinyJson.parseObject(json)
        assertEquals("say \"hi\"", parsed.str("q"))
        assertFalse(parsed.bool("on", true))
    }

    @Test
    fun parsesList() {
        val items = TinyJson.parseList("""[{"id":"1"},{"id":"2"}]""")
        assertEquals(2, items.size)
        assertEquals("1", items[0].str("id"))
    }
}
