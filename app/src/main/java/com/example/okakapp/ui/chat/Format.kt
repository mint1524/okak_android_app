package com.example.okakapp.ui.chat

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

fun formatTime(iso: String): String =
    runCatching { timeFormatter.format(Instant.parse(iso)) }.getOrDefault("")

fun formatDateTime(iso: String): String =
    runCatching { dateTimeFormatter.format(Instant.parse(iso)) }.getOrDefault(iso.take(16).replace('T', ' '))

fun dayHeaderFor(iso: String): String {
    val date = runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull() ?: return ""
    val today = LocalDate.now()
    return when (date) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    }
}
