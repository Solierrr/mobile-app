package com.project.solaria_mobile

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.project.solaria_mobile.feature.communication.data.AndroidMediaGateway
import com.project.solaria_mobile.feature.communication.data.MediaUploadTarget
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class AndroidMediaGatewayTest {
    @Test fun signedUploadUsesTheGrantedMethodHeadersAndBytesWithoutBearer() = runTest {
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val context = mockk<Context>()
        mockkStatic(Uri::class)
        try {
            every { Uri.parse("content://audio") } returns uri
            every { context.contentResolver } returns resolver
            every { resolver.openInputStream(uri) } answers { "audio".byteInputStream() }
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setResponseCode(200))
                val target = MediaUploadTarget(server.url("/signed").toString(), "POST", mapOf("content-type" to "audio/mp4", "x-upload-id" to "signed-value"))
                assertTrue(AndroidMediaGateway(context).upload(target, "content://audio", "application/octet-stream", 5))
                val request = server.takeRequest(5, TimeUnit.SECONDS)!!
                assertEquals("POST", request.method)
                assertEquals("audio/mp4", request.getHeader("Content-Type"))
                assertEquals("signed-value", request.getHeader("x-upload-id"))
                assertEquals("5", request.getHeader("Content-Length"))
                assertNull(request.getHeader("Authorization"))
                assertEquals("audio", request.body.readUtf8())
            }
        } finally { unmockkStatic(Uri::class) }
    }
}
