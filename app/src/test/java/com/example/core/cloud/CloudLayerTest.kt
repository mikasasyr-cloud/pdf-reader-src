package com.example.core.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.cloud.provider.MockCloudProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudLayerTest {

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Test
    fun `pkce verifier and s256 challenge are valid url-safe strings`() {
        val verifier = OAuthPkceManager.generateCodeVerifier()
        val challenge = OAuthPkceManager.computeS256Challenge(verifier)

        assertTrue(verifier.length >= 43)
        assertTrue(challenge.isNotBlank())
        assertNotEquals(verifier, challenge)
        assertTrue(!challenge.contains('=') && !challenge.contains('+') && !challenge.contains('/'))
    }

    @Test
    fun `mock cloud providers lazy load root and subfolders cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = SamplePdfGenerator(context, testDispatchers)

        CloudProviderType.cloudProviders.forEach { providerType ->
            val mock = MockCloudProvider(providerType, generator)
            val rootResult = mock.listFolder(null)
            assertTrue(rootResult is AppResult.Success)
            val rootNodes = (rootResult as AppResult.Success).data
            assertTrue("Expected non-empty root for $providerType", rootNodes.isNotEmpty())

            val firstFolder = rootNodes.first { it.isFolder }
            val childResult = mock.listFolder(firstFolder.id)
            assertTrue(childResult is AppResult.Success)
            val childNodes = (childResult as AppResult.Success).data
            assertTrue("Expected children in ${firstFolder.id}", childNodes.isNotEmpty())
        }
    }

    @Test
    fun `sample pdf generator writes valid multi-page PDF file`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = SamplePdfGenerator(context, testDispatchers)

        val result = generator.ensureSampleGuidePdf()
        assertTrue(result is AppResult.Success)
        val doc = (result as AppResult.Success).data
        val file = File(doc.localFilePath)
        assertTrue(file.exists())
        assertTrue(doc.totalBytes > 500L)
        assertEquals(64, doc.documentHash.length)

        val header = file.inputStream().use { it.readNBytes(5).decodeToString() }
        assertEquals("%PDF-", header)
    }

    @Test
    fun `cloud config exposes oauth spec for all 4 providers with matching redirect uri`() {
        CloudProviderType.cloudProviders.forEach { type ->
            val spec = CloudConfig.oauthSpecFor(type)
            assertTrue("Missing OAuth spec for $type", spec != null)
        }
        assertEquals("com.aistudio.qalamreader.xmpqtz://oauth2redirect", CloudConfig.REDIRECT_URI)
    }
}
