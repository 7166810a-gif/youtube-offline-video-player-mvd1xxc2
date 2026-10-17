package com.generated.app;

import java.io.Serializable;

public class VideoItem implements Serializable {
    private long id;
    private String title;
    private String dataPath;
    private long duration;
    private long size;
    private long dateAdded;

    public VideoItem(long id, String title, String dataPath, long duration, long size, long dateAdded) {
        this.id = id;
        this.title = title;
        this.dataPath = dataPath;
        this.duration = duration;
        this.size = size;
        this.dateAdded = dateAdded;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDataPath() { return dataPath; }
    public long getDuration() { return duration; }
    public long getSize() { return size; }
    public long getDateAdded() { return dateAdded; }
}
