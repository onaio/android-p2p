package org.smartregister.p2p.search.ui.p2p

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.R

const val P2P_SCREEN_TOP_BAR_TEST_TAG = "p2pScreenTopBarTestTag"
const val P2P_SCREEN_TOP_BAR_ICON_TEST_TAG = "p2pScreenTopBarIconTestTag"
const val P2P_SYNC_IMAGE_TEST_TAG = "p2pSyncImageTestTag"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
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
    ) {
    Column(    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {

      Column(modifier = modifier.padding(4.dp), verticalArrangement = Arrangement.Center) {
        Image(
          painter = painterResource(R.drawable.ic_p2p),
          contentDescription = stringResource(id = R.string.device_to_device_sync_logo),
          modifier =
          modifier
            .align(Alignment.CenterHorizontally)
            .requiredHeight(120.dp)
            .requiredWidth(140.dp)
            .testTag(P2P_SYNC_IMAGE_TEST_TAG),
        )

        Spacer(modifier = modifier.height(40.dp))

        Row(modifier = modifier
          .fillMaxSize()) {
          Column() {
            Text(text = stringResource(id = R.string.send_data))
            Text(text = stringResource(id = R.string.tap_to_send_data_msg))
          }
        }
      }
    }

  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewP2PScreen() {
  P2PScreen()
}