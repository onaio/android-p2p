package org.smartregister.p2p.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.sample.dao.P2PSenderTransferDao
import org.smartregister.p2p.utils.startP2PScreen

class SampleMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_main)

        // Init P2PLibrary
        val p2POptions = P2PLibrary.Options(context = this,
            dbPassphrase = "demo",
            username = "demo"
        )
        P2PLibrary().init(p2POptions)

        findViewById<Button>(R.id.device_sync).setOnClickListener {
            startP2PScreen(this)
        }
    }
}