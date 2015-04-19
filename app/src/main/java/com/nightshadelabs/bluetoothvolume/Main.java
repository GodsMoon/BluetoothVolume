package com.nightshadelabs.bluetoothvolume;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import de.greenrobot.event.EventBus;

public class Main extends Activity {

    private Context context;
    private AudioManager audioManager;
    private SharedPreferences sharedSettings;
    private Editor editor;

    @InjectView(R.id.MediaVolume)
    protected SeekBar mediaVolume;
    @InjectView(R.id.RingVolume)
    protected SeekBar ringVolume;
    @InjectView(R.id.CallVolume)
    protected SeekBar callVolume;
    @InjectView(R.id.NotifyVolume)
    protected SeekBar notifyVolume;

    @InjectView(R.id.BluetoothMediaVolume)
    protected SeekBar blueMediaVolume;
    @InjectView(R.id.BluetoothRingVolume)
    protected SeekBar blueRingVolume;
    @InjectView(R.id.BluetoothCallVolume)
    protected SeekBar blueCallVolume;
    @InjectView(R.id.BluetoothNotifyVolume)
    protected SeekBar blueNotifyVolume;


    private SeekBar activeMediaVolume;
    private SeekBar activeRingVolume;
    private SeekBar activeCallVolume;
    private SeekBar activeNotifyVolume;

    private SeekBar savedMediaVolume;
    private SeekBar savedRingVolume;
    private SeekBar savedCallVolume;
    private SeekBar savedNotifyVolume;


    @InjectView(R.id.BluetoothMediaCheckbox)
    protected CheckBox blueMediaCheckbox;
    @InjectView(R.id.BluetoothRingCheckbox)
    protected CheckBox blueRingCheckbox;
    @InjectView(R.id.BluetoothCallCheckbox)
    protected CheckBox blueCallCheckbox;
    @InjectView(R.id.BluetoothNotifyCheckbox)
    protected CheckBox blueNotifyCheckbox;

    @InjectView(R.id.ShouldLaunchCarHome)
    protected CheckBox shouldLaunchCarHome;

    //private BroadcastReceiver broadcastReceiver;
    private Calendar calendar;
    private AlarmManager alarmManager;

    private int maxMediaVolume = 0;

    protected static final String KEY_BLUE_MEDIA_VOLUME = "KEY_BLUE_VOLUME";
    protected static final String KEY_BLUE_RING_VOLUME = "KEY_BLUE_RING_VOLUME";
    protected static final String KEY_BLUE_CALL_VOLUME = "KEY_BLUE_CALL_VOLUME";
    protected static final String KEY_BLUE_NOTIFY_VOLUME = "KEY_BLUE_NOTIFY_VOLUME";

    protected static final String KEY_GREEN_MEDIA_VOLUME = "KEY_SAVE_VOLUME";
    protected static final String KEY_GREEN_RING_VOLUME = "KEY_SAVE_RING_VOLUME";
    protected static final String KEY_GREEN_CALL_VOLUME = "KEY_SAVE_CALL_VOLUME";
    protected static final String KEY_GREEN_NOTIFY_VOLUME = "KEY_SAVE_NOTIFY_VOLUME";


    protected static final String KEY_BLUE_MEDIA_CHECKBOX = "KEY_BLUE_MEDIA_CHECKBOX";
    protected static final String KEY_BLUE_RING_CHECKBOX = "KEY_BLUE_RING_CHECKBOX";
    protected static final String KEY_BLUE_CALL_CHECKBOX = "KEY_BLUE_CALL_CHECKBOX";
    protected static final String KEY_BLUE_NOTIFY_CHECKBOX = "KEY_BLUE_NOTIFY_CHECKBOX";

    protected static final String KEY_CARE_HOME = "KEY_CARE_HOME";
    protected static final String ACTION_STOP_RINGTONE = "stopRingtone";
    protected static final String ACTION_STOP_CALLTONE = "stopCalltone";

    public static final String FIRST_RUN = "FIRST_RUN";

    //GoogleAnalyticsTracker tracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        ButterKnife.inject(this);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
        //add a space to make things look more even
        actionBar.setTitle(" Bluetooth Volume");

        context = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        sharedSettings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedSettings.edit();

        alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initSounds();
        initViews();

        EventBus.getDefault().register(this); // register EventBus

        if(android.os.Build.VERSION.SDK_INT < 8) //less than froyo
            shouldLaunchCarHome.setVisibility(View.GONE);


        /*FlurryAgent.onStartSession(this, BuildConfig.FLURRY_KEY);
        Map<String,String> mp=new HashMap<String, String>();
        mp.put("mediaVolume", ""+mediaVolume.getProgress());
        mp.put("ringVolume", ""+ringVolume.getProgress());
        mp.put("callVolume", ""+callVolume.getProgress());
        mp.put("blueMediaVolume", ""+blueMediaVolume.getProgress());
        mp.put("blueRingVolume", ""+blueRingVolume.getProgress());
        mp.put("blueCallVolume", ""+blueCallVolume.getProgress());
        mp.put("DONATE", ""+BuildConfig.FLAVOR);


        FlurryAgent.onEvent("init",mp);

        tracker = GoogleAnalyticsTracker.getInstance();

        // Start the tracker in manual dispatch mode...
        tracker.start("UA-19746576-1", 20, this);
        tracker.trackPageView("/main");
        try {
            tracker.setProductVersion(getPackageName(), ""+getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_META_DATA).versionCode);
        } catch (NameNotFoundException e) {
            // won't happen
        }*/

    }

    private void initViews() {
        mediaVolume.setOnSeekBarChangeListener(seekListener);
        maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mediaVolume.setMax(maxMediaVolume);

        ringVolume.setOnSeekBarChangeListener(seekListener);
        ringVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));

        callVolume.setOnSeekBarChangeListener(seekListener);
        callVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));

        notifyVolume.setOnSeekBarChangeListener(seekListener);
        notifyVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));

        blueMediaVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        blueMediaVolume.setOnSeekBarChangeListener(seekListener);

        blueRingVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        blueRingVolume.setOnSeekBarChangeListener(seekListener);

        blueCallVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
        blueCallVolume.setOnSeekBarChangeListener(seekListener);

        blueNotifyVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        blueNotifyVolume.setOnSeekBarChangeListener(seekListener);
        
    }

    @Override
    protected void onResume() {
        super.onResume();

        blueMediaVolume.setEnabled(blueMediaCheckbox.isChecked());
        blueRingVolume.setEnabled(blueRingCheckbox.isChecked());
        blueCallVolume.setEnabled(blueCallCheckbox.isChecked());
        blueNotifyVolume.setEnabled(blueNotifyCheckbox.isChecked());

        shouldLaunchCarHome.setChecked(sharedSettings.getBoolean(KEY_CARE_HOME, false));
        blueMediaCheckbox.setChecked(sharedSettings.getBoolean(KEY_BLUE_MEDIA_CHECKBOX, false));
        blueRingCheckbox.setChecked(sharedSettings.getBoolean(KEY_BLUE_RING_CHECKBOX, false));
        blueCallCheckbox.setChecked(sharedSettings.getBoolean(KEY_BLUE_CALL_CHECKBOX, false));
        blueNotifyCheckbox.setChecked(sharedSettings.getBoolean(KEY_BLUE_NOTIFY_CHECKBOX, false));

        setVolumeLevels();

        //FlurryAgent.onPageView();
    }

    private void setVolumeLevels(){

        int savedMediaVolumeValue;
        int savedRingVolumeValue;
        int savedCallVolumeValue;
        int savedNotifyVolumeValue;

        if(BuildConfig.DEBUG)
            Log.i("isBluetoothA2dpOn Main", ""+audioManager.isBluetoothA2dpOn());
        if(BuildConfig.DEBUG)
            Log.i("isBluetoothScoOn Main", ""+audioManager.isBluetoothScoOn());

        //set active and saved volumes depending on bluetooth and enabled states.
        if(audioManager.isBluetoothA2dpOn() || audioManager.isBluetoothScoOn()){

            if(blueMediaVolume.isEnabled()) {
                activeMediaVolume = blueMediaVolume;
                savedMediaVolume = mediaVolume;
                savedMediaVolumeValue = sharedSettings.getInt(KEY_GREEN_MEDIA_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }else {
                activeMediaVolume = mediaVolume;
                savedMediaVolume = blueMediaVolume;
                savedMediaVolumeValue = sharedSettings.getInt(KEY_BLUE_MEDIA_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }

            if(blueRingVolume.isEnabled()) {
                activeRingVolume = blueRingVolume;
                savedRingVolume = ringVolume;
                savedRingVolumeValue = sharedSettings.getInt(KEY_GREEN_RING_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_RING));
            }else{
                activeRingVolume = ringVolume;
                savedRingVolume = blueRingVolume;
                savedRingVolumeValue = sharedSettings.getInt(KEY_BLUE_RING_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_RING));
            }

            if(blueCallVolume.isEnabled()) {
                activeCallVolume = blueCallVolume;
                savedCallVolume = callVolume;
                savedCallVolumeValue = sharedSettings.getInt(KEY_GREEN_CALL_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
            }else{
                activeCallVolume = callVolume;
                savedCallVolume = blueCallVolume;
                savedCallVolumeValue = sharedSettings.getInt(KEY_BLUE_CALL_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
            }

            if(blueNotifyVolume.isEnabled()) {
                activeNotifyVolume = blueNotifyVolume;
                savedNotifyVolume = notifyVolume;
                savedNotifyVolumeValue = sharedSettings.getInt(KEY_GREEN_NOTIFY_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
            }else{
                activeNotifyVolume = notifyVolume;
                savedNotifyVolume = blueNotifyVolume;
                savedNotifyVolumeValue = sharedSettings.getInt(KEY_BLUE_NOTIFY_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
            }

        }else{

            activeMediaVolume = mediaVolume;
            savedMediaVolume = blueMediaVolume;
            activeRingVolume = ringVolume;
            savedRingVolume = blueRingVolume;
            activeCallVolume = callVolume;
            savedCallVolume = blueCallVolume;
            activeNotifyVolume = notifyVolume;
            savedNotifyVolume = blueNotifyVolume;

            savedMediaVolumeValue = sharedSettings.getInt(KEY_BLUE_MEDIA_VOLUME, 0);
            savedRingVolumeValue = sharedSettings.getInt(KEY_BLUE_RING_VOLUME, 0);
            savedCallVolumeValue = sharedSettings.getInt(KEY_BLUE_CALL_VOLUME, 0);
            savedNotifyVolumeValue = sharedSettings.getInt(KEY_BLUE_NOTIFY_VOLUME, 0);

        }

        activeMediaVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        activeRingVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_RING));
        activeCallVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
        activeNotifyVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION));

        if(blueMediaCheckbox.isChecked())
            savedMediaVolume.setProgress(savedMediaVolumeValue);
        if(blueRingCheckbox.isChecked())
            savedRingVolume.setProgress(savedRingVolumeValue);
        if(blueCallCheckbox.isChecked())
            savedCallVolume.setProgress(savedCallVolumeValue);
        if(blueNotifyCheckbox.isChecked())
            savedNotifyVolume.setProgress(savedNotifyVolumeValue);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //FlurryAgent.onEndSession(this);
    }

    @Override
    protected void onDestroy() {
        //unregisterReceiver(broadcastReceiver);
        //tracker.stop(); // Stop the tracker when it is no longer needed.
        EventBus.getDefault().unregister(this); // unregister EventBus
        super.onDestroy();
    }

    // method that will be called when posting to eventBus
    public void onEventMainThread(BluetoothReceiver.BlueDevice event) {
        setVolumeLevels();

    }

    @OnCheckedChanged({R.id.BluetoothMediaCheckbox, R.id.BluetoothRingCheckbox, R.id.BluetoothCallCheckbox,  R.id.BluetoothNotifyCheckbox, R.id.ShouldLaunchCarHome})
    public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
        switch(buttonView.getId()) {

            case R.id.BluetoothMediaCheckbox:
                blueMediaVolume.setEnabled(blueMediaCheckbox.isChecked());
                editor.putBoolean(KEY_BLUE_MEDIA_CHECKBOX, isChecked);

                break;
            case R.id.BluetoothRingCheckbox:
                blueRingVolume.setEnabled(blueRingCheckbox.isChecked());
                editor.putBoolean(KEY_BLUE_RING_CHECKBOX, isChecked);
                break;

            case R.id.BluetoothCallCheckbox:
                blueCallVolume.setEnabled(blueCallCheckbox.isChecked());
                editor.putBoolean(KEY_BLUE_CALL_CHECKBOX, isChecked);
                break;

            case R.id.BluetoothNotifyCheckbox:
                blueNotifyVolume.setEnabled(blueNotifyCheckbox.isChecked());
                editor.putBoolean(KEY_BLUE_NOTIFY_CHECKBOX, isChecked);
                break;

            case R.id.ShouldLaunchCarHome:

                Intent car = new Intent(Intent.ACTION_MAIN);
                car.addCategory(Intent.CATEGORY_CAR_MODE);
                //car.setClassName("com.google.android.carhome", "com.google.android.carhome.CarHome");

                if (isIntentAvailable(context, car)) {
                    editor.putBoolean(KEY_CARE_HOME, isChecked);
                    editor.commit();
                } else {
                    if(isChecked) {
                        shouldLaunchCarHome.setChecked(false);

                        new AlertDialog.Builder(context)
                            .setMessage("You need to install a Car Home app to use this feature.")
                            .setTitle("Car Home not installed")
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://market.android.com/details?id=com.google.android.carhome")));
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=car+dock")));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                    }
                }

            }
        editor.commit();
        setVolumeLevels();
    }

    private OnSeekBarChangeListener seekListener = new OnSeekBarChangeListener(){

        @Override
        public void onProgressChanged(SeekBar seekBar, final int progress,
                                      boolean fromUser) {
            Log.v("progress", ""+progress+fromUser);

            switch(seekBar.getId()) {

                case R.id.MediaVolume:
                    if(fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, activeMediaVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_GREEN_MEDIA_VOLUME, progress);
                                editor.commit();

                            }
                        }).start();
                    }else{

                    }
                    break;

                case R.id.RingVolume:
                    if(fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, activeRingVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_GREEN_RING_VOLUME, progress);
                                editor.commit();
                            }
                        }).start();
                    }
                    break;

                case R.id.CallVolume:
                    if(fromUser){
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, activeCallVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_GREEN_CALL_VOLUME, progress);
                                editor.commit();
                            }
                        }).start();
                    }
                    break;

                case R.id.NotifyVolume:
                    if(fromUser){
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, activeNotifyVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_GREEN_NOTIFY_VOLUME, progress);
                                editor.commit();
                            }
                        }).start();
                    }
                    break;

                case R.id.BluetoothMediaVolume:
                    if(fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, activeMediaVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_BLUE_MEDIA_VOLUME, progress);
                                editor.commit();
                            }
                        }).start();
                    }

                    break;

                case R.id.BluetoothRingVolume:
                    if(fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, activeRingVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_BLUE_RING_VOLUME, progress);
                                editor.commit();
                            }
                        }).start();
                    }

                    break;

                case R.id.BluetoothCallVolume:
                    if(fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, activeCallVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_BLUE_CALL_VOLUME, progress);
                                editor.commit();
                            }
                        }).start();
                    }

                    break;

                case R.id.BluetoothNotifyVolume:
                    if(fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, activeNotifyVolume.getProgress(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        new Thread(new Runnable() {
                            public void run() {
                                editor.putInt(KEY_BLUE_NOTIFY_VOLUME, progress);
                                editor.commit();
                            }
                        }).start();
                    }

                    break;

            }

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            switch(seekBar.getId()) {

                case R.id.MediaVolume:
                case R.id.BluetoothMediaVolume:

                    if(playSound != null)
                        playSound.cancel(true);
                    playSound = new PlaySound();
                    playSound.execute(new Sound2Play(SOUND_BLUE, seekBar.getProgress()));

                    break;

                case R.id.RingVolume:
                case R.id.BluetoothRingVolume:

                    if(playSound != null)
                        playSound.cancel(true);
                    playSound = new PlaySound();
                    playSound.execute(new Sound2Play(SOUND_RING, seekBar.getProgress()));

                    break;

                case R.id.CallVolume:
                case R.id.BluetoothCallVolume:

                    if(playSound != null)
                        playSound.cancel(true);
                    playSound = new PlaySound();
                    playSound.execute(new Sound2Play(SOUND_CALL, seekBar.getProgress()));

                    break;

                case R.id.NotifyVolume:
                case R.id.BluetoothNotifyVolume:

                    if(playSound != null)
                        playSound.cancel(true);
                    playSound = new PlaySound();
                    playSound.execute(new Sound2Play(SOUND_NOTIFY, seekBar.getProgress()));

                    break;
            }

            Map<String,String> mp=new HashMap<String, String>();
            mp.put("seekBar", ""+seekBar.getProgress());
            mp.put("seekBarID", ""+seekBar);
            //FlurryAgent.onEvent("onStopTrackingTouch",mp);
        }

    };

    private void onVolumeKeyPressed(){

        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        activeMediaVolume.setProgress(currentVolume);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, activeMediaVolume.getProgress(), AudioManager.FLAG_PLAY_SOUND);
    }

    private SoundPool soundPool;
    private PlaySound playSound;
    private HashMap<Integer, Integer> soundPoolMap;
    private static final int SOUND_BLUE= 1;
    private static final int SOUND_RING = 2;
    private static final int SOUND_NOTIFY = 3;
    private static final int SOUND_CALL = 4;
    private static final int VOLUME_SETTINGS = 0;
    private static final int OTHER_APPS = 1;
    private AsyncPlayer asyncPlayer;

    private void initSounds() {

        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        soundPoolMap = new HashMap<Integer, Integer>();
        soundPoolMap.put(SOUND_BLUE , soundPool.load(context, R.raw.pop1, 1));

        asyncPlayer = new AsyncPlayer("blueMediaVolume");
    }

    private class Sound2Play{
        public int sound = 0;
        public int volume = 0;

        Sound2Play(int sound, int volume){
            this.sound = sound;
            this.volume = volume;
        }
    }

    private class PlaySound extends AsyncTask<Sound2Play, Void, Void> {

        int ONE_SECONDS = 1000;
        int TWO_SECONDS = 2000;

        @Override
        protected Void doInBackground(Sound2Play... sound2Plays) {

            Sound2Play sound2Play = sound2Plays[0];
            asyncPlayer.stop();

            switch (sound2Play.sound) {
                case SOUND_BLUE:

                    final int oldVolumeMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sound2Play.volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    soundPool.play(soundPoolMap.get(sound2Play.sound), 1, 1, 1, 0, 1f);

                    try {
                        Thread.sleep((long) TWO_SECONDS);
                    } catch (InterruptedException e) {}

                    if(isCancelled())
                        return null;

                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolumeMedia, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    break;
                case SOUND_RING:
                    final int oldVolumeRing = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, sound2Play.volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    try {
                        Thread.sleep((long) 500); // wait a little bit before playing sound. setStreamVolume is asynchronous.
                    } catch (InterruptedException e) {}

                    asyncPlayer.play(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), false, AudioManager.STREAM_RING);

                    try {
                        Thread.sleep((long) TWO_SECONDS);
                    } catch (InterruptedException e) {}

                    if(isCancelled())
                        return null;

                    asyncPlayer.stop();

                    try {
                        Thread.sleep((long) ONE_SECONDS); // wait a little bit longer before restoring volume because asyncPlayer is asynchronous
                    } catch (InterruptedException e) {}

                    if(isCancelled())
                        return null;

                    audioManager.setStreamVolume(AudioManager.STREAM_RING, oldVolumeRing, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    break;

                case SOUND_CALL:
                    final int oldVolumeCall = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, sound2Play.volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    asyncPlayer.play(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), false, AudioManager.STREAM_VOICE_CALL);

                    try {
                        Thread.sleep((long) ONE_SECONDS);
                    } catch (InterruptedException e) {} //

                    if(isCancelled())
                        return null;

                    asyncPlayer.stop();

                    if(isCancelled())
                        return null;

                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, oldVolumeCall, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    break;

                case SOUND_NOTIFY:

                    final int oldVolumeNotify = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, sound2Play.volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    asyncPlayer.play(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), false, AudioManager.STREAM_NOTIFICATION);

                    try {
                        Thread.sleep((long) ONE_SECONDS);
                    } catch (InterruptedException e) {} //

                    if(isCancelled())
                        return null;

                    asyncPlayer.stop();

                    if(isCancelled())
                        return null;

                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, oldVolumeNotify, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    break;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setVolumeLevels(); //Ringer and Notify mute each other so update levels
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        {
            asyncPlayer.stop();
            onVolumeKeyPressed();
        }

        return super.onKeyDown(keyCode, event);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, VOLUME_SETTINGS, 0, "System Sounds Shortcut").setIcon(R.drawable.ic_volume);
        menu.add(1, OTHER_APPS, 1, "More Apps from Developer").setIcon(R.drawable.nightshade);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case VOLUME_SETTINGS:

                Intent i = new Intent(Settings.ACTION_SOUND_SETTINGS);
                startActivity(i);
                return true;

            case OTHER_APPS:

                Intent goToMarket = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://search?q=pub:\"Nightshade Labs\"&referrer=utm_source%3DBluetoothVolume%26utm_medium%3DMore%2520Apps%2520Button%26utm_campaign%3Din%2520app%2520promo"));

                startActivity(goToMarket);
                return true;

        }
        return false;
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        //final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}