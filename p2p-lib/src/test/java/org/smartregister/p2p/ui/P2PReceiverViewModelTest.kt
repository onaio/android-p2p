package org.smartregister.p2p.ui

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncReceiverHandler
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity
import org.smartregister.p2p.search.ui.P2PReceiverViewModel
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

class P2PReceiverViewModelTest: RobolectricTest() {
    private lateinit var context: P2PDeviceSearchActivity
    private lateinit var dataSharingStrategy: DataSharingStrategy
    private lateinit var p2PReceiverViewModel: P2PReceiverViewModel
    private lateinit var syncReceiverHandler: SyncReceiverHandler
    private lateinit var manifest: Manifest

    @Before
    fun setUp() {
        clearAllMocks()
        context = mockk()
        dataSharingStrategy = mockk(relaxed = true)
        syncReceiverHandler = mockk(relaxed = true)

        p2PReceiverViewModel = spyk(P2PReceiverViewModel(context, dataSharingStrategy))
        val dataType = DataType(name = "Patient", type = DataType.Filetype.JSON, position = 1 )
        manifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
        every { p2PReceiverViewModel.listenForIncomingManifest() } answers {manifest}
        ReflectionHelpers.setField(p2PReceiverViewModel, "syncReceiverHandler", syncReceiverHandler)
    }

    @Test
    fun `getSendingDeviceAppLifetimeKey() return correct sending device appLifetime key`() {
        val appLifetimeKey = "ecd51f4c-ad4f-46a5-bda0-df38c5196aa8"
        ReflectionHelpers.setField(p2PReceiverViewModel, "sendingDeviceAppLifetimeKey", appLifetimeKey)
        val sendingDeviceAppLifeTimeKey = p2PReceiverViewModel.getSendingDeviceAppLifetimeKey()
        Assert.assertNotNull(sendingDeviceAppLifeTimeKey)
        Assert.assertEquals(appLifetimeKey, sendingDeviceAppLifeTimeKey)
    }

    @Test
    fun `processIncomingManifest()  calls syncReceiver#processManifest()` () {
        p2PReceiverViewModel.processIncomingManifest()
        verify (exactly = 1) { syncReceiverHandler.processManifest(manifest = manifest) }
    }

    @Test
    fun `processIncomingManifest() with sync complete manifest value  calls p2PReceiverViewModel#handleDataTransferCompleteManifest()` () {
        val dataType = DataType(name = Constants.SYNC_COMPLETE, type = DataType.Filetype.JSON, position = 1 )
        manifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
        every { p2PReceiverViewModel.listenForIncomingManifest() } answers {manifest}
        p2PReceiverViewModel.processIncomingManifest()
        verify (exactly = 1) { p2PReceiverViewModel.handleDataTransferCompleteManifest() }
    }
}