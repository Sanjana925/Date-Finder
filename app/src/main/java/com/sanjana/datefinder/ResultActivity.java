package com.sanjana.datefinder;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    private LinearLayout tableContainer;

    private TextView tvResultCount;

    private final List<JSONObject> results =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result);

        tableContainer =
                findViewById(
                        R.id.tableContainer
                );

        tvResultCount =
                findViewById(
                        R.id.tvResultCount
                );

        loadResults();
    }

    private void loadResults() {

        results.clear();

        tableContainer.removeAllViews();

        try {

            FileInputStream input =
                    openFileInput(
                            "scan_results.jsonl"
                    );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                try {

                    JSONObject object =
                            new JSONObject(line);

                    results.add(object);

                } catch (Exception ignored) {
                }
            }

            reader.close();

        } catch (Exception e) {

            tvResultCount.setText(
                    "No scan results available."
            );

            return;
        }

        tvResultCount.setText(
                results.size() +
                        " metadata records found"
        );

        for (JSONObject metadata : results) {

            addResultRow(metadata);
        }
    }

    private void addResultRow(
            JSONObject metadata
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                0,
                1,
                0,
                1
        );

        String fileName =
                metadata.optString(
                        "title",
                        "Unknown"
                );

        TextView tvFileName =
                createCell(
                        fileName,
                        220
                );

        String photoTakenDate =
                getFormattedDate(
                        metadata,
                        "photoTakenTime"
                );

        TextView tvPhotoDate =
                createCell(
                        photoTakenDate,
                        220
                );

        String dateSource =
                getDateSource(metadata);

        TextView tvDateSource =
                createCell(
                        dateSource,
                        170
                );

        String creationDate =
                getFormattedDate(
                        metadata,
                        "creationTime"
                );

        TextView tvCreationDate =
                createCell(
                        creationDate,
                        220
                );

        String otherDates =
                getOtherDates(metadata);

        TextView tvOtherDates =
                createCell(
                        otherDates,
                        240
                );

        TextView btnDetail =
                createCell(
                        "View Detail",
                        160
                );

        btnDetail.setGravity(
                Gravity.CENTER
        );

        btnDetail.setOnClickListener(v ->
                showDetailsPopup(metadata)
        );

        row.addView(tvFileName);
        row.addView(tvPhotoDate);
        row.addView(tvDateSource);
        row.addView(tvCreationDate);
        row.addView(tvOtherDates);
        row.addView(btnDetail);

        tableContainer.addView(row);
    }

    private String getFormattedDate(
            JSONObject metadata,
            String field
    ) {

        JSONObject dateObject =
                metadata.optJSONObject(field);

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

    private String getDateSource(
            JSONObject metadata
    ) {

        JSONObject photoTaken =
                metadata.optJSONObject(
                        "photoTakenTime"
                );

        if (photoTaken != null) {

            String timestamp =
                    photoTaken.optString(
                            "timestamp",
                            ""
                    );

            if (!timestamp.isEmpty()) {
                return "Photo Taken Time";
            }
        }

        JSONObject creation =
                metadata.optJSONObject(
                        "creationTime"
                );

        if (creation != null) {

            String timestamp =
                    creation.optString(
                            "timestamp",
                            ""
                    );

            if (!timestamp.isEmpty()) {
                return "Creation Time";
            }
        }

        return "No date";
    }

    private String getOtherDates(
            JSONObject metadata
    ) {

        StringBuilder result =
                new StringBuilder();

        Iterator<String> keys =
                metadata.keys();

        while (keys.hasNext()) {

            String key =
                    keys.next();

            if (key.equals("photoTakenTime") ||
                    key.equals("creationTime")) {

                continue;
            }

            Object value =
                    metadata.opt(key);

            if (value instanceof JSONObject) {

                JSONObject object =
                        (JSONObject) value;

                if (isDateObject(object) ||
                        isDateField(key)) {

                    String date =
                            getDateFromObject(
                                    object
                            );

                    if (!date.isEmpty()) {

                        appendOtherDate(
                                result,
                                formatKey(key),
                                date
                        );
                    }
                }
            }
        }

        if (result.length() == 0) {
            return "—";
        }

        return result.toString();
    }

    private boolean isDateObject(
            JSONObject object
    ) {

        return object.has("timestamp") ||
                object.has("formatted");
    }

    private String getDateFromObject(
            JSONObject object
    ) {

        String formatted =
                object.optString(
                        "formatted",
                        ""
                );

        if (!formatted.isEmpty()) {
            return formatted;
        }

        return object.optString(
                "timestamp",
                ""
        );
    }

    private boolean isDateField(
            String key
    ) {

        String lower =
                key.toLowerCase();

        return lower.contains("date")
                || lower.contains("time")
                || lower.contains("timestamp")
                || lower.contains("created")
                || lower.contains("modified")
                || lower.contains("updated")
                || lower.contains("taken");
    }

    private void appendOtherDate(
            StringBuilder result,
            String name,
            String value
    ) {

        if (result.length() > 0) {
            result.append("\n");
        }

        result.append(name)
                .append(": ")
                .append(value);
    }

    /*
     * =========================================================
     * DETAIL POPUP
     * =========================================================
     */

    private void showDetailsPopup(
            JSONObject metadata
    ) {

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                dp(20),
                dp(8),
                dp(20),
                dp(8)
        );

        /*
         * -----------------------------------------------------
         * Photo Taken Time
         * -----------------------------------------------------
         *
         * This is shown separately because date fields are
         * otherwise excluded from the dynamic metadata section.
         */
        addPhotoTakenTime(
                container,
                metadata
        );

        /*
         * -----------------------------------------------------
         * Creation Time
         * -----------------------------------------------------
         */
        addCreationTime(
                container,
                metadata
        );

        /*
         * -----------------------------------------------------
         * Other metadata
         * -----------------------------------------------------
         */
        addNonDateMetadata(
                container,
                metadata,
                ""
        );

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);

        scrollView.addView(container);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("File Details")
                        .setView(scrollView)
                        .setPositiveButton(
                                "Close",
                                null
                        )
                        .create();

        dialog.show();
    }

    private void addPhotoTakenTime(
            LinearLayout container,
            JSONObject metadata
    ) {

        JSONObject photoTaken =
                metadata.optJSONObject(
                        "photoTakenTime"
                );

        if (photoTaken == null) {
            return;
        }

        String value =
                getDateFromObject(
                        photoTaken
                );

        if (value.isEmpty()) {
            return;
        }

        addSectionTitle(
                container,
                "Photo Taken Time"
        );

        addValue(
                container,
                value
        );
    }

    private void addCreationTime(
            LinearLayout container,
            JSONObject metadata
    ) {

        JSONObject creation =
                metadata.optJSONObject(
                        "creationTime"
                );

        if (creation == null) {
            return;
        }

        String value =
                getDateFromObject(
                        creation
                );

        if (value.isEmpty()) {
            return;
        }

        addSectionTitle(
                container,
                "Creation Time"
        );

        addValue(
                container,
                value
        );
    }

    private void addNonDateMetadata(
            LinearLayout container,
            JSONObject object,
            String prefix
    ) {

        Iterator<String> keys =
                object.keys();

        while (keys.hasNext()) {

            String key =
                    keys.next();

            /*
             * Date information is already displayed
             * separately above.
             */
            if (isDateField(key)) {
                continue;
            }

            Object value =
                    object.opt(key);

            if (value == null ||
                    value == JSONObject.NULL) {

                continue;
            }

            /*
             * Nested object
             */
            if (value instanceof JSONObject) {

                JSONObject child =
                        (JSONObject) value;

                if (child.length() == 0) {
                    continue;
                }

                addSectionTitle(
                        container,
                        prefix +
                                formatKey(key)
                );

                addNonDateMetadata(
                        container,
                        child,
                        prefix + "    "
                );

                continue;
            }

            /*
             * Array
             */
            if (value instanceof JSONArray) {

                JSONArray array =
                        (JSONArray) value;

                if (array.length() == 0) {
                    continue;
                }

                addSectionTitle(
                        container,
                        prefix +
                                formatKey(key)
                );

                for (int i = 0;
                     i < array.length();
                     i++) {

                    Object item =
                            array.opt(i);

                    if (item instanceof JSONObject) {

                        addNonDateMetadata(
                                container,
                                (JSONObject) item,
                                prefix + "    "
                        );

                    } else {

                        addValue(
                                container,
                                prefix +
                                        "    " +
                                        String.valueOf(
                                                item
                                        )
                        );
                    }
                }

                continue;
            }

            /*
             * Normal value
             */
            String stringValue =
                    String.valueOf(value);

            if (stringValue.trim().isEmpty()) {
                continue;
            }

            addValue(
                    container,
                    prefix +
                            formatKey(key) +
                            ": " +
                            stringValue
            );
        }
    }

    private void addSectionTitle(
            LinearLayout container,
            String text
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(text);

        tv.setTextSize(16);

        tv.setTypeface(
                null,
                Typeface.BOLD
        );

        tv.setPadding(
                0,
                dp(14),
                0,
                dp(5)
        );

        container.addView(tv);
    }

    private void addValue(
            LinearLayout container,
            String text
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(text);

        tv.setTextSize(15);

        tv.setPadding(
                0,
                dp(5),
                0,
                dp(5)
        );

        container.addView(tv);
    }

    private TextView createCell(
            String text,
            int widthDp
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(text);

        tv.setTextSize(14);

        tv.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        tv.setGravity(
                Gravity.CENTER_VERTICAL
        );

        tv.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(widthDp),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        tv.setBackgroundResource(
                android.R.drawable
                        .btn_default
        );

        return tv;
    }

    private String formatKey(
            String key
    ) {

        if (key == null ||
                key.isEmpty()) {

            return "";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < key.length();
             i++) {

            char c =
                    key.charAt(i);

            if (i == 0) {

                result.append(
                        Character.toUpperCase(c)
                );

            } else if (
                    Character.isUpperCase(c)
            ) {

                result.append(" ");
                result.append(c);

            } else {

                result.append(c);
            }
        }

        return result.toString();
    }

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}