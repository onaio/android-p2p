package org.smartregister.p2p.model

data class RecordCount(
    val totalRecordCount: Long = 0,
    val dataTypeTotalCountMap: HashMap<String, Long> = hashMapOf()
)
