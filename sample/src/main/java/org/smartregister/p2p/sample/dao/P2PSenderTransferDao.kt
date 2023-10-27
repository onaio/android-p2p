package org.smartregister.p2p.sample.dao

import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.model.RecordCount
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import java.util.TreeSet

class P2PSenderTransferDao() :SenderTransferDao {
    override fun getP2PDataTypes(): TreeSet<DataType> {
        return TreeSet<DataType>()
    }

    override fun getTotalRecordCount(highestRecordIdMap: HashMap<String, Long>): RecordCount {
        return RecordCount()
    }

    override fun getJsonData(
        dataType: DataType,
        lastRecordId: Long,
        batchSize: Int,
        offset: Int
    ): JsonData? {
        return JsonData()
    }
}