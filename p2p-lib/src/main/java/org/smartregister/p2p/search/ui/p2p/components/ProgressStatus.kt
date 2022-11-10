/*
 * Copyright 2022 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartregister.p2p.search.ui.p2p.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.p2p.R
import org.smartregister.p2p.search.ui.theme.DefaultColor
import org.smartregister.p2p.search.ui.theme.SuccessColor

@Composable
fun ProgressStatusIndicator(
  modifier: Modifier = Modifier,
  showCircularProgressIndicator: Boolean = true
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier.wrapContentSize().background(SuccessColor.copy(alpha = 0.2F), shape = CircleShape),
  ) {
    Icon(imageVector = Icons.Filled.Done, contentDescription = null, tint = DefaultColor.copy(0.8f))
    if (showCircularProgressIndicator) {
      CircularProgressIndicator(modifier = modifier.size(40.dp), strokeWidth = 2.dp)
    }
  }
}

@Composable
fun ProgressStatusText(modifier: Modifier = Modifier, title: String?, message: String?) {
  Column(modifier = modifier.wrapContentWidth(Alignment.CenterHorizontally)) {
    if (!title.isNullOrBlank()) {
      Text(text = title!!, fontWeight = FontWeight.Bold)
    }

    if (!message.isNullOrBlank()) {
      Text(
        text = message,
        color = DefaultColor,
        modifier = modifier.wrapContentWidth(Alignment.Start)
      )
    }
  }
}

@Composable
fun DeclineAcceptAction(modifier: Modifier = Modifier) {
  Row(
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
  ) {
    Button(onClick = { /*TODO*/}, modifier.padding(end = 10.dp)) {
      Text(text = stringResource(id = R.string.decline))
    }
    Button(onClick = { /*TODO*/}, modifier.padding(start = 10.dp)) {
      Text(text = stringResource(id = R.string.pair))
    }
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewProgressStatusIndicator() {
  ProgressStatusIndicator()
}

@Preview(showBackground = true)
@Composable
fun PreviewProgressStatusText() {
  ProgressStatusText(title = "sample title", message = "sample message")
}

@Preview(showBackground = true)
@Composable
fun PreviewDeclineAcceptAction() {
  DeclineAcceptAction()
}
