package com.project.solaria_mobile

import com.project.solaria_mobile.core.mock.MockApiData
import com.project.solaria_mobile.core.mock.MockImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockImagesTest {
    private val projectId = "qa-project-1"
    private val catalog: (Int) -> List<MockImage> = { listOf(MockImage(byteArrayOf(1), "image/webp"), MockImage(byteArrayOf(2), "image/webp")) }

    @Test
    fun demoImagesFillTheGalleryAndOneIsCover() {
        val photos = MockApiData().photos(projectId, catalog)
        assertEquals(2, photos.size)
        assertEquals(1, photos.count { it.cover })
    }

    @Test
    fun turningImagesOffHidesThemButKeepsUploads() {
        val data = MockApiData()
        data.photos(projectId, catalog)
        val upload = data.addPhoto(projectId, byteArrayOf(9), "image/jpeg")
        val off = data.photos(projectId, null)
        assertEquals(listOf(upload.id), off.map { it.id })
        assertTrue(off.single().cover)
        assertEquals(3, data.photos(projectId, catalog).size)
    }

    @Test
    fun imagesStayEmptyWhenNeverEnabled() {
        assertTrue(MockApiData().photos(projectId, null).isEmpty())
    }
}
