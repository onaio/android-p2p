package org.smartregister.p2p.search.ui.p2p.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.R
import org.smartregister.p2p.search.ui.theme.DefaultColor

@Composable
fun DeviceList(modifier: Modifier = Modifier) {
  Column(modifier = modifier.wrapContentWidth(Alignment.Start)) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      Text(text = stringResource(id = R.string.select_recipient_device))
      Icon(
        imageVector = Icons.Filled.Image,
        contentDescription = null,
        tint = DefaultColor.copy(0.8f)
      )
    }

  }
}

@Composable
fun PairDeviceRow(modifier: Modifier = Modifier) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    Icon(
      imageVector = Icons.Filled.Image,
      contentDescription = null,
      tint = DefaultColor.copy(0.8f)
    )
    Column(modifier = modifier.wrapContentWidth(Alignment.Start)) {
      Text(text = "James Phone")
      Text(
        text = stringResource(id = R.string.pairing),
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
    Button(onClick = { /*TODO*/ }) {
      Text(text = stringResource(id = R.string.pair))
    }
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewDeviceList() {
  DeviceList()
}

@Preview(showBackground = true)
@Composable
fun PreviewPairDeviceRow() {
  PairDeviceRow()
}