package com.example.usbmusictest2;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;


public class MySongAdapter extends RecyclerView.Adapter<MySongAdapter.ViewHolder> {

    private ArrayList<Songs> songsData;
    //Context context = myActivityContext;
    private Context context2;

    MySongAdapter(ArrayList<Songs> songsData){
        this.songsData = songsData;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View songItem = layoutInflater.inflate(R.layout.song_item, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(songItem);
        //context.getApplicationContext();
        context2 = songItem.getContext();
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Songs songs = songsData.get(position);
        if (songsData.get(position).getTitle() == null || songsData.get(position).getTitle().isEmpty()){
            holder.titleTextView.setText(songsData.get(position).getFilename());
        } else {
            holder.titleTextView.setText(songsData.get(position).getTitle());
        }
        holder.artistTextView.setText(songsData.get(position).getAtrist());
        holder.albumTextView.setText(songsData.get(position).getAlbum());

        holder.songConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("song_chosen_from_storage_intent");
                intent.putExtra("uriValue", songs.getSongUri().toString());
                intent.putExtra("index_number", position);
                LocalBroadcastManager.getInstance(context2).sendBroadcast(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return songsData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView artistTextView;
        TextView titleTextView;
        TextView albumTextView;
        ConstraintLayout songConstraintLayout;

        ViewHolder(View itemView){
            super(itemView);
            this.artistTextView = itemView.findViewById(R.id.song_artist_textview);
            this.titleTextView = itemView.findViewById(R.id.song_title_textview);
            this.albumTextView = itemView.findViewById(R.id.song_album_textview);
            songConstraintLayout = itemView.findViewById(R.id.song_item_constraint_layout);
        }
    }

}
