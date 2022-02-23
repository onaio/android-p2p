package org.smartregister.p2p

import kotlinx.serialization.Serializable

@Serializable
data class SyncPayload(val message: String)