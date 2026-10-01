package com.sanjana.datefinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView tvSelectedFolder;

    private Button btnScanJson;
    private Button btnViewResult;
    private Button btnRename;

    private Uri selectedFolderUri;

    private static final String RESULT_FILE =
            "scan_results.jsonl";

    private static final String PREFS_NAME =
            "DateFinderPrefs";

    private static final String FOLDER_URI_KEY =
            "folder_uri";

    private final ActivityResultLauncher<Intent> folderPicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            Uri uri =
                                    result.getData().getData();

                            if (uri != null) {

                                selectedFolderUri = uri;

                                try {

                                    getContentResolver()
                                            .takePersistableUriPermission(
                                                    uri,
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            );

                                } catch (SecurityException e) {

                                    e.printStackTrace();
                                }

                                /*
                                 * Save selected folder so that
                                 * RenameActivity can use it later.
                                 */
                                SharedPreferences prefs =
                                        getSharedPreferences(
                                                PREFS_NAME,
                                                MODE_PRIVATE
                                        );

                                prefs.edit()
                                        .putString(
                                                FOLDER_URI_KEY,
                                                uri.toString()
                                        )
                                        .apply();

                                DocumentFile folder =
                                        DocumentFile.fromTreeUri(
                                                this,
                                                uri
                                        );

                                if (folder != null &&
                                        folder.getName() != null) {

                                    tvSelectedFolder.setText(
                                            folder.getName()
                                    );

                                } else {

                                    tvSelectedFolder.setText(
                                            "Folder selected"
                                    );
                                }

                                btnScanJson.setEnabled(true);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_main
        );

        tvSelectedFolder =
                findViewById(
                        R.id.tvSelectedFolder
                );

        btnScanJson =
                findViewById(
                        R.id.btnScanJson
                );

        btnViewResult =
                findViewById(
                        R.id.btnViewResult
                );

        btnRename =
                findViewById(
                        R.id.btnRename
                );

        Button btnChooseFolder =
                findViewById(
                        R.id.btnChooseFolder
                );

        /*
         * Restore previously selected folder.
         */
        loadSavedFolder();

        /*
         * Initial button state.
         */
        btnScanJson.setEnabled(
                selectedFolderUri != null
        );

        updateResultButtons();

        /*
         * Choose Folder.
         */
        btnChooseFolder.setOnClickListener(v ->
                openFolderPicker()
        );

        /*
         * Scan JSON.
         */
        btnScanJson.setOnClickListener(v -> {

            if (selectedFolderUri == null) {
                return;
            }

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            ScanActivity.class
                    );

            intent.putExtra(
                    "folder_uri",
                    selectedFolderUri.toString()
            );

            startActivity(intent);
        });

        /*
         * View Result.
         */
        btnViewResult.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            ResultActivity.class
                    );

            startActivity(intent);
        });

        /*
         * Rename.
         */
        btnRename.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            RenameActivity.class
                    );

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {

        super.onResume();

        /*
         * Refresh result buttons when coming back
         * from Scan, Result or Rename.
         */
        updateResultButtons();
    }

    private void loadSavedFolder() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        String uriString =
                prefs.getString(
                        FOLDER_URI_KEY,
                        null
                );

        if (uriString == null ||
                uriString.isEmpty()) {

            return;
        }

        try {

            selectedFolderUri =
                    Uri.parse(uriString);

            DocumentFile folder =
                    DocumentFile.fromTreeUri(
                            this,
                            selectedFolderUri
                    );

            if (folder != null &&
                    folder.exists()) {

                if (folder.getName() != null) {

                    tvSelectedFolder.setText(
                            folder.getName()
                    );

                } else {

                    tvSelectedFolder.setText(
                            "Folder selected"
                    );
                }

            } else {

                selectedFolderUri = null;

                tvSelectedFolder.setText(
                        "No folder selected"
                );
            }

        } catch (Exception e) {

            selectedFolderUri = null;

            tvSelectedFolder.setText(
                    "No folder selected"
            );
        }
    }

    private void updateResultButtons() {

        File resultFile =
                new File(
                        getFilesDir(),
                        RESULT_FILE
                );

        boolean hasResults =
                resultFile.exists()
                        && resultFile.length() > 0;

        btnViewResult.setEnabled(
                hasResults
        );

        btnRename.setEnabled(
                hasResults
        );
    }

    private void openFolderPicker() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT_TREE
                );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        folderPicker.launch(intent);
    }
}