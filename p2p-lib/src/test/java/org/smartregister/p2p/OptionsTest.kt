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
package org.smartregister.p2p

import android.content.Context
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.smartregister.p2p.dao.ReceiverTransferDao
import org.smartregister.p2p.dao.SenderTransferDao

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 14-03-2022. */
class OptionsTest {

  lateinit var options: P2PLibrary.Options
  lateinit var context: Context
  lateinit var dbPassphrase: String
  lateinit var username: String
  lateinit var senderTransferDao: SenderTransferDao
  lateinit var receiverTransferDao: ReceiverTransferDao

  @Before
  fun setUp() {
    context = Mockito.mock(Context::class.java)
    receiverTransferDao = Mockito.mock(ReceiverTransferDao::class.java)
    senderTransferDao = Mockito.mock(SenderTransferDao::class.java)
    dbPassphrase = "some-db-passphrase"
    username = "john-doe"

    options =
      P2PLibrary.Options(context, dbPassphrase, username, senderTransferDao, receiverTransferDao)
  }

  @Test
  fun testOptionsConfig() {
    Assert.assertEquals(options.context, context)
    Assert.assertEquals(options.dbPassphrase, dbPassphrase)
    Assert.assertEquals(options.username, username)
    Assert.assertEquals(options.senderTransferDao, senderTransferDao)
    Assert.assertEquals(options.receiverTransferDao, receiverTransferDao)
  }
}
