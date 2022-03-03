package org.smartregister.p2p.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Locale
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 25-02-2022.
 */

fun startP2PScreen(context: Activity) {
    context.startActivity(Intent(context, P2PDeviceSearchActivity::class.java))
}

fun getUsername(context: Context) : String {
    return "demo"
}

fun getDeviceName(context: Context) : String = "${getUsername(context)} (${getDeviceModel()})"

fun getDeviceModel(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
        model.capitalize()
    } else {
        manufacturer.capitalize() + " " + model
    }
}

fun String.capitalize() : String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }