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

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";
    Button mChooseUsbStreamingButton;
    ArrayList<Songs> songsArrayList;
    protected static Context myActivityContext;

    private RecyclerView mSongRecyclerView;
    private RecyclerView.Adapter mSongAdapter;
    private RecyclerView.LayoutManager msongLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myActivityContext = getApplicationContext();
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

        mSongRecyclerView = findViewById(R.id.recycler_view_songs);
        mSongRecyclerView.setHasFixedSize(true);
        msongLayoutManager = new LinearLayoutManager(this);
        mSongRecyclerView.setLayoutManager(msongLayoutManager);
        mSongAdapter = new MySongAdapter(songsArrayList);
        mSongRecyclerView.setAdapter(mSongAdapter);
        mSongRecyclerView.addItemDecoration(new DividerItemDecoration(mSongRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        LocalBroadcastManager.getInstance(this).registerReceiver(songClickBroadcastReceiver, new IntentFilter("song_chosen_from_storage_intent"));

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(songClickBroadcastReceiver);
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
                        }
                    }
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

    private BroadcastReceiver songClickBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String uriValueExtra = intent.getStringExtra("uriValue");
            Log.d(TAG, "uri rxd by broadcast: " + uriValueExtra);
            playMedia(Uri.parse(uriValueExtra));
        }
    };

}
