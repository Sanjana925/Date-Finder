package com.sanjana.datefinder;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RenameActivity extends AppCompatActivity {

    // ---------------------------------------------------------
    // MAIN SCREEN
    // ---------------------------------------------------------

    private TextView tvWorkingFolder;
    private TextView tvFileCount;
    private LinearLayout fileListContainer;
    private Button btnRenameByDate;
    private ImageButton btnSettings;

    // ---------------------------------------------------------
    // RENAME SETTINGS
    // ---------------------------------------------------------

    private String dateFormatMode = "Default";

    private String customDateFormat =
            "yyyy_MM_dd";

    private String defaultDateFormat =
            "YYYY_MM_DD";

    private String selectedDateSource =
            "photoTakenTime";

    private String nameStyle =
            "Date only";

    private String mediaLabelMode =
            "Automatic (IMG_ / VID_)";

    private String imageLabel =
            "IMG_";

    private String videoLabel =
            "VID_";

    private String otherLabel =
            "";

    private boolean serialEnabled =
            true;

    private int serialStart =
            1;

    private int serialDigits =
            1;

    private String serialPosition =
            "After date";

    private boolean showNoChangeFiles =
            true;

    // ---------------------------------------------------------
    // DUPLICATE NAME ADJUSTMENT
    // ---------------------------------------------------------

    /*
     * Number of files that needed a serial suffix because
     * their normal generated name was already used.
     *
     * Example:
     *
     * First:
     * IMG_2020_11_26.jpg
     *
     * Second:
     * IMG_2020_11_26_1.jpg
     *
     * Third:
     * IMG_2020_11_26_2.jpg
     *
     * The second and third files count as duplicate
     * adjustments.
     */
    private int duplicateAdjustedCount = 0;

    // ---------------------------------------------------------
    // DATA
    // ---------------------------------------------------------

    private final List<JSONObject> metadataList =
            new ArrayList<>();

    private final List<RenameItem> availableItems =
            new ArrayList<>();

    private final Map<String, DocumentFile> mediaFiles =
            new HashMap<>();

    private final Map<String, JSONObject> metadataByTitle =
            new HashMap<>();

    private final List<String> dateSources =
            new ArrayList<>();

    private Uri rootFolderUri;

    // ---------------------------------------------------------
    // BACKGROUND THREAD
    // ---------------------------------------------------------

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private boolean initialLoadFinished =
            false;

    private boolean needsRefresh =
            false;

    private boolean activityDestroyed =
            false;

    // ---------------------------------------------------------
    // PREFS
    // ---------------------------------------------------------

    private static final String PREFS =
            "DateFinderPrefs";

    private static final String FOLDER_URI =
            "folder_uri";

    private static final String RENAME_PREFS =
            "DateFinderRenameSettings";

    private static final String KEY_DATE_FORMAT_MODE =
            "date_format_mode";

    private static final String KEY_DEFAULT_DATE_FORMAT =
            "date_format";

    private static final String KEY_DEFAULT_DATE_FORMAT_OLD =
            "default_date_format";

    private static final String KEY_CUSTOM_DATE_FORMAT =
            "custom_date_format";

    private static final String KEY_DATE_SOURCE =
            "date_source";

    private static final String KEY_NAME_STYLE =
            "name_style";

    private static final String KEY_MEDIA_LABEL_MODE =
            "media_label_mode";

    private static final String KEY_IMAGE_LABEL =
            "image_label";

    private static final String KEY_VIDEO_LABEL =
            "video_label";

    private static final String KEY_OTHER_LABEL =
            "other_label";

    private static final String KEY_SERIAL_ENABLED =
            "serial_enabled";

    private static final String KEY_SERIAL_START =
            "serial_start";

    private static final String KEY_SERIAL_DIGITS =
            "serial_digits";

    private static final String KEY_SERIAL_POSITION =
            "serial_position";

    private static final String KEY_SHOW_NO_CHANGE =
            "show_no_change";

    private static final String RESULT_FILE =
            "scan_results.jsonl";

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_rename
        );

        initializeViews();

        loadFolder();

        btnRenameByDate.setEnabled(false);

        btnSettings.setOnClickListener(v -> {

            needsRefresh = true;

            Intent intent =
                    new Intent(
                            RenameActivity.this,
                            RenameSettingsActivity.class
                    );

            startActivity(intent);
        });

        btnRenameByDate.setOnClickListener(v ->
                showRenamePreview()
        );

        startBackgroundLoad();
    }

    // ---------------------------------------------------------
    // RESUME
    // ---------------------------------------------------------

    @Override
    protected void onResume() {

        super.onResume();

        if (initialLoadFinished &&
                needsRefresh) {

            needsRefresh = false;

            loadFolder();

            startBackgroundLoad();
        }
    }

    // ---------------------------------------------------------
    // DESTROY
    // ---------------------------------------------------------

    @Override
    protected void onDestroy() {

        activityDestroyed = true;

        executor.shutdownNow();

        super.onDestroy();
    }

    // ---------------------------------------------------------
    // INITIALIZE VIEWS
    // ---------------------------------------------------------

    private void initializeViews() {

        tvWorkingFolder =
                findViewById(
                        R.id.tvWorkingFolder
                );

        tvFileCount =
                findViewById(
                        R.id.tvFileCount
                );

        fileListContainer =
                findViewById(
                        R.id.fileListContainer
                );

        btnRenameByDate =
                findViewById(
                        R.id.btnRenameByDate
                );

        btnSettings =
                findViewById(
                        R.id.btnSettings
                );
    }

    // ---------------------------------------------------------
    // BACKGROUND LOAD
    // ---------------------------------------------------------

    private void startBackgroundLoad() {

        btnRenameByDate.setEnabled(false);

        tvFileCount.setText(
                "Loading files..."
        );

        executor.execute(() -> {

            loadMetadata();

            loadSettings();

            buildMediaFileIndex();

            buildAvailableFiles();

            if (activityDestroyed) {
                return;
            }

            runOnUiThread(() -> {

                if (activityDestroyed) {
                    return;
                }

                initialLoadFinished = true;

                updateMainFileList();

                btnRenameByDate.setEnabled(
                        hasFilesToRename()
                );
            });
        });
    }

    // ---------------------------------------------------------
    // LOAD FOLDER
    // ---------------------------------------------------------

    private void loadFolder() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                );

        String uriString =
                prefs.getString(
                        FOLDER_URI,
                        null
                );

        if (uriString == null ||
                uriString.isEmpty()) {

            rootFolderUri = null;

            tvWorkingFolder.setText(
                    "No folder selected"
            );

            return;
        }

        try {

            rootFolderUri =
                    Uri.parse(uriString);

            DocumentFile folder =
                    DocumentFile.fromTreeUri(
                            this,
                            rootFolderUri
                    );

            if (folder != null &&
                    folder.exists()) {

                String name =
                        folder.getName();

                if (name == null ||
                        name.isEmpty()) {

                    name =
                            "Selected folder";
                }

                tvWorkingFolder.setText(
                        name
                );

            } else {

                rootFolderUri = null;

                tvWorkingFolder.setText(
                        "Unable to access folder"
                );
            }

        } catch (Exception e) {

            rootFolderUri = null;

            tvWorkingFolder.setText(
                    "Unable to access folder"
            );
        }
    }

    // ---------------------------------------------------------
    // LOAD METADATA
    // ---------------------------------------------------------

    private void loadMetadata() {

        metadataList.clear();

        metadataByTitle.clear();

        dateSources.clear();

        try {

            FileInputStream input =
                    openFileInput(
                            RESULT_FILE
                    );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            String line;

            while ((line =
                    reader.readLine()) != null) {

                line =
                        line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                try {

                    JSONObject object =
                            new JSONObject(line);

                    metadataList.add(
                            object
                    );

                    String title =
                            object.optString(
                                    "title",
                                    ""
                            );

                    if (!title.isEmpty()) {

                        String key =
                                title.toLowerCase(
                                        Locale.ROOT
                                );

                        if (!metadataByTitle
                                .containsKey(key)) {

                            metadataByTitle.put(
                                    key,
                                    object
                            );
                        }
                    }

                    collectDateFields(
                            object,
                            "",
                            dateSources
                    );

                } catch (Exception ignored) {
                }
            }

            reader.close();

        } catch (Exception ignored) {
        }

        sortDateSources();
    }

    // ---------------------------------------------------------
    // COLLECT DATE SOURCES
    // ---------------------------------------------------------

    private void collectDateFields(
            JSONObject object,
            String prefix,
            List<String> output
    ) {

        java.util.Iterator<String> keys =
                object.keys();

        while (keys.hasNext()) {

            String key =
                    keys.next();

            Object value =
                    object.opt(key);

            String fullName =
                    prefix.isEmpty()
                            ? key
                            : prefix + "." + key;

            if (value instanceof JSONObject) {

                JSONObject child =
                        (JSONObject) value;

                if (child.has("timestamp") ||
                        child.has("formatted")) {

                    if (!output.contains(
                            fullName
                    )) {

                        output.add(
                                fullName
                        );
                    }

                } else {

                    collectDateFields(
                            child,
                            fullName,
                            output
                    );
                }
            }
        }
    }

    private void sortDateSources() {

        Collections.sort(
                dateSources,
                (a, b) -> {

                    int pa =
                            dateSourcePriority(a);

                    int pb =
                            dateSourcePriority(b);

                    if (pa != pb) {

                        return Integer.compare(
                                pa,
                                pb
                        );
                    }

                    return a.compareToIgnoreCase(
                            b
                    );
                }
        );
    }

    private int dateSourcePriority(
            String source
    ) {

        String lower =
                source.toLowerCase(
                        Locale.ROOT
                );

        if (lower.equals(
                "phototakentime"
        )) {

            return 0;
        }

        if (lower.equals(
                "creationtime"
        )) {

            return 1;
        }

        if (lower.contains(
                "phototakentime"
        )) {

            return 2;
        }

        if (lower.contains(
                "creationtime"
        )) {

            return 3;
        }

        return 10;
    }

    // ---------------------------------------------------------
    // LOAD SETTINGS
    // ---------------------------------------------------------

    private void loadSettings() {

        dateFormatMode =
                "Default";

        customDateFormat =
                "yyyy_MM_dd";

        defaultDateFormat =
                "YYYY_MM_DD";

        selectedDateSource =
                findPhotoTakenSource();

        nameStyle =
                "Date only";

        mediaLabelMode =
                "Automatic (IMG_ / VID_)";

        imageLabel =
                "IMG_";

        videoLabel =
                "VID_";

        otherLabel =
                "";

        serialEnabled =
                true;

        serialStart =
                1;

        serialDigits =
                1;

        serialPosition =
                "After date";

        showNoChangeFiles =
                true;

        SharedPreferences prefs =
                getSharedPreferences(
                        RENAME_PREFS,
                        MODE_PRIVATE
                );

        if (prefs.contains(
                KEY_DATE_FORMAT_MODE
        )) {

            dateFormatMode =
                    prefs.getString(
                            KEY_DATE_FORMAT_MODE,
                            dateFormatMode
                    );
        }

        if (prefs.contains(
                KEY_CUSTOM_DATE_FORMAT
        )) {

            customDateFormat =
                    prefs.getString(
                            KEY_CUSTOM_DATE_FORMAT,
                            customDateFormat
                    );
        }

        if (prefs.contains(
                KEY_DEFAULT_DATE_FORMAT
        )) {

            defaultDateFormat =
                    prefs.getString(
                            KEY_DEFAULT_DATE_FORMAT,
                            defaultDateFormat
                    );

        } else if (prefs.contains(
                KEY_DEFAULT_DATE_FORMAT_OLD
        )) {

            defaultDateFormat =
                    prefs.getString(
                            KEY_DEFAULT_DATE_FORMAT_OLD,
                            defaultDateFormat
                    );
        }

        if (prefs.contains(
                KEY_DATE_SOURCE
        )) {

            selectedDateSource =
                    prefs.getString(
                            KEY_DATE_SOURCE,
                            selectedDateSource
                    );
        }

        if (prefs.contains(
                KEY_NAME_STYLE
        )) {

            nameStyle =
                    prefs.getString(
                            KEY_NAME_STYLE,
                            nameStyle
                    );
        }

        if (prefs.contains(
                KEY_MEDIA_LABEL_MODE
        )) {

            mediaLabelMode =
                    prefs.getString(
                            KEY_MEDIA_LABEL_MODE,
                            mediaLabelMode
                    );
        }

        if (prefs.contains(
                KEY_IMAGE_LABEL
        )) {

            imageLabel =
                    prefs.getString(
                            KEY_IMAGE_LABEL,
                            imageLabel
                    );
        }

        if (prefs.contains(
                KEY_VIDEO_LABEL
        )) {

            videoLabel =
                    prefs.getString(
                            KEY_VIDEO_LABEL,
                            videoLabel
                    );
        }

        if (prefs.contains(
                KEY_OTHER_LABEL
        )) {

            otherLabel =
                    prefs.getString(
                            KEY_OTHER_LABEL,
                            otherLabel
                    );
        }

        if (prefs.contains(
                KEY_SERIAL_ENABLED
        )) {

            serialEnabled =
                    prefs.getBoolean(
                            KEY_SERIAL_ENABLED,
                            serialEnabled
                    );
        }

        if (prefs.contains(
                KEY_SERIAL_START
        )) {

            serialStart =
                    prefs.getInt(
                            KEY_SERIAL_START,
                            serialStart
                    );
        }

        if (prefs.contains(
                KEY_SERIAL_DIGITS
        )) {

            serialDigits =
                    prefs.getInt(
                            KEY_SERIAL_DIGITS,
                            serialDigits
                    );
        }

        if (prefs.contains(
                KEY_SERIAL_POSITION
        )) {

            serialPosition =
                    prefs.getString(
                            KEY_SERIAL_POSITION,
                            serialPosition
                    );
        }

        if (prefs.contains(
                KEY_SHOW_NO_CHANGE
        )) {

            showNoChangeFiles =
                    prefs.getBoolean(
                            KEY_SHOW_NO_CHANGE,
                            showNoChangeFiles
                    );
        }

        if (!dateSourceExists(
                selectedDateSource
        )) {

            selectedDateSource =
                    findPhotoTakenSource();
        }
    }

    private boolean dateSourceExists(
            String source
    ) {

        if (source == null ||
                source.isEmpty()) {

            return false;
        }

        for (String available :
                dateSources) {

            if (available.equals(
                    source
            )) {

                return true;
            }
        }

        String normalized =
                normalizeDateSource(
                        source
                );

        for (String available :
                dateSources) {

            if (normalizeDateSource(
                    available
            ).equalsIgnoreCase(
                    normalized
            )) {

                return true;
            }
        }

        return false;
    }

    private String normalizeDateSource(
            String source
    ) {

        if (source == null) {
            return "";
        }

        return source
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        " ",
                        ""
                )
                .replace(
                        ">",
                        ""
                )
                .replace(
                        ".",
                        ""
                );
    }

    private String findPhotoTakenSource() {

        for (String source :
                dateSources) {

            if (source.equalsIgnoreCase(
                    "photoTakenTime"
            )) {

                return source;
            }
        }

        for (String source :
                dateSources) {

            String lower =
                    source.toLowerCase(
                            Locale.ROOT
                    );

            if (lower.contains(
                    "phototakentime"
            )) {

                return source;
            }
        }

        if (!dateSources.isEmpty()) {

            return dateSources.get(0);
        }

        return "photoTakenTime";
    }

    // ---------------------------------------------------------
    // MEDIA INDEX
    // ---------------------------------------------------------

    private void buildMediaFileIndex() {

        mediaFiles.clear();

        if (rootFolderUri == null) {
            return;
        }

        DocumentFile root =
                DocumentFile.fromTreeUri(
                        this,
                        rootFolderUri
                );

        if (root == null ||
                !root.exists()) {

            return;
        }

        indexFolder(root);
    }

    private void indexFolder(
            DocumentFile folder
    ) {

        DocumentFile[] files;

        try {

            files =
                    folder.listFiles();

        } catch (Exception e) {

            return;
        }

        if (files == null) {
            return;
        }

        for (DocumentFile file :
                files) {

            if (file == null) {
                continue;
            }

            if (file.isDirectory()) {

                indexFolder(file);

                continue;
            }

            String name =
                    file.getName();

            if (name == null) {
                continue;
            }

            if (!isImage(name) &&
                    !isVideo(name)) {

                continue;
            }

            String key =
                    name.toLowerCase(
                            Locale.ROOT
                    );

            if (!mediaFiles.containsKey(
                    key
            )) {

                mediaFiles.put(
                        key,
                        file
                );
            }
        }
    }

    // ---------------------------------------------------------
    // BUILD AVAILABLE FILES
    // ---------------------------------------------------------

    private void buildAvailableFiles() {

        availableItems.clear();

        duplicateAdjustedCount = 0;

        /*
         * Contains names that have already been generated
         * during this rename operation.
         */
        Set<String> usedNames =
                new HashSet<>();

        for (JSONObject metadata :
                metadataList) {

            String title =
                    metadata.optString(
                            "title",
                            ""
                    );

            if (title.isEmpty()) {
                continue;
            }

            String key =
                    title.toLowerCase(
                            Locale.ROOT
                    );

            DocumentFile file =
                    mediaFiles.get(key);

            /*
             * Keep exact filename matching.
             */
            if (file == null) {
                continue;
            }

            String fileName =
                    file.getName();

            if (fileName == null) {
                continue;
            }

            String newName =
                    generateNewName(
                            metadata,
                            fileName,
                            usedNames
                    );

            String photoTakenTime =
                    getFormattedDate(
                            metadata,
                            "photoTakenTime"
                    );

            if (!showNoChangeFiles &&
                    (newName == null ||
                            newName.equals(
                                    fileName
                            ))) {

                continue;
            }

            RenameItem item =
                    new RenameItem(
                            file,
                            fileName,
                            newName == null
                                    ? ""
                                    : newName,
                            photoTakenTime
                    );

            availableItems.add(
                    item
            );
        }
    }

    // ---------------------------------------------------------
    // GET FORMATTED DATE
    // ---------------------------------------------------------

    private String getFormattedDate(
            JSONObject metadata,
            String field
    ) {

        JSONObject dateObject =
                metadata.optJSONObject(
                        field
                );

        if (dateObject == null) {
            return "—";
        }

        String formatted =
                dateObject.optString(
                        "formatted",
                        ""
                );

        if (!formatted.isEmpty()) {
            return formatted;
        }

        String timestamp =
                dateObject.optString(
                        "timestamp",
                        ""
                );

        if (!timestamp.isEmpty()) {
            return timestamp;
        }

        return "—";
    }

    // ---------------------------------------------------------
    // NAME GENERATION
    // ---------------------------------------------------------

    private String generateNewName(
            JSONObject metadata,
            String originalName,
            Set<String> usedNames
    ) {

        String rawDate =
                getSelectedDate(
                        metadata,
                        selectedDateSource
                );

        if (rawDate == null ||
                rawDate.isEmpty()) {

            return null;
        }

        String date =
                formatDate(
                        rawDate
                );

        if (date == null ||
                date.isEmpty()) {

            return null;
        }

        String extension =
                getExtension(
                        originalName
                );

        String prefix =
                getMediaPrefix(
                        originalName
                );

        String originalBase =
                removeExtension(
                        originalName
                );

        /*
         * -----------------------------------------------------
         * FIRST TRY:
         *
         * Always generate the normal name WITHOUT a serial.
         *
         * Example:
         *
         * IMG_2020_11_26.jpg
         * -----------------------------------------------------
         */
        String base =
                buildBaseName(
                        prefix,
                        date,
                        originalBase,
                        ""
                );

        String result =
                base +
                        extension;

        String lower =
                result.toLowerCase(
                        Locale.ROOT
                );

        /*
         * -----------------------------------------------------
         * DUPLICATE:
         *
         * Only now do we add a serial.
         * -----------------------------------------------------
         */
        if (usedNames.contains(
                lower
        )) {

            if (serialEnabled) {

                duplicateAdjustedCount++;

                int nextSerial =
                        Math.max(
                                0,
                                serialStart
                        );

                do {

                    String serialText =
                            formatSerial(
                                    nextSerial
                            );

                    base =
                            buildBaseName(
                                    prefix,
                                    date,
                                    originalBase,
                                    serialText
                            );

                    result =
                            base +
                                    extension;

                    lower =
                            result.toLowerCase(
                                    Locale.ROOT
                            );

                    nextSerial++;

                } while (usedNames.contains(
                        lower
                ));

            } else {

                /*
                 * Serial is disabled.
                 *
                 * In this case the duplicate name cannot
                 * be adjusted automatically.
                 *
                 * Keep the generated name as-is.
                 */
            }
        }

        usedNames.add(
                lower
        );

        return result;
    }

    // ---------------------------------------------------------
    // BUILD BASE NAME
    // ---------------------------------------------------------

    private String buildBaseName(
            String prefix,
            String date,
            String originalBase,
            String serialText
    ) {

        boolean hasSerial =
                serialText != null &&
                        !serialText.isEmpty();

        /*
         * -----------------------------------------------------
         * DATE ONLY
         * -----------------------------------------------------
         */
        if (nameStyle.equals(
                "Date only"
        )) {

            if (!hasSerial) {

                return prefix +
                        date;
            }

            if (serialPosition.equals(
                    "After date"
            )) {

                return prefix +
                        date +
                        "_" +
                        serialText;

            } else {

                return prefix +
                        serialText +
                        "_" +
                        date;
            }
        }

        /*
         * -----------------------------------------------------
         * DATE + ORIGINAL
         * -----------------------------------------------------
         *
         * Normal:
         * IMG_2020_11_26_original
         *
         * Duplicate:
         * IMG_1_2020_11_26_original
         * -----------------------------------------------------
         */
        if (nameStyle.equals(
                "Date + original"
        )) {

            if (!hasSerial) {

                return prefix +
                        date +
                        "_" +
                        originalBase;
            }

            if (serialPosition.equals(
                    "After date"
            )) {

                return prefix +
                        date +
                        "_" +
                        serialText +
                        "_" +
                        originalBase;

            } else {

                return prefix +
                        serialText +
                        "_" +
                        date +
                        "_" +
                        originalBase;
            }
        }

        /*
         * -----------------------------------------------------
         * ORIGINAL + DATE
         * -----------------------------------------------------
         *
         * Normal:
         * IMG_original_2020_11_26
         *
         * Duplicate:
         * IMG_original_2020_11_26_1
         * -----------------------------------------------------
         */
        if (!hasSerial) {

            return prefix +
                    originalBase +
                    "_" +
                    date;
        }

        if (serialPosition.equals(
                "After date"
        )) {

            return prefix +
                    originalBase +
                    "_" +
                    date +
                    "_" +
                    serialText;

        } else {

            return prefix +
                    serialText +
                    "_" +
                    originalBase +
                    "_" +
                    date;
        }
    }

    // ---------------------------------------------------------
    // FORMAT SERIAL
    // ---------------------------------------------------------

    private String formatSerial(
            int number
    ) {

        int digits =
                Math.max(
                        1,
                        serialDigits
                );

        return String.format(
                Locale.US,
                "%0" +
                        digits +
                        "d",
                number
        );
    }

    // ---------------------------------------------------------
    // MEDIA PREFIX
    // ---------------------------------------------------------

    private String getMediaPrefix(
            String fileName
    ) {

        if (mediaLabelMode.equals(
                "None"
        )) {

            return "";
        }

        if (isImage(fileName)) {

            /*
             * Automatic and Custom both use
             * the configured image label here.
             */
            return imageLabel;
        }

        if (isVideo(fileName)) {

            return videoLabel;
        }

        return otherLabel;
    }

    // ---------------------------------------------------------
    // GET SELECTED DATE
    // ---------------------------------------------------------

    private String getSelectedDate(
            JSONObject metadata,
            String selectedField
    ) {

        if (selectedField == null ||
                selectedField.isEmpty()) {

            return null;
        }

        String actualField =
                resolveDateSource(
                        metadata,
                        selectedField
                );

        if (actualField == null ||
                actualField.isEmpty()) {

            return null;
        }

        String[] parts =
                actualField.split(
                        "\\."
                );

        JSONObject current =
                metadata;

        for (int i = 0;
             i < parts.length;
             i++) {

            Object value =
                    current.opt(
                            parts[i]
                    );

            if (!(value instanceof JSONObject)) {

                return null;
            }

            JSONObject child =
                    (JSONObject) value;

            if (i ==
                    parts.length - 1) {

                String timestamp =
                        child.optString(
                                "timestamp",
                                ""
                        );

                if (!timestamp.isEmpty()) {

                    return timestamp;
                }

                String formatted =
                        child.optString(
                                "formatted",
                                ""
                        );

                if (!formatted.isEmpty()) {

                    return formatted;
                }

                return null;
            }

            current =
                    child;
        }

        return null;
    }

    private String resolveDateSource(
            JSONObject metadata,
            String selected
    ) {

        if (hasJsonPath(
                metadata,
                selected
        )) {

            return selected;
        }

        String normalized =
                normalizeDateSource(
                        selected
                );

        for (String source :
                dateSources) {

            if (normalizeDateSource(
                    source
            ).equalsIgnoreCase(
                    normalized
            )) {

                if (hasJsonPath(
                        metadata,
                        source
                )) {

                    return source;
                }
            }
        }

        if (normalized.contains(
                "phototakentime"
        )) {

            if (hasJsonPath(
                    metadata,
                    "photoTakenTime"
            )) {

                return "photoTakenTime";
            }
        }

        if (normalized.contains(
                "creationtime"
        )) {

            if (hasJsonPath(
                    metadata,
                    "creationTime"
            )) {

                return "creationTime";
            }
        }

        return selected;
    }

    private boolean hasJsonPath(
            JSONObject object,
            String path
    ) {

        if (path == null ||
                path.isEmpty()) {

            return false;
        }

        String[] parts =
                path.split(
                        "\\."
                );

        JSONObject current =
                object;

        for (int i = 0;
             i < parts.length;
             i++) {

            Object value =
                    current.opt(
                            parts[i]
                    );

            if (!(value instanceof JSONObject)) {

                return false;
            }

            JSONObject child =
                    (JSONObject) value;

            if (i ==
                    parts.length - 1) {

                return child.has(
                        "timestamp"
                ) ||
                        child.has(
                                "formatted"
                        );
            }

            current =
                    child;
        }

        return false;
    }

    // ---------------------------------------------------------
    // FORMAT DATE
    // ---------------------------------------------------------

    private String formatDate(
            String value
    ) {

        String pattern;

        if (dateFormatMode.equals(
                "Custom"
        )) {

            pattern =
                    customDateFormat;

        } else {

            pattern =
                    convertDateFormat(
                            defaultDateFormat
                    );
        }

        if (pattern == null ||
                pattern.isEmpty()) {

            pattern =
                    "yyyy_MM_dd";
        }

        try {

            long timestamp =
                    Long.parseLong(
                            value
                    );

            if (timestamp <
                    100000000000L) {

                timestamp *= 1000;
            }

            Date date =
                    new Date(
                            timestamp
                    );

            SimpleDateFormat sdf =
                    new SimpleDateFormat(
                            pattern,
                            Locale.US
                    );

            sdf.setTimeZone(
                    TimeZone.getTimeZone(
                            "UTC"
                    )
            );

            return sdf.format(
                    date
            );

        } catch (Exception ignored) {

            try {

                SimpleDateFormat input =
                        new SimpleDateFormat(
                                "dd MMM yyyy, HH:mm:ss z",
                                Locale.US
                        );

                Date date =
                        input.parse(
                                value
                        );

                if (date == null) {

                    return value;
                }

                SimpleDateFormat output =
                        new SimpleDateFormat(
                                pattern,
                                Locale.US
                        );

                output.setTimeZone(
                        TimeZone.getTimeZone(
                                "UTC"
                        )
                );

                return output.format(
                        date
                );

            } catch (Exception e) {

                return value;
            }
        }
    }

    private String convertDateFormat(
            String format
    ) {

        if (format == null) {

            return "yyyy_MM_dd";
        }

        switch (format) {

            case "YYYY-MM-DD":
                return "yyyy-MM-dd";

            case "YYYY_MM_DD":
                return "yyyy_MM_dd";

            case "DD-MM-YYYY":
                return "dd-MM-yyyy";

            case "DD_MM_YYYY":
                return "dd_MM_yyyy";

            case "YYYYMMDD":
                return "yyyyMMdd";

            default:
                return "yyyy_MM_dd";
        }
    }

    // ---------------------------------------------------------
    // RENAME PREVIEW
    // ---------------------------------------------------------

    private void showRenamePreview() {

        List<RenameItem> itemsToRename =
                getItemsToRename();

        if (itemsToRename.isEmpty()) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Rename by Date"
                    )
                    .setMessage(
                            "No files can be renamed " +
                                    "with the current settings."
                    )
                    .setPositiveButton(
                            "OK",
                            null
                    )
                    .show();

            return;
        }

        /*
         * -----------------------------------------------------
         * INFORMATION ONLY
         *
         * No Current Name / Photo Taken Time / New Name
         * table here.
         * -----------------------------------------------------
         */

        String duplicateInfo;

        if (duplicateAdjustedCount == 0) {

            duplicateInfo =
                    "Duplicate suffix adjustments: None";

        } else if (
                duplicateAdjustedCount == 1
        ) {

            duplicateInfo =
                    "Duplicate suffix adjustments: " +
                            "1 file";

        } else {

            duplicateInfo =
                    "Duplicate suffix adjustments: " +
                            duplicateAdjustedCount +
                            " files";
        }

        String serialInfo;

        if (serialEnabled) {

            serialInfo =
                    "Serial number: Enabled" +
                            "\n" +
                            "Serial start: " +
                            serialStart +
                            "\n" +
                            "Serial digits: " +
                            serialDigits +
                            "\n" +
                            "Serial position: " +
                            serialPosition;

        } else {

            serialInfo =
                    "Serial number: Disabled";
        }

        String labelInfo;

        if (mediaLabelMode.equals(
                "None"
        )) {

            labelInfo =
                    "Media label: None";

        } else if (
                mediaLabelMode.equals(
                        "Custom"
                )
        ) {

            labelInfo =
                    "Media label: Custom";

        } else {

            labelInfo =
                    "Media label: Automatic (IMG_ / VID_)";
        }

        String message =
                "Files to rename: " +
                        itemsToRename.size() +

                        "\n\n" +

                        "Date format: " +
                        getDisplayedDateFormat() +

                        "\n" +

                        "Date source: " +
                        formatSourceName(
                                selectedDateSource
                        ) +

                        "\n" +

                        "Name style: " +
                        nameStyle +

                        "\n" +

                        labelInfo +

                        "\n\n" +

                        serialInfo +

                        "\n\n" +

                        duplicateInfo;

        new AlertDialog.Builder(this)
                .setTitle(
                        "Rename Preview"
                )
                .setMessage(
                        message
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Rename",
                        (dialog, which) ->
                                executeRename(
                                        itemsToRename
                                )
                )
                .show();
    }

    private String getDisplayedDateFormat() {

        if (dateFormatMode.equals(
                "Custom"
        )) {

            return customDateFormat;
        }

        return defaultDateFormat;
    }

    private String formatSourceName(
            String source
    ) {

        if (source == null ||
                source.isEmpty()) {

            return "Photo Taken Time";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < source.length();
             i++) {

            char c =
                    source.charAt(i);

            if (i == 0) {

                result.append(
                        Character.toUpperCase(c)
                );

            } else if (c == '.') {

                result.append(
                        " > "
                );

            } else if (
                    Character.isUpperCase(c)
            ) {

                result.append(
                        " "
                );

                result.append(
                        c
                );

            } else {

                result.append(
                        c
                );
            }
        }

        return result.toString();
    }

    // ---------------------------------------------------------
    // ITEMS TO RENAME
    // ---------------------------------------------------------

    private List<RenameItem> getItemsToRename() {

        List<RenameItem> result =
                new ArrayList<>();

        for (RenameItem item :
                availableItems) {

            if (item.newName == null ||
                    item.newName.isEmpty()) {

                continue;
            }

            if (item.newName.equals(
                    item.oldName
            )) {

                continue;
            }

            result.add(
                    item
            );
        }

        return result;
    }

    private boolean hasFilesToRename() {

        for (RenameItem item :
                availableItems) {

            if (item.newName != null &&
                    !item.newName.isEmpty() &&
                    !item.newName.equals(
                            item.oldName
                    )) {

                return true;
            }
        }

        return false;
    }

    // ---------------------------------------------------------
    // ACTUAL RENAME
    // ---------------------------------------------------------

    private void executeRename(
            List<RenameItem> items
    ) {

        btnRenameByDate.setEnabled(false);

        Toast.makeText(
                this,
                "Renaming files...",
                Toast.LENGTH_SHORT
        ).show();

        executor.execute(() -> {

            int success =
                    0;

            int failed =
                    0;

            for (RenameItem item :
                    items) {

                try {

                    if (item.file.renameTo(
                            item.newName
                    )) {

                        success++;

                    } else {

                        failed++;
                    }

                } catch (Exception e) {

                    failed++;
                }
            }

            buildMediaFileIndex();

            buildAvailableFiles();

            int finalSuccess =
                    success;

            int finalFailed =
                    failed;

            if (activityDestroyed) {
                return;
            }

            runOnUiThread(() -> {

                if (activityDestroyed) {
                    return;
                }

                Toast.makeText(
                        this,
                        "Renamed: " +
                                finalSuccess +
                                "\nFailed: " +
                                finalFailed,
                        Toast.LENGTH_LONG
                ).show();

                updateMainFileList();

                btnRenameByDate.setEnabled(
                        hasFilesToRename()
                );
            });
        });
    }

    // ---------------------------------------------------------
    // MAIN FILE LIST
    // ---------------------------------------------------------

    private void updateMainFileList() {

        if (fileListContainer == null) {
            return;
        }

        fileListContainer.removeAllViews();

        int changeCount =
                0;

        for (RenameItem item :
                availableItems) {

            if (item.newName != null &&
                    !item.newName.isEmpty() &&
                    !item.newName.equals(
                            item.oldName
                    )) {

                changeCount++;
            }
        }

        tvFileCount.setText(
                changeCount +
                        " files to rename"
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        TextView currentHeader =
                createTableCell(
                        "Current name",
                        true
                );

        TextView photoTakenHeader =
                createTableCell(
                        "Photo Taken Time",
                        true
                );

        TextView newHeader =
                createTableCell(
                        "New name",
                        true
                );

        header.addView(
                currentHeader
        );

        header.addView(
                photoTakenHeader
        );

        header.addView(
                newHeader
        );

        fileListContainer.addView(
                header
        );

        for (RenameItem item :
                availableItems) {

            boolean changed =
                    item.newName != null &&
                            !item.newName.isEmpty() &&
                            !item.newName.equals(
                                    item.oldName
                            );

            if (!showNoChangeFiles &&
                    !changed) {

                continue;
            }

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            String newName =
                    item.newName == null ||
                            item.newName.isEmpty()
                            ? "—"
                            : item.newName;

            row.addView(
                    createTableCell(
                            item.oldName,
                            false
                    )
            );

            row.addView(
                    createTableCell(
                            item.photoTakenTime,
                            false
                    )
            );

            row.addView(
                    createTableCell(
                            newName,
                            false
                    )
            );

            fileListContainer.addView(
                    row
            );
        }

        btnRenameByDate.setEnabled(
                changeCount > 0
        );
    }

    private TextView createTableCell(
            String text,
            boolean header
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(
                text == null ||
                        text.isEmpty()
                        ? "—"
                        : text
        );

        tv.setTextSize(
                header
                        ? 14
                        : 13
        );

        if (header) {

            tv.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }

        tv.setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10)
        );

        tv.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dp(240),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        tv.setLayoutParams(
                params
        );

        return tv;
    }

    // ---------------------------------------------------------
    // FILE TYPE
    // ---------------------------------------------------------

    private boolean isImage(
            String name
    ) {

        String lower =
                name.toLowerCase(
                        Locale.ROOT
                );

        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif")
                || lower.endsWith(".heic")
                || lower.endsWith(".heif")
                || lower.endsWith(".bmp");
    }

    private boolean isVideo(
            String name
    ) {

        String lower =
                name.toLowerCase(
                        Locale.ROOT
                );

        return lower.endsWith(".mp4")
                || lower.endsWith(".mov")
                || lower.endsWith(".mkv")
                || lower.endsWith(".avi")
                || lower.endsWith(".webm")
                || lower.endsWith(".3gp")
                || lower.endsWith(".m4v");
    }

    // ---------------------------------------------------------
    // FILE NAME HELPERS
    // ---------------------------------------------------------

    private String getExtension(
            String name
    ) {

        int index =
                name.lastIndexOf(
                        "."
                );

        if (index < 0) {

            return "";
        }

        return name.substring(
                index
        );
    }

    private String removeExtension(
            String name
    ) {

        int index =
                name.lastIndexOf(
                        "."
                );

        if (index < 0) {

            return name;
        }

        return name.substring(
                0,
                index
        );
    }

    // ---------------------------------------------------------
    // DP
    // ---------------------------------------------------------

    private int dp(
            int value
    ) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ---------------------------------------------------------
    // RENAME ITEM
    // ---------------------------------------------------------

    private static class RenameItem {

        DocumentFile file;

        String oldName;

        String newName;

        String photoTakenTime;

        RenameItem(
                DocumentFile file,
                String oldName,
                String newName,
                String photoTakenTime
        ) {

            this.file =
                    file;

            this.oldName =
                    oldName;

            this.newName =
                    newName;

            this.photoTakenTime =
                    photoTakenTime;
        }
    }
}