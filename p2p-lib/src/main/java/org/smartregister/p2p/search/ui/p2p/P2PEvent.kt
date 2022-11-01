package org.smartregister.p2p.search.ui.p2p

sealed class P2PEvent {

  object ReceiveData : P2PEvent()

  object SendData : P2PEvent()
}