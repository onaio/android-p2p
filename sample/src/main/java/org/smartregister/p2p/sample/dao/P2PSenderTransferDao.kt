package org.smartregister.p2p.sample.dao

import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import java.util.*

class P2PSenderTransferDao() :SenderTransferDao {
    override fun getP2PDataTypes(): TreeSet<DataType> {
        TODO("Not yet implemented")
    }

    override fun getJsonData(dataType: DataType, lastRecordId: Long, batchSize: Int): JsonData? {
        TODO("Not yet implemented")
    }
}