package org.smartregister.p2p

import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for SyncPayload class
 */
class SyncPayloadTest {

    @Test
    fun syncPayloadIsSetCorrectly() {
        val payload = SyncPayload("test")
        Assert.assertEquals("test", payload.message)
    }
}