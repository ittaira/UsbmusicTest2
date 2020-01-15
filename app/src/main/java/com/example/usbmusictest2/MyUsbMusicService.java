package com.example.usbmusictest2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import java.net.URI;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MyUsbMusicService extends Service {

    final static String TAG = "MyUsbStreamingService",
                ACTION_MY_USB_STREAMING_INTENT_FILTER = MyUsbMusicService.class.getName() + "My_usb_streaming_change_song",
                EXTRA_USB_STREAMING_SONG_URI = MyUsbMusicService.class.getName() + "My_usb_streaming_song_uri",
                EXTRA_USB_STREAMING_PERFORM_TASK_ID = MyUsbMusicService.class.getName() + "My_usb_streaming_task_id",
                EXTRA_USB_STREAMING_VOLUME = MyUsbMusicService.class.getName() + "My_usb_streaming_volume",
                ACTION_USB_MEDIAPLAYER_SONG_COMPLETED = MyUsbMusicService.class.getName() + "My_usb_song_completed";

    final static int USB_STREAM_TASK_LOAD_SONG = 1,
                    USB_STREAM_TASK_RESUME_PLAYING = 2,
                    USB_STREAM_TASK_PAUSE_PLAYING = 3,
                    USB_STREAM_TASK_SUPPRESS_VOLUME = 4,
                    USB_STREAM_TASK_UNSUPPRESS_VOLUME = 5,
                    USB_STREAM_TASK_CHANGE_VOLUME = 6;

    private MediaPlayer myUsbMediaPlayer;
    IntentFilter myUsbStreamingIntentFilter;
    String mySongUriString;
    int myUsbStreamerPerformTaskId;
    int myUsbStreamerVolumeRxdFromIntent;
    private boolean isUsbMediaPlayerPlaying;

    Thread myUsbMusicThread;


    public MyUsbMusicService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "usb service created");
        myUsbStreamingIntentFilter = new IntentFilter(ACTION_MY_USB_STREAMING_INTENT_FILTER);
        myUsbMusicThread = new Thread(new IttaiUsbPlayerThread(startId));
        myUsbMusicThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (myUsbMediaPlayer != null){
            myUsbMediaPlayer.reset();
            myUsbMediaPlayer.release();
            myUsbMediaPlayer = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myUsbStreamingBroadcastReceiver);
        Log.d(TAG, "usb service finished");
    }

    final class IttaiUsbPlayerThread implements Runnable{
        int serviceId;
        public IttaiUsbPlayerThread(int serviceId) {
            this.serviceId = serviceId;
        }

        @Override
        public void run() {
            myUsbMediaPlayer = new MediaPlayer();
            myUsbMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            registerUsbStreamingBroadcastReceiver();
            //LocalBroadcastManager.getInstance(this).registerReceiver(myUsbStreamingBroadcastReceiver, myUsbStreamingIntentFilter);
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        }
    }

    private void registerUsbStreamingBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(myUsbStreamingBroadcastReceiver, myUsbStreamingIntentFilter);
    }

    private BroadcastReceiver myUsbStreamingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mySongUriString = intent.getStringExtra(EXTRA_USB_STREAMING_SONG_URI);
            myUsbStreamerPerformTaskId = intent.getIntExtra(EXTRA_USB_STREAMING_PERFORM_TASK_ID, 0);
            myUsbStreamerVolumeRxdFromIntent = intent.getIntExtra(EXTRA_USB_STREAMING_VOLUME, 3);

            switch (myUsbStreamerPerformTaskId){
                case USB_STREAM_TASK_LOAD_SONG:{
                    startPlayingNewSong(mySongUriString);
                    break;
                }
                case USB_STREAM_TASK_RESUME_PLAYING:{
                    if ((myUsbMediaPlayer != null) && !isUsbMediaPlayerPlaying){
                        if (!myUsbMediaPlayer.isPlaying())
                            myUsbMediaPlayer.start();
                    }
                    break;
                }
                case USB_STREAM_TASK_PAUSE_PLAYING:{
                    if (myUsbMediaPlayer.isPlaying()){
                        myUsbMediaPlayer.pause();
                        isUsbMediaPlayerPlaying = false;
                    }
                    break;
                }
                case USB_STREAM_TASK_SUPPRESS_VOLUME:{
                    if (myUsbMediaPlayer.isPlaying()){
                        myUsbMediaPlayer.setVolume(0.05f, 0.05f);
                    }
                    break;
                }
                case USB_STREAM_TASK_UNSUPPRESS_VOLUME:{
                    if (myUsbMediaPlayer.isPlaying()){
                        myUsbMediaPlayer.setVolume(1,1);
                    }
                    break;
                }
                case USB_STREAM_TASK_CHANGE_VOLUME:{
                    if (myUsbMediaPlayer.isPlaying()){
                        float convertedVol = ((float) myUsbStreamerVolumeRxdFromIntent)/15;
                        myUsbMediaPlayer.setVolume(convertedVol, convertedVol);
                    }
                    break;
                }
                default: break;
            }

        }
    };

    private void startPlayingNewSong(String mySongUriString) {
        if (myUsbMediaPlayer.isPlaying()){
            myUsbMediaPlayer.pause();
            myUsbMediaPlayer.reset();
        }
        new UsbPlayerAsyncTask().execute(mySongUriString);
        isUsbMediaPlayerPlaying = true;
    }


    class UsbPlayerAsyncTask extends AsyncTask<String, Void, Boolean>{
        @Override
        protected Boolean doInBackground(String... strings) {
            Boolean isPrepared = false;
            try {
                myUsbMediaPlayer.reset();
                Uri uri = Uri.parse(strings[0]);
                myUsbMediaPlayer.setDataSource(getApplicationContext(), uri);
                float convertedVol = ((float) myUsbStreamerVolumeRxdFromIntent)/15;
                myUsbMediaPlayer.setVolume(convertedVol, convertedVol);
                myUsbMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Log.d(TAG, "song onCompletion called");
                        //send broadcast to play next song
                        Intent intent = new Intent(ACTION_USB_MEDIAPLAYER_SONG_COMPLETED);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }
                });
                myUsbMediaPlayer.prepare();
                isPrepared = true;
            }catch (Exception e){
                isPrepared = false;
                isUsbMediaPlayerPlaying = false;
                e.printStackTrace();
            }
            return isPrepared;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            myUsbMediaPlayer.start();
        }
    }

}
