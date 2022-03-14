package org.smartregister.p2p

import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.rules.ExpectedException


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 14-03-2022.
 */
internal class P2PLibraryTest {

    @get:Rule
    var thrown = ExpectedException.none()

    @Test
    fun getInstanceShouldThrowExceptionWhenInstanceIsNull() {
        thrown.expect(IllegalStateException::class.java)
        thrown.expectMessage(
            "Instance does not exist!!! Call P2PLibrary.init method"
                    + "in the onCreate method of "
                    + "your Application class "
        )
        P2PLibrary.getInstance()
    }
}