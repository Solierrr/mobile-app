package com.project.solaria_mobile

import com.project.solaria_mobile.feature.project.data.ProjectHistoryDto
import com.project.solaria_mobile.feature.project.data.ProjectPhotoDto
import com.project.solaria_mobile.feature.project.data.ProjectReportDto
import com.project.solaria_mobile.feature.project.data.toEntry
import com.project.solaria_mobile.feature.project.data.toReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectReportTest {
    @Test fun aHistoryEntryFromAReportCarriesItsIdAndSaysSo() {
        val entry = ProjectHistoryDto("h1", "REPORT_ADDED", "Visita técnica", "Ana", "2026-10-31T10:00:00Z", reportId = "r1").toEntry()

        assertEquals("r1", entry.reportId)
        assertEquals("Relatório: Visita técnica", entry.title)
    }

    @Test fun aRegularHistoryEntryHasNoReport() {
        val entry = ProjectHistoryDto("h2", "PHOTO_ADDED", "Foto adicionada", "Ana", "2026-10-31T10:00:00Z").toEntry()

        assertNull(entry.reportId)
        assertEquals("Foto adicionada", entry.title)
    }

    @Test fun theReportKeepsTheOrderOfItsImages() {
        val report = ProjectReportDto(
            "r1", "Vistoria", "Tudo certo.", "Ana", "2026-10-31T10:00:00Z",
            photos = listOf(ProjectPhotoDto("p2"), ProjectPhotoDto("p1")),
        ).toReport()

        assertEquals(listOf("p2", "p1"), report.photoIds)
        assertEquals("Vistoria", report.title)
        assertEquals("Ana", report.author)
    }

    @Test fun aReportWithMissingFieldsStillMaps() {
        val report = ProjectReportDto("r1").toReport()

        assertEquals("", report.title)
        assertEquals("", report.content)
        assertEquals(emptyList<String>(), report.photoIds)
    }
}
