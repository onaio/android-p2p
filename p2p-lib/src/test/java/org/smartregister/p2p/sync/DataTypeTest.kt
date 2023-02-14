/*
 * Copyright 2022-2023 Ona Systems, Inc
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
package org.smartregister.p2p.sync

import org.junit.Assert
import org.junit.Test
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.utils.Constants

class DataTypeTest : RobolectricTest() {

  @Test
  fun `constructor sets correct values`() {
    val dataType =
      DataType(name = Constants.SYNC_COMPLETE, type = DataType.Filetype.JSON, position = 0)
    Assert.assertEquals(Constants.SYNC_COMPLETE, dataType.name)
    Assert.assertEquals(DataType.Filetype.JSON, dataType.type)
    Assert.assertEquals(0, dataType.position)
  }

  @Test
  fun `compareTo() returns correct values`() {
    val syncCompleteDataType =
      DataType(name = Constants.SYNC_COMPLETE, type = DataType.Filetype.JSON, position = 0)
    val groupDataType = DataType(name = "Group", type = DataType.Filetype.JSON, position = 1)
    val patientDataType = DataType(name = "Patient", type = DataType.Filetype.JSON, position = 1)

    Assert.assertEquals(1, groupDataType.compareTo(syncCompleteDataType))
    Assert.assertEquals(-1, syncCompleteDataType.compareTo(groupDataType))
    Assert.assertEquals(0, patientDataType.compareTo(groupDataType))
  }
}
