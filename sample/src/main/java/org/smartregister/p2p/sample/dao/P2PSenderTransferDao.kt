package org.smartregister.p2p.sample.dao

import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import java.util.TreeSet

class P2PSenderTransferDao() :SenderTransferDao {
    override fun getP2PDataTypes(): TreeSet<DataType> {
        return TreeSet<DataType>()
    }

    override fun getTotalRecordCount(): Long {
        return 0
    }

    override fun getJsonData(dataType: DataType, lastRecordId: Long, batchSize: Int): JsonData? {
        return JsonData()
    }
}