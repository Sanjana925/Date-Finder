package com.sanjana.datefinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnStartScan;
    private Button btnViewResults;

    private Uri folderUri;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final String RESULT_FILE =
            "scan_results.jsonl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);

        tvStatus = findViewById(R.id.tvStatus);
        btnStartScan = findViewById(R.id.btnStartScan);
        btnViewResults = findViewById(R.id.btnViewResults);

        btnViewResults.setEnabled(false);

        btnViewResults.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ScanActivity.this,
                            ResultActivity.class
                    );

            startActivity(intent);
        });

        String uriString =
                getIntent().getStringExtra("folder_uri");

        if (uriString != null) {

            folderUri = Uri.parse(uriString);

            tvStatus.setText(
                    "Folder ready.\n\n" +
                            "Press Start Scan to scan the JSON files."
            );

        } else {

            tvStatus.setText(
                    "No folder selected."
            );

            btnStartScan.setEnabled(false);
        }

        btnStartScan.setOnClickListener(v ->
                scanFolder()
        );
    }

    private void scanFolder() {

        if (folderUri == null) {

            tvStatus.setText(
                    "No folder selected."
            );

            return;
        }

        btnStartScan.setEnabled(false);
        btnViewResults.setEnabled(false);

        tvStatus.setText(
                "Scanning...\n\n" +
                        "Reading Google Photos metadata."
        );

        executor.execute(() -> {

            DocumentFile folder =
                    DocumentFile.fromTreeUri(
                            ScanActivity.this,
                            folderUri
                    );

            if (folder == null || !folder.exists()) {

                runOnUiThread(() -> {

                    tvStatus.setText(
                            "Unable to access the selected folder."
                    );

                    btnStartScan.setEnabled(true);
                });

                return;
            }

            FileScanResult result =
                    scanAllJsonFiles(folder);

            runOnUiThread(() -> {

                if (result.error) {

                    tvStatus.setText(
                            "Scan failed.\n\n" +
                                    result.errorMessage
                    );

                    btnStartScan.setEnabled(true);

                    return;
                }

                tvStatus.setText(
                        "Scan complete.\n\n" +
                                "JSON files found: " +
                                result.jsonFiles +
                                "\n\n" +
                                "Metadata records: " +
                                result.records
                );

                btnStartScan.setEnabled(true);

                if (result.records > 0) {
                    btnViewResults.setEnabled(true);
                }
            });
        });
    }

    private FileScanResult scanAllJsonFiles(
            DocumentFile folder
    ) {

        FileScanResult result =
                new FileScanResult();

        try {

            /*
             * Remove old scan results.
             */
            deleteFile(RESULT_FILE);

            /*
             * Create a new result file.
             */
            OutputStream output =
                    openFileOutput(
                            RESULT_FILE,
                            MODE_PRIVATE
                    );

            BufferedWriter writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    output,
                                    StandardCharsets.UTF_8
                            )
                    );

            scanFolderRecursive(
                    folder,
                    writer,
                    result
            );

            writer.flush();
            writer.close();

        } catch (Exception e) {

            result.error = true;
            result.errorMessage =
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unknown error.";
        }

        return result;
    }

    private void scanFolderRecursive(
            DocumentFile folder,
            BufferedWriter writer,
            FileScanResult result
    ) throws Exception {

        DocumentFile[] files =
                folder.listFiles();

        for (DocumentFile file : files) {

            if (file.isDirectory()) {

                scanFolderRecursive(
                        file,
                        writer,
                        result
                );

                continue;
            }

            String name =
                    file.getName();

            if (name == null) {
                continue;
            }

            if (!name.toLowerCase()
                    .endsWith(".json")) {

                continue;
            }

            result.jsonFiles++;

            readJsonFile(
                    file,
                    writer,
                    result
            );
        }
    }

    private void readJsonFile(
            DocumentFile file,
            BufferedWriter writer,
            FileScanResult result
    ) {

        try {

            InputStream input =
                    getContentResolver()
                            .openInputStream(
                                    file.getUri()
                            );

            if (input == null) {
                return;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder jsonText =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {

                jsonText.append(line);
            }

            reader.close();

            String content =
                    jsonText.toString().trim();

            if (content.isEmpty()) {
                return;
            }

            /*
             * Normal Google Photos Takeout metadata:
             * one JSONObject per JSON file.
             */
            if (content.startsWith("{")) {

                JSONObject object =
                        new JSONObject(content);

                writer.write(
                        object.toString()
                );

                writer.newLine();

                result.records++;

            }

            /*
             * Also support a JSON array if encountered.
             */
            else if (content.startsWith("[")) {

                JSONArray array =
                        new JSONArray(content);

                for (int i = 0;
                     i < array.length();
                     i++) {

                    Object item =
                            array.opt(i);

                    if (item instanceof JSONObject) {

                        writer.write(
                                item.toString()
                        );

                        writer.newLine();

                        result.records++;
                    }
                }
            }

        } catch (JSONException e) {

            /*
             * Ignore invalid/non-metadata JSON files.
             * The scan continues with the remaining files.
             */

        } catch (Exception e) {

            /*
             * Ignore individual unreadable files.
             * The scan continues.
             */
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executor.shutdownNow();
    }

    private static class FileScanResult {

        int jsonFiles = 0;
        int records = 0;

        boolean error = false;

        String errorMessage = "";
    }
}