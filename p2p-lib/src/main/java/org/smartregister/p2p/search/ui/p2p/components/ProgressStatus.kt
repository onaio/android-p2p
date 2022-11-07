package org.smartregister.p2p.search.ui.p2p.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.search.ui.theme.SuccessColor

@Composable
fun ProgressStatus(modifier: Modifier = Modifier) {
  Box(contentAlignment = Alignment.Center,
    modifier = modifier.fillMaxWidth().background(SuccessColor.copy(alpha = 0.2F)),

  ) {
    Icon(
      imageVector = Icons.Filled.Done,
      contentDescription = null,
      tint = DefaultColor.copy(0.8f)
    )
    CircularProgressIndicator(modifier = modifier.size(40.dp), strokeWidth = 2.dp)
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewProgressStatus() {
  ProgressStatus()
}