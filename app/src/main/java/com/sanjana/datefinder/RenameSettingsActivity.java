package com.sanjana.datefinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RenameSettingsActivity extends AppCompatActivity {

    private static final String PREFS =
            "DateFinderRenameSettings";

    private static final String RESULT_FILE =
            "scan_results.jsonl";

    private RadioGroup rgDateFormat;

    private RadioButton rbDateDefault;
    private RadioButton rbDateCustom;

    private Spinner spinnerDefaultDateFormat;
    private EditText etCustomDateFormat;

    private Spinner spinnerDateSource;

    private RadioGroup rgNameStyle;

    private RadioButton rbDateOnly;
    private RadioButton rbDateOriginal;
    private RadioButton rbOriginalDate;

    private RadioGroup rgMediaLabel;

    private RadioButton rbLabelNone;
    private RadioButton rbLabelAutomatic;
    private RadioButton rbLabelCustom;

    private EditText etImageLabel;
    private EditText etVideoLabel;
    private EditText etOtherLabel;

    private CheckBox cbSerial;

    private EditText etSerialStart;
    private EditText etSerialDigits;

    private RadioGroup rgSerialPosition;

    private RadioButton rbSerialBefore;
    private RadioButton rbSerialAfter;

    private CheckBox cbShowNoChange;

    private TextView tvRenameExample;

    private Button btnSave;

    private final List<String> dateSources =
            new ArrayList<>();

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_rename_settings
        );

        initializeViews();

        loadDateSources();

        setupDateFormat();

        setupDateSource();

        setupDefaults();

        setupListeners();

        updateRenameExample();

        btnSave.setOnClickListener(v ->
                saveSettings()
        );
    }

    // ---------------------------------------------------------
    // INITIALIZE
    // ---------------------------------------------------------

    private void initializeViews() {

        rgDateFormat =
                findViewById(
                        R.id.rgDateFormat
                );

        rbDateDefault =
                findViewById(
                        R.id.rbDateDefault
                );

        rbDateCustom =
                findViewById(
                        R.id.rbDateCustom
                );

        spinnerDefaultDateFormat =
                findViewById(
                        R.id.spinnerDefaultDateFormat
                );

        etCustomDateFormat =
                findViewById(
                        R.id.etCustomDateFormat
                );

        spinnerDateSource =
                findViewById(
                        R.id.spinnerDateSource
                );

        rgNameStyle =
                findViewById(
                        R.id.rgNameStyle
                );

        rbDateOnly =
                findViewById(
                        R.id.rbDateOnly
                );

        rbDateOriginal =
                findViewById(
                        R.id.rbDateOriginal
                );

        rbOriginalDate =
                findViewById(
                        R.id.rbOriginalDate
                );

        rgMediaLabel =
                findViewById(
                        R.id.rgMediaLabel
                );

        rbLabelNone =
                findViewById(
                        R.id.rbLabelNone
                );

        rbLabelAutomatic =
                findViewById(
                        R.id.rbLabelAutomatic
                );

        rbLabelCustom =
                findViewById(
                        R.id.rbLabelCustom
                );

        etImageLabel =
                findViewById(
                        R.id.etImageLabel
                );

        etVideoLabel =
                findViewById(
                        R.id.etVideoLabel
                );

        etOtherLabel =
                findViewById(
                        R.id.etOtherLabel
                );

        cbSerial =
                findViewById(
                        R.id.cbSerial
                );

        etSerialStart =
                findViewById(
                        R.id.etSerialStart
                );

        etSerialDigits =
                findViewById(
                        R.id.etSerialDigits
                );

        rgSerialPosition =
                findViewById(
                        R.id.rgSerialPosition
                );

        rbSerialBefore =
                findViewById(
                        R.id.rbSerialBefore
                );

        rbSerialAfter =
                findViewById(
                        R.id.rbSerialAfter
                );

        cbShowNoChange =
                findViewById(
                        R.id.cbShowNoChange
                );

        tvRenameExample =
                findViewById(
                        R.id.tvRenameExample
                );

        btnSave =
                findViewById(
                        R.id.btnSave
                );
    }

    // ---------------------------------------------------------
    // DATE SOURCES
    // ---------------------------------------------------------

    private void loadDateSources() {

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

        if (dateSources.isEmpty()) {

            dateSources.add(
                    "photoTakenTime"
            );

            dateSources.add(
                    "creationTime"
            );
        }
    }

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
            String value
    ) {

        String lower =
                value.toLowerCase(
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
    // SETUP DATE FORMAT
    // ---------------------------------------------------------

    private void setupDateFormat() {

        String[] formats = {
                "YYYY-MM-DD",
                "YYYY_MM_DD",
                "DD-MM-YYYY",
                "DD_MM_YYYY",
                "YYYYMMDD"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        formats
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerDefaultDateFormat.setAdapter(
                adapter
        );

        spinnerDefaultDateFormat.setSelection(
                1
        );
    }

    // ---------------------------------------------------------
    // SETUP DATE SOURCE
    // ---------------------------------------------------------

    private void setupDateSource() {

        List<String> displaySources =
                new ArrayList<>();

        for (String source :
                dateSources) {

            displaySources.add(
                    formatSourceName(
                            source
                    )
            );
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        displaySources
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerDateSource.setAdapter(
                adapter
        );
    }

    // ---------------------------------------------------------
    // DEFAULTS
    // ---------------------------------------------------------

    private void setupDefaults() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                );

        boolean initialized =
                prefs.getBoolean(
                        "initialized",
                        false
                );

        if (!initialized) {

            rbDateDefault.setChecked(
                    true
            );

            spinnerDefaultDateFormat.setSelection(
                    1
            );

            selectPhotoTakenTime();

            rbDateOnly.setChecked(
                    true
            );

            rbLabelAutomatic.setChecked(
                    true
            );

            etImageLabel.setText(
                    "IMG_"
            );

            etVideoLabel.setText(
                    "VID_"
            );

            etOtherLabel.setText(
                    ""
            );

            cbSerial.setChecked(
                    true
            );

            etSerialStart.setText(
                    "1"
            );

            etSerialDigits.setText(
                    "1"
            );

            rbSerialAfter.setChecked(
                    true
            );

            cbShowNoChange.setChecked(
                    true
            );

            return;
        }

        loadSavedSettings(
                prefs
        );
    }

    // ---------------------------------------------------------
    // LOAD SAVED SETTINGS
    // ---------------------------------------------------------

    private void loadSavedSettings(
            SharedPreferences prefs
    ) {

        String dateMode =
                prefs.getString(
                        "date_format_mode",
                        "Default"
                );

        if (dateMode.equals(
                "Custom"
        )) {

            rbDateCustom.setChecked(
                    true
            );

        } else {

            rbDateDefault.setChecked(
                    true
            );
        }

        String savedFormat =
                prefs.getString(
                        "date_format",
                        "YYYY_MM_DD"
                );

        setDateFormatSelection(
                savedFormat
        );

        etCustomDateFormat.setText(
                prefs.getString(
                        "custom_date_format",
                        "yyyy_MM_dd"
                )
        );

        String source =
                prefs.getString(
                        "date_source",
                        "photoTakenTime"
                );

        setDateSourceSelection(
                source
        );

        String nameStyle =
                prefs.getString(
                        "name_style",
                        "Date only"
                );

        if (nameStyle.equals(
                "Date + original"
        )) {

            rbDateOriginal.setChecked(
                    true
            );

        } else if (
                nameStyle.equals(
                        "Original name + date"
                )
        ) {

            rbOriginalDate.setChecked(
                    true
            );

        } else {

            rbDateOnly.setChecked(
                    true
            );
        }

        String labelMode =
                prefs.getString(
                        "media_label_mode",
                        "Automatic"
                );

        if (labelMode.equals(
                "None"
        )) {

            rbLabelNone.setChecked(
                    true
            );

        } else if (
                labelMode.equals(
                        "Custom"
                )
        ) {

            rbLabelCustom.setChecked(
                    true
            );

        } else {

            rbLabelAutomatic.setChecked(
                    true
            );
        }

        etImageLabel.setText(
                prefs.getString(
                        "image_label",
                        "IMG_"
                )
        );

        etVideoLabel.setText(
                prefs.getString(
                        "video_label",
                        "VID_"
                )
        );

        etOtherLabel.setText(
                prefs.getString(
                        "other_label",
                        ""
                )
        );

        cbSerial.setChecked(
                prefs.getBoolean(
                        "serial_enabled",
                        true
                )
        );

        etSerialStart.setText(
                String.valueOf(
                        prefs.getInt(
                                "serial_start",
                                1
                        )
                )
        );

        etSerialDigits.setText(
                String.valueOf(
                        prefs.getInt(
                                "serial_digits",
                                1
                        )
                )
        );

        String position =
                prefs.getString(
                        "serial_position",
                        "After date"
                );

        if (position.equals(
                "Before date"
        )) {

            rbSerialBefore.setChecked(
                    true
            );

        } else {

            rbSerialAfter.setChecked(
                    true
            );
        }

        cbShowNoChange.setChecked(
                prefs.getBoolean(
                        "show_no_change",
                        true
                )
        );
    }

    // ---------------------------------------------------------
    // LISTENERS
    // ---------------------------------------------------------

    private void setupListeners() {

        rgDateFormat.setOnCheckedChangeListener(
                (group, checkedId) -> {

                    updateCustomFormatVisibility();

                    updateRenameExample();
                }
        );

        spinnerDefaultDateFormat.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        updateRenameExample();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        spinnerDateSource.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        updateRenameExample();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        rgNameStyle.setOnCheckedChangeListener(
                (group, checkedId) ->
                        updateRenameExample()
        );

        rgMediaLabel.setOnCheckedChangeListener(
                (group, checkedId) -> {

                    updateLabelVisibility();

                    updateRenameExample();
                }
        );

        rgSerialPosition.setOnCheckedChangeListener(
                (group, checkedId) ->
                        updateRenameExample()
        );

        cbSerial.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        updateRenameExample()
        );

        cbShowNoChange.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        updateRenameExample()
        );

        addTextWatcher(
                etCustomDateFormat
        );

        addTextWatcher(
                etImageLabel
        );

        addTextWatcher(
                etVideoLabel
        );

        addTextWatcher(
                etOtherLabel
        );

        addTextWatcher(
                etSerialStart
        );

        addTextWatcher(
                etSerialDigits
        );

        updateCustomFormatVisibility();

        updateLabelVisibility();
    }

    private void addTextWatcher(
            EditText editText
    ) {

        if (editText == null) {
            return;
        }

        editText.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        updateRenameExample();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    private void updateCustomFormatVisibility() {

        etCustomDateFormat.setVisibility(
                rbDateCustom.isChecked()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void updateLabelVisibility() {

        boolean show =
                rbLabelCustom.isChecked();

        etImageLabel.setVisibility(
                show
                        ? View.VISIBLE
                        : View.GONE
        );

        etVideoLabel.setVisibility(
                show
                        ? View.VISIBLE
                        : View.GONE
        );

        etOtherLabel.setVisibility(
                show
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ---------------------------------------------------------
    // SAVE
    // ---------------------------------------------------------

    private void saveSettings() {

        SharedPreferences.Editor editor =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                )
                        .edit();

        String dateMode;

        if (rbDateCustom.isChecked()) {

            dateMode =
                    "Custom";

        } else {

            dateMode =
                    "Default";
        }

        String selectedFormat =
                String.valueOf(
                        spinnerDefaultDateFormat
                                .getSelectedItem()
                );

        String customFormat =
                etCustomDateFormat
                        .getText()
                        .toString()
                        .trim();

        if (customFormat.isEmpty()) {

            customFormat =
                    "yyyy_MM_dd";
        }

        int sourcePosition =
                spinnerDateSource
                        .getSelectedItemPosition();

        String dateSource =
                sourcePosition >= 0 &&
                        sourcePosition <
                                dateSources.size()
                        ? dateSources.get(
                        sourcePosition
                )
                        : "photoTakenTime";

        String nameStyle;

        if (rbDateOriginal.isChecked()) {

            nameStyle =
                    "Date + original";

        } else if (
                rbOriginalDate.isChecked()
        ) {

            nameStyle =
                    "Original name + date";

        } else {

            nameStyle =
                    "Date only";
        }

        String labelMode;

        if (rbLabelNone.isChecked()) {

            labelMode =
                    "None";

        } else if (
                rbLabelCustom.isChecked()
        ) {

            labelMode =
                    "Custom";

        } else {

            labelMode =
                    "Automatic";
        }

        int serialStart =
                parseInt(
                        etSerialStart
                                .getText()
                                .toString(),
                        1
                );

        int serialDigits =
                parseInt(
                        etSerialDigits
                                .getText()
                                .toString(),
                        1
                );

        if (serialStart < 0) {
            serialStart = 1;
        }

        if (serialDigits < 1) {
            serialDigits = 1;
        }

        String serialPosition =
                rbSerialAfter.isChecked()
                        ? "After date"
                        : "Before date";

        editor.putBoolean(
                "initialized",
                true
        );

        editor.putString(
                "date_format_mode",
                dateMode
        );

        editor.putString(
                "date_format",
                selectedFormat
        );

        editor.putString(
                "default_date_format",
                selectedFormat
        );

        editor.putString(
                "custom_date_format",
                customFormat
        );

        editor.putString(
                "date_source",
                dateSource
        );

        editor.putString(
                "name_style",
                nameStyle
        );

        editor.putString(
                "media_label_mode",
                labelMode
        );

        editor.putString(
                "image_label",
                etImageLabel
                        .getText()
                        .toString()
        );

        editor.putString(
                "video_label",
                etVideoLabel
                        .getText()
                        .toString()
        );

        editor.putString(
                "other_label",
                etOtherLabel
                        .getText()
                        .toString()
        );

        editor.putBoolean(
                "serial_enabled",
                cbSerial.isChecked()
        );

        editor.putInt(
                "serial_start",
                serialStart
        );

        editor.putInt(
                "serial_digits",
                serialDigits
        );

        editor.putString(
                "serial_position",
                serialPosition
        );

        editor.putBoolean(
                "show_no_change",
                cbShowNoChange.isChecked()
        );

        editor.apply();

        setResult(
                RESULT_OK,
                new Intent()
        );

        finish();
    }

    // ---------------------------------------------------------
    // PHOTO TAKEN TIME
    // ---------------------------------------------------------

    private void selectPhotoTakenTime() {

        for (int i = 0;
             i < dateSources.size();
             i++) {

            String value =
                    dateSources.get(i)
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (value.equals(
                    "phototakentime"
            )) {

                spinnerDateSource.setSelection(
                        i
                );

                return;
            }
        }

        for (int i = 0;
             i < dateSources.size();
             i++) {

            String value =
                    dateSources.get(i)
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (value.contains(
                    "phototakentime"
            )) {

                spinnerDateSource.setSelection(
                        i
                );

                return;
            }
        }
    }

    // ---------------------------------------------------------
    // DATE FORMAT SELECTION
    // ---------------------------------------------------------

    private void setDateFormatSelection(
            String value
    ) {

        String[] formats = {
                "YYYY-MM-DD",
                "YYYY_MM_DD",
                "DD-MM-YYYY",
                "DD_MM_YYYY",
                "YYYYMMDD"
        };

        for (int i = 0;
             i < formats.length;
             i++) {

            if (formats[i].equals(
                    value
            )) {

                spinnerDefaultDateFormat
                        .setSelection(i);

                return;
            }
        }

        spinnerDefaultDateFormat
                .setSelection(1);
    }

    // ---------------------------------------------------------
    // DATE SOURCE SELECTION
    // ---------------------------------------------------------

    private void setDateSourceSelection(
            String source
    ) {

        for (int i = 0;
             i < dateSources.size();
             i++) {

            if (dateSources.get(i).equals(
                    source
            )) {

                spinnerDateSource.setSelection(
                        i
                );

                return;
            }
        }

        selectPhotoTakenTime();
    }

    // ---------------------------------------------------------
    // RENAME EXAMPLE
    // ---------------------------------------------------------

    private void updateRenameExample() {

        if (tvRenameExample == null) {
            return;
        }

        /*
         * Sample date:
         * 26 November 2020
         */
        String date =
                getExampleDate();

        /*
         * Serial number uses the CURRENT
         * serial start and digit settings.
         */
        String serial = "";

        if (cbSerial != null &&
                cbSerial.isChecked()) {

            int start =
                    parseInt(
                            etSerialStart
                                    .getText()
                                    .toString(),
                            1
                    );

            int digits =
                    parseInt(
                            etSerialDigits
                                    .getText()
                                    .toString(),
                            1
                    );

            if (start < 0) {
                start = 1;
            }

            if (digits < 1) {
                digits = 1;
            }

            String number =
                    String.valueOf(start);

            while (number.length() < digits) {

                number =
                        "0" + number;
            }

            serial = number;
        }

        /*
         * Media label.
         */
        String prefix =
                "IMG_";

        if (rbLabelNone != null &&
                rbLabelNone.isChecked()) {

            prefix = "";

        } else if (
                rbLabelCustom != null &&
                        rbLabelCustom.isChecked()
        ) {

            prefix =
                    etImageLabel
                            .getText()
                            .toString();
        }

        String example;

        /*
         * DATE + ORIGINAL
         */
        if (rbDateOriginal != null &&
                rbDateOriginal.isChecked()) {

            if (cbSerial != null &&
                    cbSerial.isChecked()) {

                if (rbSerialAfter != null &&
                        rbSerialAfter.isChecked()) {

                    example =
                            prefix +
                                    date +
                                    "_" +
                                    serial +
                                    "_original.jpg";

                } else {

                    example =
                            prefix +
                                    serial +
                                    "_" +
                                    date +
                                    "_original.jpg";
                }

            } else {

                example =
                        prefix +
                                date +
                                "_original.jpg";
            }

            /*
             * ORIGINAL + DATE
             */
        } else if (
                rbOriginalDate != null &&
                        rbOriginalDate.isChecked()
        ) {

            if (cbSerial != null &&
                    cbSerial.isChecked()) {

                if (rbSerialAfter != null &&
                        rbSerialAfter.isChecked()) {

                    example =
                            prefix +
                                    "_original_" +
                                    date +
                                    "_" +
                                    serial +
                                    ".jpg";

                } else {

                    example =
                            prefix +
                                    serial +
                                    "_original_" +
                                    date +
                                    ".jpg";
                }

            } else {

                example =
                        prefix +
                                "_original_" +
                                date +
                                ".jpg";
            }

            /*
             * DATE ONLY
             */
        } else {

            if (cbSerial != null &&
                    cbSerial.isChecked()) {

                if (rbSerialAfter != null &&
                        rbSerialAfter.isChecked()) {

                    example =
                            prefix +
                                    date +
                                    "_" +
                                    serial +
                                    ".jpg";

                } else {

                    example =
                            prefix +
                                    serial +
                                    "_" +
                                    date +
                                    ".jpg";
                }

            } else {

                example =
                        prefix +
                                date +
                                ".jpg";
            }
        }

        tvRenameExample.setText(
                example
        );
    }

    // ---------------------------------------------------------
    // EXAMPLE DATE
    // ---------------------------------------------------------

    private String getExampleDate() {

        /*
         * The example represents:
         *
         * 26 Nov 2020
         *
         * regardless of which metadata source
         * is selected.
         */

        String javaFormat;

        if (rbDateCustom != null &&
                rbDateCustom.isChecked()) {

            javaFormat =
                    etCustomDateFormat
                            .getText()
                            .toString()
                            .trim();

            if (javaFormat.isEmpty()) {

                javaFormat =
                        "yyyy_MM_dd";
            }

        } else {

            String selected =
                    String.valueOf(
                            spinnerDefaultDateFormat
                                    .getSelectedItem()
                    );

            javaFormat =
                    convertSettingsFormatToJava(
                            selected
                    );
        }

        try {

            java.text.SimpleDateFormat input =
                    new java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            java.util.Date sample =
                    input.parse(
                            "2020-11-26"
                    );

            java.text.SimpleDateFormat output =
                    new java.text.SimpleDateFormat(
                            javaFormat,
                            Locale.US
                    );

            return output.format(
                    sample
            );

        } catch (Exception e) {

            return "2020_11_26";
        }
    }

    private String convertSettingsFormatToJava(
            String format
    ) {

        if (format == null ||
                format.isEmpty()) {

            return "yyyy_MM_dd";
        }

        return format
                .replace(
                        "YYYY",
                        "yyyy"
                )
                .replace(
                        "DD",
                        "dd"
                );
    }

    // ---------------------------------------------------------
    // FORMAT SOURCE NAME
    // ---------------------------------------------------------

    private String formatSourceName(
            String source
    ) {

        if (source == null ||
                source.isEmpty()) {

            return "";
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

                result.append(c);

            } else {

                result.append(c);
            }
        }

        return result.toString();
    }

    // ---------------------------------------------------------
    // PARSE INT
    // ---------------------------------------------------------

    private int parseInt(
            String value,
            int defaultValue
    ) {

        try {

            return Integer.parseInt(
                    value
            );

        } catch (Exception e) {

            return defaultValue;
        }
    }
}