package com.generated.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "offline_youtube.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_FAVORITES = "favorites";
    private static final String TABLE_HISTORY = "history";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_SIZE = "size";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createFavs = "CREATE TABLE " + TABLE_FAVORITES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_PATH + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_DURATION + " INTEGER, " +
                COLUMN_SIZE + " INTEGER)";
        db.execSQL(createFavs);

        String createHist = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_PATH + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_DURATION + " INTEGER, " +
                COLUMN_SIZE + " INTEGER, " +
                COLUMN_TIMESTAMP + " INTEGER)";
        db.execSQL(createHist);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public void addFavorite(VideoItem video) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, video.getId());
        cv.put(COLUMN_PATH, video.getDataPath());
        cv.put(COLUMN_TITLE, video.getTitle());
        cv.put(COLUMN_DURATION, video.getDuration());
        cv.put(COLUMN_SIZE, video.getSize());
        db.insertWithOnConflict(TABLE_FAVORITES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public void removeFavorite(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FAVORITES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public boolean isFavorite(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, new String[]{COLUMN_ID}, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public List<VideoItem> getFavorites() {
        List<VideoItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SIZE));
                list.add(new VideoItem(id, title, path, duration, size, 0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void addHistory(VideoItem video) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, video.getId());
        cv.put(COLUMN_PATH, video.getDataPath());
        cv.put(COLUMN_TITLE, video.getTitle());
        cv.put(COLUMN_DURATION, video.getDuration());
        cv.put(COLUMN_SIZE, video.getSize());
        cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<VideoItem> getHistory() {
        List<VideoItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SIZE));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                list.add(new VideoItem(id, title, path, duration, size, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}
