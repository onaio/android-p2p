package org.smartregister.p2p.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import org.smartregister.p2p.utils.startP2PScreen

class SampleMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_main)

        findViewById<Button>(R.id.device_sync).setOnClickListener {
            startP2PScreen(this)
        }
    }
}