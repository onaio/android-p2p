package org.smartregister.p2p.search.ui.p2p

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.search.ui.theme.ProfileBackgroundColor

const val P2P_SCREEN_TOP_BAR_TEST_TAG = "p2pScreenTopBarTestTag"
const val P2P_SCREEN_TOP_BAR_ICON_TEST_TAG = "p2pScreenTopBarIconTestTag"

@Composable
fun P2PScreen(
  modifier: Modifier = Modifier
) {
  Scaffold (
    topBar = {
      TopAppBar(
        modifier = modifier.testTag(P2P_SCREEN_TOP_BAR_TEST_TAG),
        title = {"Transfer Data"},
        navigationIcon = {
          IconButton(onClick = {  }) {
            Icon(
              Icons.Filled.ArrowBack,
              null,
              modifier = modifier.testTag(P2P_SCREEN_TOP_BAR_ICON_TEST_TAG)
            )
          }
        },
        elevation = 0.dp
      )
    }
    ) { innerPadding ->
    Box(
      modifier = modifier.background(ProfileBackgroundColor).fillMaxHeight().padding(innerPadding)
    )
    {
      Text("we are here")
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewP2PScreen() {
  P2PScreen()
}