package com.expensetracker.data.converter

import com.expensetracker.data.model.RecurrenceFrequency
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `convert BigDecimal to string and back should preserve value`() {
        val original = BigDecimal("123.45")
        val stringValue = converters.fromBigDecimal(original)
        val converted = converters.toBigDecimal(stringValue)

        assertEquals(original, converted)
    }

    @Test
    fun `convert null BigDecimal should return null`() {
        val stringValue = converters.fromBigDecimal(null)
        val converted = converters.toBigDecimal(null)

        assertNull(stringValue)
        assertNull(converted)
    }

    @Test
    fun `convert LocalDateTime to string and back should preserve value`() {
        val original = LocalDateTime.of(2024, 1, 15, 14, 30, 45)
        val stringValue = converters.fromLocalDateTime(original)
        val converted = converters.toLocalDateTime(stringValue)

        assertEquals(original, converted)
    }

    @Test
    fun `convert null LocalDateTime should return null`() {
        val stringValue = converters.fromLocalDateTime(null)
        val converted = converters.toLocalDateTime(null)

        assertNull(stringValue)
        assertNull(converted)
    }

    @Test
    fun `convert LocalDate to string and back should preserve value`() {
        val original = LocalDate.of(2024, 1, 15)
        val stringValue = converters.fromLocalDate(original)
        val converted = converters.toLocalDate(stringValue)

        assertEquals(original, converted)
    }

    @Test
    fun `convert null LocalDate should return null`() {
        val stringValue = converters.fromLocalDate(null)
        val converted = converters.toLocalDate(null)

        assertNull(stringValue)
        assertNull(converted)
    }

    @Test
    fun `convert string list to JSON and back should preserve value`() {
        val original = listOf("tag1", "tag2", "tag3")
        val jsonValue = converters.fromStringList(original)
        val converted = converters.toStringList(jsonValue)

        assertEquals(original, converted)
    }

    @Test
    fun `convert empty string list should work`() {
        val original = emptyList<String>()
        val jsonValue = converters.fromStringList(original)
        val converted = converters.toStringList(jsonValue)

        assertEquals(original, converted)
        assertTrue(converted!!.isEmpty())
    }

    @Test
    fun `convert null string list should return null`() {
        val jsonValue = converters.fromStringList(null)
        val converted = converters.toStringList(null)

        assertNull(jsonValue)
        assertNull(converted)
    }

    @Test
    fun `convert string list with special characters should work`() {
        val original = listOf("tag with spaces", "tag-with-dashes", "tag_with_underscores", "tag.with.dots")
        val jsonValue = converters.fromStringList(original)
        val converted = converters.toStringList(jsonValue)

        assertEquals(original, converted)
    }

    @Test
    fun `convert RecurrenceFrequency to string and back should preserve value`() {
        for (frequency in RecurrenceFrequency.values()) {
            val stringValue = converters.fromRecurrenceFrequency(frequency)
            val converted = converters.toRecurrenceFrequency(stringValue)

            assertEquals(frequency, converted)
        }
    }

    @Test
    fun `convert null RecurrenceFrequency should return null`() {
        val stringValue = converters.fromRecurrenceFrequency(null)
        val converted = converters.toRecurrenceFrequency(null)

        assertNull(stringValue)
        assertNull(converted)
    }

    @Test
    fun `convert specific RecurrenceFrequency values should work`() {
        assertEquals("DAILY", converters.fromRecurrenceFrequency(RecurrenceFrequency.DAILY))
        assertEquals("WEEKLY", converters.fromRecurrenceFrequency(RecurrenceFrequency.WEEKLY))
        assertEquals("MONTHLY", converters.fromRecurrenceFrequency(RecurrenceFrequency.MONTHLY))
        assertEquals("YEARLY", converters.fromRecurrenceFrequency(RecurrenceFrequency.YEARLY))

        assertEquals(RecurrenceFrequency.DAILY, converters.toRecurrenceFrequency("DAILY"))
        assertEquals(RecurrenceFrequency.WEEKLY, converters.toRecurrenceFrequency("WEEKLY"))
        assertEquals(RecurrenceFrequency.MONTHLY, converters.toRecurrenceFrequency("MONTHLY"))
        assertEquals(RecurrenceFrequency.YEARLY, converters.toRecurrenceFrequency("YEARLY"))
    }

    @Test
    fun `convert large BigDecimal should work`() {
        val original = BigDecimal("999999999.99")
        val stringValue = converters.fromBigDecimal(original)
        val converted = converters.toBigDecimal(stringValue)

        assertEquals(original, converted)
    }

    @Test
    fun `convert BigDecimal with many decimal places should work`() {
        val original = BigDecimal("123.456789")
        val stringValue = converters.fromBigDecimal(original)
        val converted = converters.toBigDecimal(stringValue)

        assertEquals(original, converted)
    }

    @Test
    fun `LocalDateTime string format should be ISO format`() {
        val dateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45)
        val stringValue = converters.fromLocalDateTime(dateTime)

        assertEquals("2024-01-15T14:30:45", stringValue)
    }

    @Test
    fun `LocalDate string format should be ISO format`() {
        val date = LocalDate.of(2024, 1, 15)
        val stringValue = converters.fromLocalDate(date)

        assertEquals("2024-01-15", stringValue)
    }
}