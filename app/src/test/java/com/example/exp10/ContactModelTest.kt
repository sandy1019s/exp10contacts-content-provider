package com.example.exp10

import com.example.exp10.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactModelTest {

    @Test
    fun testInitialsGeneration() {
        val contact1 = Contact("1", "key1", "John Doe", "+1234567890")
        assertEquals("JD", contact1.getInitials())

        val contact2 = Contact("2", "key2", "Alice", "+9876543210")
        assertEquals("A", contact2.getInitials())

        val contact3 = Contact("3", "key3", "Robert Baden Powell", "+1122334455")
        assertEquals("RB", contact3.getInitials())

        val contact4 = Contact("4", "key4", "  ", "+0000000000")
        assertEquals("?", contact4.getInitials())
    }
}
