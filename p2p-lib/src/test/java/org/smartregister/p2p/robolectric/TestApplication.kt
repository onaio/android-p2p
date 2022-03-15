package org.smartregister.p2p.robolectric

import android.app.Application
import org.smartregister.p2p.R

/**
 * This class is used to set the theme
 * in order to fix the following exception when running activity tests
 * java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_MaterialComponents_DayNight_DarkActionBar);
    }
}