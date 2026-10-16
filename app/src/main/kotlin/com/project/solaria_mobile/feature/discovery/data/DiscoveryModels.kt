package com.project.solaria_mobile.feature.discovery.data


data class ScheduleEvent(val day: Int, val month: Int, val time: String, val title: String, val place: String)
data class CalendarDay(val number: Int, val inCurrentMonth: Boolean)
