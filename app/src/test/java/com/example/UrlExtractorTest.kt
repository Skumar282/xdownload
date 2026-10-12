package com.example

import com.example.util.UrlExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun testValidXComUrl() {
        val input = "https://x.com/jack/status/20"
        val result = UrlExtractor.extractXStatusUrl(input)
        assertNotNull(result)
        assertEquals("20", result?.statusId)
        assertEquals("https://x.com/jack/status/20", result?.canonicalUrl)
    }

    @Test
    fun testValidTwitterComUrlWithQueryParameters() {
        val input = "https://twitter.com/elonmusk/status/1844781290123456789?s=20&t=abcdef123"
        val result = UrlExtractor.extractXStatusUrl(input)
        assertNotNull(result)
        assertEquals("1844781290123456789", result?.statusId)
        assertEquals("https://x.com/elonmusk/status/1844781290123456789", result?.canonicalUrl)
    }

    @Test
    fun testMobileSubdomains() {
        val inputMobileTwitter = "https://mobile.twitter.com/nasa/status/9876543210"
        val result1 = UrlExtractor.extractXStatusUrl(inputMobileTwitter)
        assertNotNull(result1)
        assertEquals("9876543210", result1?.statusId)

        val inputMobileX = "https://m.x.com/SpaceX/status/1122334455"
        val result2 = UrlExtractor.extractXStatusUrl(inputMobileX)
        assertNotNull(result2)
        assertEquals("1122334455", result2?.statusId)
    }

    @Test
    fun testUrlEmbeddedInSharedText() {
        val sharedText = "Look at this amazing footage from the launch! https://x.com/SpaceX/status/1844781290123456789?s=46 via @SpaceX"
        val result = UrlExtractor.extractXStatusUrl(sharedText)
        assertNotNull(result)
        assertEquals("1844781290123456789", result?.statusId)
    }

    @Test
    fun testRejectUnrelatedUrls() {
        val youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val result = UrlExtractor.extractXStatusUrl(youtubeUrl)
        assertNull(result)

        val nonXDomain = UrlExtractor.detectNonXUrlDomain(youtubeUrl)
        assertEquals("www.youtube.com", nonXDomain)

        val instagramUrl = "https://www.instagram.com/p/DA123456789/"
        assertNull(UrlExtractor.extractXStatusUrl(instagramUrl))
    }

    @Test
    fun testMalformedOrEmptyUrl() {
        assertNull(UrlExtractor.extractXStatusUrl(null))
        assertNull(UrlExtractor.extractXStatusUrl(""))
        assertNull(UrlExtractor.extractXStatusUrl("just some random text without urls"))
        assertNull(UrlExtractor.extractXStatusUrl("https://x.com/home"))
        assertNull(UrlExtractor.extractXStatusUrl("https://twitter.com/explore"))
    }
}
