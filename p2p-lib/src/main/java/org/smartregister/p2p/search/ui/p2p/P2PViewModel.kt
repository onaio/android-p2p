package org.smartregister.p2p.search.ui.p2p

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf

class P2PViewModel () : ViewModel() {
  val p2PUiState = mutableStateOf(P2PUiState())
}