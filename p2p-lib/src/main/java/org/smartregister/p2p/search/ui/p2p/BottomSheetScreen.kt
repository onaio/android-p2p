package org.smartregister.p2p.search.ui.p2p

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.smartregister.p2p.R

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetScreen() {

  val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

  ModalBottomSheetLayout(
    sheetState = modalBottomSheetState,
    sheetContent =   {
    Scaffold(
      Modifier
        .fillMaxWidth()
    ) {
      Row(
      ) {
        Column() {
          Image(
            painter = painterResource(R.drawable.ic_p2p),
            contentDescription = stringResource(id = R.string.device_to_device_sync_logo),
            modifier =
            Modifier
              .align(Alignment.CenterHorizontally)
          )
        }
        Column() {
          Text(text = "Bottom sheet")
          Text(text = "sample description")
        }
      }
    }
  },
    scrimColor = Color.Black.copy(alpha = 0.32f)
  ) {
    val coroutineScope = rememberCoroutineScope()
    Button(
      onClick = { coroutineScope.launch {
      modalBottomSheetState.show()
    }
      }
    ) {

    }

  }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomSheetScreen() {
  BottomSheetScreen()
}