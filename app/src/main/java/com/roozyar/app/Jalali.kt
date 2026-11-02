package com.roozyar.app

import java.time.LocalDate

val MONTHS = listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")
val DAYS = listOf("شنبه","یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه")

fun String.fa() = map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")
fun Int.fa() = toString().fa()

/** 0 = شنبه ... 6 = جمعه */
fun dayIdx(epochDay: Long) = (LocalDate.ofEpochDay(epochDay).dayOfWeek.value + 1) % 7

fun jalali(epochDay: Long): Triple<Int, Int, Int> {
    val d = LocalDate.ofEpochDay(epochDay)
    val gy = d.year; val gm = d.monthValue; val gd = d.dayOfMonth
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365 }
    val jm: Int; val jd: Int
    if (days < 186) { jm = 1 + days / 31; jd = 1 + days % 31 } else { jm = 7 + (days - 186) / 30; jd = 1 + (days - 186) % 30 }
    return Triple(jy, jm, jd)
}

fun fullDate(epochDay: Long): String {
    val (y, m, d) = jalali(epochDay)
    return "${DAYS[dayIdx(epochDay)]} ${d.fa()} ${MONTHS[m - 1]} ${y.fa()}"
}
fun shortDate(epochDay: Long): String {
    val (_, m, d) = jalali(epochDay)
    return "${d.fa()} ${MONTHS[m - 1]}"
}
fun hm(min: Int) = "%02d:%02d".format(min / 60, min % 60).fa()
