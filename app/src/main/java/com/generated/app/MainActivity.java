package com.generated.app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etSearch;
    private TextView tabExplore, tabFavorites, tabHistory;
    private ListView listViewVideos;
    private LinearLayout layoutEmpty;
    private TextView tvEmpty;
    private Button btnPermission;

    private DatabaseHelper dbHelper;
    private VideoAdapter videoAdapter;
    private List<VideoItem> allVideos = new ArrayList<>();
    private List<VideoItem> displayedVideos = new ArrayList<>();
    
    private int currentTab = 0; // 0: Explore, 1: Favorites, 2: History

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupListeners();

        checkPermissionAndLoad();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        tabExplore = findViewById(R.id.tabExplore);
        tabFavorites = findViewById(R.id.tabFavorites);
        tabHistory = findViewById(R.id.tabHistory);
        listViewVideos = findViewById(R.id.listViewVideos);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnPermission = findViewById(R.id.btnPermission);

        videoAdapter = new VideoAdapter(this, displayedVideos);
        listViewVideos.setAdapter(videoAdapter);
    }

    private void setupListeners() {
        tabExplore.setOnClickListener(v -> switchTab(0));
        tabFavorites.setOnClickListener(v -> switchTab(1));
        tabHistory.setOnClickListener(v -> switchTab(2));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterVideos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listViewVideos.setOnItemClickListener((parent, view, position, id) -> {
            VideoItem video = displayedVideos.get(position);
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putExtra("video", video);
            startActivity(intent);
        });

        btnPermission.setOnClickListener(v -> requestVideoPermission());
    }

    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        
        // Reset tabs style
        tabExplore.setBackgroundResource(tabIndex == 0 ? R.drawable.bg_chip : R.drawable.bg_chip_unselected);
        tabExplore.setTextColor(tabIndex == 0 ? 0xFFFFFFFF : 0xFF212121);

        tabFavorites.setBackgroundResource(tabIndex == 1 ? R.drawable.bg_chip : R.drawable.bg_chip_unselected);
        tabFavorites.setTextColor(tabIndex == 1 ? 0xFFFFFFFF : 0xFF212121);

        tabHistory.setBackgroundResource(tabIndex == 2 ? R.drawable.bg_chip : R.drawable.bg_chip_unselected);
        tabHistory.setTextColor(tabIndex == 2 ? 0xFFFFFFFF : 0xFF212121);

        loadVideosForCurrentTab();
    }

    private void loadVideosForCurrentTab() {
        if (currentTab == 0) {
            allVideos = fetchDeviceVideos();
        } else if (currentTab == 1) {
            allVideos = dbHelper.getFavorites();
        } else {
            allVideos = dbHelper.getHistory();
        }
        filterVideos(etSearch.getText().toString());
    }

    private void filterVideos(String query) {
        displayedVideos.clear();
        String lowerQuery = query.toLowerCase().trim();
        for (VideoItem video : allVideos) {
            if (video.getTitle().toLowerCase().contains(lowerQuery)) {
                displayedVideos.add(video);
            }
        }
        videoAdapter.updateList(displayedVideos);

        if (displayedVideos.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            listViewVideos.setVisibility(View.GONE);
            if (currentTab == 0 && allVideos.isEmpty()) {
                tvEmpty.setText(R.string.empty_videos);
                btnPermission.setVisibility(View.GONE);
            } else if (currentTab == 1) {
                tvEmpty.setText("אין סרטונים במועדפים");
                btnPermission.setVisibility(View.GONE);
            } else if (currentTab == 2) {
                tvEmpty.setText("היסטוריית הצפייה ריקה");
                btnPermission.setVisibility(View.GONE);
            } else {
                tvEmpty.setText("לא נמצאו תוצאות");
                btnPermission.setVisibility(View.GONE);
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            listViewVideos.setVisibility(View.VISIBLE);
        }
    }

    private void checkPermissionAndLoad() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
            loadVideosForCurrentTab();
        } else {
            requestVideoPermission();
        }
    }

    private void requestVideoPermission() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.READ_EXTERNAL_STORAGE;
        requestPermissions(new String[]{perm}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideosForCurrentTab();
            } else {
                layoutEmpty.setVisibility(View.VISIBLE);
                listViewVideos.setVisibility(View.GONE);
                tvEmpty.setText(R.string.permission_required);
                btnPermission.setVisibility(View.VISIBLE);
            }
        }
    }

    private List<VideoItem> fetchDeviceVideos() {
        List<VideoItem> list = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
        };

        Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC");
        if (cursor != null) {
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String data = cursor.getString(dataCol);
                long duration = cursor.getLong(durationCol);
                long size = cursor.getLong(sizeCol);
                long dateAdded = cursor.getLong(dateCol);

                if (title == null) title = "סרטון ללא שם";
                list.add(new VideoItem(id, title, data, duration, size, dateAdded));
            }
            cursor.close();
        }
        return list;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab != 0) {
            loadVideosForCurrentTab();
        }
    }
}
