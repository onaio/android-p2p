package org.smartregister.p2p

import android.content.Context
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 14-03-2022.
 */
class OptionsTest {

    lateinit var options: P2PLibrary.Options
    lateinit var context: Context
    lateinit var dbPassphrase: String
    lateinit var username: String

    @Before
    fun setUp() {
        context = Mockito.mock(Context::class.java)
        dbPassphrase = "some-db-passphrase"
        username = "john-doe"

        options = P2PLibrary.Options(
            context,
            dbPassphrase,
            username
        )
    }

    @Test
    fun testOptionsConfig() {
        Assert.assertEquals(options.context, context)
        Assert.assertEquals(options.dbPassphrase, dbPassphrase)
        Assert.assertEquals(options.username, username)
    }
}