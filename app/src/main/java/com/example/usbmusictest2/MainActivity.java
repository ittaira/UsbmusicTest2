package com.example.usbmusictest2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.example.usbmusictest2.MyUsbMusicService.ACTION_MY_USB_STREAMING_INTENT_FILTER;
import static com.example.usbmusictest2.MyUsbMusicService.ACTION_USB_MEDIAPLAYER_SONG_COMPLETED;
import static com.example.usbmusictest2.MyUsbMusicService.EXTRA_USB_STREAMING_PERFORM_TASK_ID;
import static com.example.usbmusictest2.MyUsbMusicService.EXTRA_USB_STREAMING_SONG_URI;
import static com.example.usbmusictest2.MyUsbMusicService.USB_STREAM_TASK_LOAD_SONG;
import static com.example.usbmusictest2.MyUsbMusicService.USB_STREAM_TASK_PAUSE_PLAYING;
import static com.example.usbmusictest2.MyUsbMusicService.USB_STREAM_TASK_RESUME_PLAYING;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private Button mChooseUsbStreamingButton;
    private Button mShuffleSongsButton;
    private Button mOrganizeSongsButton;
    private Button mNextSongButton;
    private Button mPrevSongButton;
    private Switch mIsMusicPlayingSwitch;
    private ArrayList<Songs> songsArrayList;
    private int currentSongPlayingIndex;
    //protected static Context myActivityContext;

    private RecyclerView mSongRecyclerView;
    private RecyclerView.Adapter mSongAdapter;
    private RecyclerView.LayoutManager msongLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //myActivityContext = getApplicationContext();
        songsArrayList = new ArrayList<>();

        mChooseUsbStreamingButton = findViewById(R.id.button_select_usb);
        mChooseUsbStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Uri mySongLocationUri = Uri.parse("file:///");
                songsArrayList.clear();
                mSongAdapter.notifyDataSetChanged();
                openDirectory(mySongLocationUri);

            }
        });

        mShuffleSongsButton = findViewById(R.id.button_suffle_list);
        mShuffleSongsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (songsArrayList != null && mSongRecyclerView != null){
                    Collections.shuffle(songsArrayList);
                    mSongAdapter.notifyDataSetChanged();
                }
            }
        });

        mOrganizeSongsButton = findViewById(R.id.button_order_list);
        mOrganizeSongsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Collections.sort(songsArrayList, new Comparator<Songs>() {
                    @Override
                    public int compare(Songs songs, Songs t1) {
                        return songs.getAtrist().toUpperCase().compareTo(t1.getAtrist().toUpperCase());
                    }
                });
                mSongAdapter.notifyDataSetChanged();
            }

        });

        mNextSongButton = findViewById(R.id.button_next_song);
        mNextSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (songsArrayList.size() > 0){
                    currentSongPlayingIndex ++;
                    if (currentSongPlayingIndex >= songsArrayList.size()){
                        currentSongPlayingIndex = 0;
                    }
                    sendBroadcastToUsbStreamService(songsArrayList.get(currentSongPlayingIndex).getSongUri().toString(), USB_STREAM_TASK_LOAD_SONG);
                }
            }
        });

        mPrevSongButton = findViewById(R.id.button_prev_song);
        mPrevSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (songsArrayList.size() > 0){
                    currentSongPlayingIndex --;
                    if (currentSongPlayingIndex < 0){
                        currentSongPlayingIndex = (songsArrayList.size() - 1);
                    }
                    sendBroadcastToUsbStreamService(songsArrayList.get(currentSongPlayingIndex).getSongUri().toString(),USB_STREAM_TASK_LOAD_SONG);
                }
            }
        });

        mIsMusicPlayingSwitch = findViewById(R.id.switch_is_playing);
        mIsMusicPlayingSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsMusicPlayingSwitch.isChecked()){
                    mIsMusicPlayingSwitch.setText("playing");
                    sendBroadcastToUsbStreamService("", USB_STREAM_TASK_RESUME_PLAYING);
                }else {
                    mIsMusicPlayingSwitch.setText("paused");
                    sendBroadcastToUsbStreamService("", USB_STREAM_TASK_PAUSE_PLAYING);
                }
            }
        });

        mSongRecyclerView = findViewById(R.id.recycler_view_songs);
        mSongRecyclerView.setHasFixedSize(true);
        msongLayoutManager = new LinearLayoutManager(this);
        mSongRecyclerView.setLayoutManager(msongLayoutManager);
        mSongAdapter = new MySongAdapter(songsArrayList);
        mSongRecyclerView.setAdapter(mSongAdapter);
        mSongRecyclerView.addItemDecoration(new DividerItemDecoration(mSongRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        LocalBroadcastManager.getInstance(this).registerReceiver(songClickBroadcastReceiver, new IntentFilter("song_chosen_from_storage_intent"));
        LocalBroadcastManager.getInstance(this).registerReceiver(songFinishedPlayingReceiver, new IntentFilter(ACTION_USB_MEDIAPLAYER_SONG_COMPLETED));

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(songClickBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(songFinishedPlayingReceiver);
        getApplicationContext().stopService(new Intent(getApplicationContext(), MyUsbMusicService.class));
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 14 && resultCode == Activity.RESULT_OK){
            Uri uri = null;
            if (data != null){
                uri = data.getData();
                //Log.d(TAG, "uri found: " + uri);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, uri);
                for (DocumentFile file : pickedDir.listFiles()){
                    //Log.d(TAG, "found file: " + file.getName() + " with size: " + file.length() + " type: " + file.getType());
                    Uri fileUri = file.getUri();
                    //Log.d(TAG, "found file uri: " + fileUri);
                    if (file.getType() != null){
                        if (file.getType().contains("audio") || file.getType().contains("mp3")){
                            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                            metadataRetriever.setDataSource(MainActivity.this, fileUri);
                            String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                            String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                            String album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                            String fileName = file.getName();
                            Log.d(TAG, "file name: " + fileName);
                            Songs song = new Songs(fileUri, title, artist, album, fileName);
                            songsArrayList.add(song);
                            mSongAdapter.notifyDataSetChanged();
                            //Activity activity = getParent().startService(new Intent(getApplicationContext(), MyUsbMusicService.class)); //for using in fragment

                        }
                    }
                }
                if (songsArrayList.size() > 0) {
                    getApplicationContext().startService(new Intent(getApplicationContext(), MyUsbMusicService.class));
                }

                //Log.d(TAG, "songsArrayList length: " + songsArrayList.size());
                //Log.d(TAG, "songsArrayList: " + songsArrayList);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void openDirectory(Uri mySongLocationUri) {
        //choose a directory using the system's file picker
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        //provide read access to files and sub-directories in the user selected directory:
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //optionally, specify a URI for the directory that should be opened in the system file picker when it loads:
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mySongLocationUri);
        //Log.d(TAG, "openning activity for result");
        startActivityForResult(intent,  14);

    }

    private void playMedia(Uri uri) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(MainActivity.this, uri);
            String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            //Log.d(TAG, "title: " + title + " artist: " + artist);
        }catch (IOException e){
            e.printStackTrace();
        }
        try {
            mediaPlayer.prepare();
        }catch (IOException e){
            e.printStackTrace();
        }
        mediaPlayer.start();
    }

    private final BroadcastReceiver songClickBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String uriValueExtra = intent.getStringExtra("uriValue");
            Log.d(TAG, "uri rxd by broadcast: " + uriValueExtra);
            currentSongPlayingIndex = intent.getIntExtra("index_number", -1);
            //playMedia(Uri.parse(uriValueExtra));  //plays on main thread
            // below moved to onActivityResult:
            //Activity activity = getParent().startService(new Intent(getApplicationContext(), MyUsbMusicService.class)); //for using in fragment
            //getApplicationContext().startService(new Intent(getApplicationContext(), MyUsbMusicService.class));

            mIsMusicPlayingSwitch.setChecked(true);
            mIsMusicPlayingSwitch.setText("playing");
            sendBroadcastToUsbStreamService(uriValueExtra, USB_STREAM_TASK_LOAD_SONG);
            /*
            //broadcast to service:
            Intent intentToBroadcastToUsbStreamingService = new Intent(ACTION_MY_USB_STREAMING_INTENT_FILTER);
            intentToBroadcastToUsbStreamingService.putExtra(EXTRA_USB_STREAMING_PERFORM_TASK_ID, USB_STREAM_TASK_LOAD_SONG);
            intentToBroadcastToUsbStreamingService.putExtra(EXTRA_USB_STREAMING_SONG_URI, uriValueExtra);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentToBroadcastToUsbStreamingService);
            Log.d(TAG, "broadcast sent to usbStreamingService");
             */
        }
    };


    private BroadcastReceiver songFinishedPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Broadcast of song ended received");
            if (songsArrayList.size() > 0){
                currentSongPlayingIndex ++;
                if (currentSongPlayingIndex < songsArrayList.size()){
                    Log.d(TAG,"current song playing index: " + currentSongPlayingIndex);
                }
                else if (currentSongPlayingIndex >= songsArrayList.size()){
                    currentSongPlayingIndex = 0;
                    Log.d(TAG,"current song playing index2: " + currentSongPlayingIndex);

                }

                sendBroadcastToUsbStreamService(songsArrayList.get(currentSongPlayingIndex).getSongUri().toString(),USB_STREAM_TASK_LOAD_SONG);
                /*
                //broadcast to service:
                Intent intentToBroadcastToUsbStreamingService = new Intent(ACTION_MY_USB_STREAMING_INTENT_FILTER);
                intentToBroadcastToUsbStreamingService.putExtra(EXTRA_USB_STREAMING_PERFORM_TASK_ID, USB_STREAM_TASK_LOAD_SONG);
                intentToBroadcastToUsbStreamingService.putExtra(EXTRA_USB_STREAMING_SONG_URI, songsArrayList.get(currentSongPlayingIndex).getSongUri().toString());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentToBroadcastToUsbStreamingService);
                Log.d(TAG, "broadcast sent to usbStreamingService");
                 */
            }
        }
    };

    private void sendBroadcastToUsbStreamService(String songUriString, int task){
        //broadcast to service
        Intent intentToBroadcastToUsbStreamingService = new Intent(ACTION_MY_USB_STREAMING_INTENT_FILTER);
        intentToBroadcastToUsbStreamingService.putExtra(EXTRA_USB_STREAMING_PERFORM_TASK_ID, task);
        intentToBroadcastToUsbStreamingService.putExtra(EXTRA_USB_STREAMING_SONG_URI, songUriString);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentToBroadcastToUsbStreamingService);
        Log.d(TAG, "broadcast sent to usbStreamingService");

    }



}
