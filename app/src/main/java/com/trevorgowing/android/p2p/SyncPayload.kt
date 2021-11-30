package com.trevorgowing.android.p2p

import kotlinx.serialization.Serializable

@Serializable
data class SyncPayload(val message: String)