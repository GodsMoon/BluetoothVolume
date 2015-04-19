package com.nightshadelabs.bluetoothvolume;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import de.greenrobot.event.EventBus;

public class BluetoothReceiver extends BroadcastReceiver{
    private static final String KEY_CONNECTED = "KEY_CONNECTED";
    private static final int NOTIFICATION_ID = 777;
    private boolean isA2DPConnecting = false;
    private boolean isHeadsetConnecting = false;
    private boolean disconnecting = false;

    //GoogleAnalyticsTracker tracker;

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedSettings = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = sharedSettings.edit();

        if (BuildConfig.DEBUG) {
            Log.e("class", "Action " + intent.getAction());
        }

        //if BT is turned off
        if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF ||
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_TURNING_OFF){
                disconnecting = true;
            }

//        EXTRA_STATE - The current state of the profile.
//                EXTRA_PREVIOUS_STATE- The previous state of the profile.
//                EXTRA_DEVICE - The remote device.
//        EXTRA_STATE or EXTRA_PREVIOUS_STATE can be any of STATE_DISCONNECTED, STATE_CONNECTING, STATE_CONNECTED, STATE_DISCONNECTING.
        }else if(intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)){
            int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE,-1);
            if(state == BluetoothA2dp.STATE_CONNECTED){
                isA2DPConnecting = true;
            }else  if(state == BluetoothA2dp.STATE_DISCONNECTED){
                disconnecting = true;
            }
            //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        }else if(intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)){
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,-1);
            if(state == BluetoothHeadset.STATE_CONNECTED){
                isHeadsetConnecting = true;
            }else  if(state == BluetoothHeadset.STATE_DISCONNECTED){
                disconnecting = true;
            }
        }

        if(disconnecting) {
            //only disconnect if we previously connected
            if (sharedSettings.getBoolean(KEY_CONNECTED, false)) {
                disconnecting = true;

                editor.putBoolean(KEY_CONNECTED, false);
            }else{
                disconnecting = false;
            }
        }

        /*tracker = GoogleAnalyticsTracker.getInstance();

        // Start the tracker in manual dispatch mode...
        tracker.start("UA-19746576-1", 20, context);
        tracker.trackPageView("/BluetoothReceiver");*/

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (isA2DPConnecting || isHeadsetConnecting) {

            editor.putBoolean(KEY_CONNECTED, true);

            //tracker.trackPageView("/ACTION_ACL_CONNECTED");
            boolean somethingHappened = false;

            int blueVolume = 0; //recycle this variable for each type of volume
            if (sharedSettings.getBoolean(Main.KEY_BLUE_MEDIA_CHECKBOX, false)) {
                editor.putInt(Main.KEY_GREEN_MEDIA_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)); //save the old volume
                blueVolume = sharedSettings.getInt(Main.KEY_BLUE_MEDIA_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, blueVolume, 0);
                somethingHappened = true;

                if (BuildConfig.DEBUG)
                    Log.d("onReceive", "KEY_BLUE_MEDIA_CHECKBOX");
            }

            if (sharedSettings.getBoolean(Main.KEY_BLUE_RING_CHECKBOX, false)) {
                editor.putInt(Main.KEY_GREEN_RING_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_RING));  //save the old volume
                blueVolume = sharedSettings.getInt(Main.KEY_BLUE_RING_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, blueVolume, 0);
                somethingHappened = true;

                if (BuildConfig.DEBUG)
                    Log.d("onReceive", "KEY_BLUE_RING_CHECKBOX");
            }

            if (sharedSettings.getBoolean(Main.KEY_BLUE_CALL_CHECKBOX, false)) {
                editor.putInt(Main.KEY_GREEN_CALL_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));  //save the old volume
                blueVolume = sharedSettings.getInt(Main.KEY_BLUE_CALL_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, blueVolume, 0);
                somethingHappened = true;

                if (BuildConfig.DEBUG)
                    Log.d("onReceive", "KEY_BLUE_CALL_CHECKBOX");
            }

            if (sharedSettings.getBoolean(Main.KEY_BLUE_NOTIFY_CHECKBOX, false)) {
                editor.putInt(Main.KEY_GREEN_NOTIFY_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION));  //save the old volume
                blueVolume = sharedSettings.getInt(Main.KEY_BLUE_NOTIFY_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, blueVolume, 0);
                somethingHappened = true;

                if (BuildConfig.DEBUG)
                    Log.d("onReceive", "KEY_BLUE_NOTIFY_CHECKBOX");
            }

            if (sharedSettings.getBoolean(Main.KEY_CARE_HOME, false)) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "Bluetooth Volume");
                wl.acquire();


                UiModeManager uiMode = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
                uiMode.enableCarMode(UiModeManager.ENABLE_CAR_MODE_GO_CAR_HOME);
                try {
                    Thread.sleep((long) 3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                wl.release();

                if (BuildConfig.DEBUG)
                    Log.d("onReceive", "KEY_CARE_HOME");
            }

            editor.commit();

            if (somethingHappened){
                createNotification(context);
            }

            //FlurryAgent.onEvent("ACTION_ACL_CONNECTED",null);

            try {
                Thread.sleep((long) 3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            EventBus.getDefault().post(new BlueDevice(true));

        } else if (disconnecting) {
            if (BuildConfig.DEBUG)
                Log.i("ACTION_ACL_DISCONNECTED", "");

            //tracker.trackPageView("/ACTION_ACL_DISCONNECTED");
            boolean somethingHappened = false;

            int restoreSavedVolume = 0; //recycle this variable for each type of volume
            if (sharedSettings.getBoolean(Main.KEY_BLUE_MEDIA_CHECKBOX, false)) {
                editor.putInt(Main.KEY_BLUE_MEDIA_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)); //save for next time

                restoreSavedVolume = sharedSettings.getInt(Main.KEY_GREEN_MEDIA_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreSavedVolume, 0);
                somethingHappened = true;
            }

            if (sharedSettings.getBoolean(Main.KEY_BLUE_RING_CHECKBOX, false)) {
                editor.putInt(Main.KEY_BLUE_RING_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_RING)); //save for next time

                restoreSavedVolume = sharedSettings.getInt(Main.KEY_GREEN_RING_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, restoreSavedVolume, 0);
                somethingHappened = true;
            }

            if (sharedSettings.getBoolean(Main.KEY_BLUE_CALL_CHECKBOX, false)) {
                editor.putInt(Main.KEY_BLUE_CALL_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)); //save for next time

                restoreSavedVolume = sharedSettings.getInt(Main.KEY_GREEN_CALL_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, restoreSavedVolume, 0);
                somethingHappened = true;
            }

            if (sharedSettings.getBoolean(Main.KEY_BLUE_NOTIFY_CHECKBOX, false)) {
                editor.putInt(Main.KEY_BLUE_NOTIFY_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)); //save for next time

                restoreSavedVolume = sharedSettings.getInt(Main.KEY_GREEN_NOTIFY_VOLUME, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, restoreSavedVolume, 0);
                somethingHappened = true;
            }

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) //UiModeManager is new in froyo
            {
                try {
                    UiModeManager uiMode = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
                    if (uiMode.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR)
                        uiMode.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME);
                } catch (Exception e) {
                    //just in case
                }
            }

            if (somethingHappened){
                dismissNotification(context);
            }

            //FlurryAgent.onEvent("ACTION_ACL_DISCONNECTED",null);

            try {
                Thread.sleep((long) 3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            EventBus.getDefault().post(new BlueDevice(false));
        }

        /*FlurryAgent.onEndSession(context);
        tracker.stop(); // Stop the tracker when it is no longer needed.*/
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void createNotification(Context context) {
        Notification.Builder builder =
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_device_bluetooth_volume)
                        .setTicker("Adjusting Bluetooth Volumes")
                        .setContentTitle("Bluetooth Volumes Active")
                        .setContentText("rock n' roll");

        Bitmap bigIcon =  BitmapFactory.decodeResource(context.getResources(),
                R.drawable.icon);

        builder.setLargeIcon(bigIcon);

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(Notification.PRIORITY_MIN);
        }

        Intent openApp = new Intent(context, Main.class);
        openApp.setAction(Intent.ACTION_MAIN);
        openApp.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent openAppIntent = PendingIntent.getActivity(context, 0, openApp, 0);

        builder.setContentIntent(openAppIntent);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void dismissNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(NOTIFICATION_ID);

        Toast.makeText(context, "Adjusting Bluetooth Volumes", Toast.LENGTH_SHORT).show();
    }

    public static boolean isBluetoothConnected(Context context){

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if(BuildConfig.DEBUG)
            Log.i("isBluetoothA2dpOn", ""+audioManager.isBluetoothA2dpOn());
        if(BuildConfig.DEBUG)
            Log.i("isBluetoothScoOn", ""+audioManager.isBluetoothScoOn());

//        if(audioManager.isBluetoothA2dpOn() || audioManager.isBluetoothScoOn()){
//            return true;
//        }else{
//            return false;
//        }


        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter(context);

        // Establish connection to the proxy.
        //mBluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET);


        //Only check A2DP and HEADSET profiles. We don't want fitness or LE devices
        boolean isConnected = false;

        int state = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP);

        if(state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING)
            isConnected = true;

        state = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);

        if(state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING)
            isConnected = true;

        if(BuildConfig.DEBUG)
            Log.i("isConnected", ""+isConnected);

        return isConnected;

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static BluetoothAdapter getBluetoothAdapter(Context context){
        BluetoothAdapter mBluetoothAdapter;

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2){
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }else{

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return mBluetoothAdapter;

    }

    BluetoothHeadset mBluetoothHeadset;
    private BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = (BluetoothHeadset) proxy;
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            }
        }
    };

    // eventBus
    public class BlueDevice {

        private boolean isBluetoothDeviceConnected;

        public BlueDevice(boolean isBluetoothDeviceConnected) {
            this.isBluetoothDeviceConnected = isBluetoothDeviceConnected;
        }

        public boolean isConnected() {
            return this.isBluetoothDeviceConnected;
        }
    }


    public static boolean isBluetoothConnectedRecursiveDebug(Context context){
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        try {
            Thread.sleep((long) 100);
        } catch (InterruptedException e) {}

        if(BuildConfig.DEBUG)
            Log.i("isBluetoothA2dpOn", ""+audioManager.isBluetoothA2dpOn());

        if(audioManager.isBluetoothA2dpOn() == true)
            return true;

        while(audioManager.isBluetoothA2dpOn() == false)
            isBluetoothConnected(context);

        return false;
    }
}
