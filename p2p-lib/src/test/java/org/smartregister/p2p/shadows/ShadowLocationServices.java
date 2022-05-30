package org.smartregister.p2p.shadows;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.SettingsClient;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 19-05-2022.
 */
@Implements(LocationServices.class)
public class ShadowLocationServices {

    private static SettingsClient settingsClient;

    @Implementation
    public static SettingsClient getSettingsClient(@NonNull Activity var0) {
        if (settingsClient == null) {
            return ReflectionHelpers.callConstructor(SettingsClient.class);
        }

        return settingsClient;
    }

    public static void setSettingsClient(SettingsClient settingsClient) {
        ShadowLocationServices.settingsClient = settingsClient;
    }
}
