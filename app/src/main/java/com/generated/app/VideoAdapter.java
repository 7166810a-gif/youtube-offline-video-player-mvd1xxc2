package com.generated.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class VideoAdapter extends BaseAdapter {
    private Context context;
    private List<VideoItem> videoList;
    private LayoutInflater inflater;

    public VideoAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateList(List<VideoItem> newList) {
        this.videoList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return videoList.size();
    }

    @Override
    public Object getItem(int position) {
        return videoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return videoList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_video, parent, false);
            holder = new ViewHolder();
            holder.imgThumbnail = convertView.findViewById(R.id.imgThumbnail);
            holder.tvTitle = convertView.findViewById(R.id.tvTitle);
            holder.tvSubInfo = convertView.findViewById(R.id.tvSubInfo);
            holder.tvDuration = convertView.findViewById(R.id.tvDuration);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        VideoItem video = videoList.get(position);
        holder.tvTitle.setText(video.getTitle());

        // Format duration (ms to mm:ss)
        long seconds = video.getDuration() / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        String durationStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        holder.tvDuration.setText(durationStr);

        // Format size (bytes to MB)
        double sizeMb = (double) video.getSize() / (1024 * 1024);
        String subText = String.format(Locale.getDefault(), "%.1f MB • %s", sizeMb, durationStr);
        holder.tvSubInfo.setText(subText);

        // Load Thumbnail async or via MediaStore helper
        try {
            Bitmap thumb = MediaStore.Video.Thumbnails.getThumbnail(
                    context.getContentResolver(),
                    video.getId(),
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    null
            );
            if (thumb != null) {
                holder.imgThumbnail.setImageBitmap(thumb);
            } else {
                holder.imgThumbnail.setImageResource(android.R.drawable.ic_media_play);
            }
        } catch (Exception e) {
            holder.imgThumbnail.setImageResource(android.R.drawable.ic_media_play);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView imgThumbnail;
        TextView tvTitle;
        TextView tvSubInfo;
        TextView tvDuration;
    }
}
