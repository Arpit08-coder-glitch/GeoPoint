package com.quantasip.plotpoint;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class FormActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_DOC_IMAGE = 1;
    private static final int REQUEST_CODE_PLOT_IMAGE = 2;

    private EditText fullNameEditText, dobEditText, aadharNumberEditText;
    private byte[] documentImage, plotImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form); // Ensure the layout is defined

        // Initialize Views
        fullNameEditText = findViewById(R.id.fullNameEditText);
        dobEditText = findViewById(R.id.dobEditText);
        aadharNumberEditText = findViewById(R.id.aadharNumberEditText);
        Button uploadDocButton = findViewById(R.id.uploadDocButton);
        Button uploadPlotButton = findViewById(R.id.uploadPlotButton);
        Button submitButton = findViewById(R.id.submitButton);

        // Setup Upload Buttons
        uploadDocButton.setOnClickListener(v -> openCameraForDocument());
        uploadPlotButton.setOnClickListener(v -> openCameraForPlot());

        // Setup Submit Button
        submitButton.setOnClickListener(v -> submitForm());
    }

    private void openCameraForDocument() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_DOC_IMAGE);
    }

    private void openCameraForPlot() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_PLOT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byte[] imageBytes = outputStream.toByteArray();

                if (requestCode == REQUEST_CODE_DOC_IMAGE) {
                    documentImage = imageBytes;
                } else if (requestCode == REQUEST_CODE_PLOT_IMAGE) {
                    plotImage = imageBytes;
                }
            }
        }
    }

    private void submitForm() {
        String fullName = fullNameEditText.getText().toString();
        String dob = dobEditText.getText().toString();
        String aadharNumber = aadharNumberEditText.getText().toString();

        if (fullName.isEmpty() || dob.isEmpty() || aadharNumber.isEmpty() || documentImage == null || plotImage == null) {
            Toast.makeText(this, "Please fill all fields and upload images", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> saveDataToDatabase(fullName, dob, aadharNumber, documentImage, plotImage)).start();
    }

    private void saveDataToDatabase(String fullName, String dob, String aadharNumber, byte[] documentImage, byte[] plotImage) {
        String url = "jdbc:postgresql://45.251.14.68:5432/Demo";
        String user = "user_it";
        String password = "Qawsed*&^%";

        String sql = "INSERT INTO user_data (full_name, dob, aadhar_number, document_image, plot_image) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fullName);

            // Parse and set the date
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date parsedDate = dateFormat.parse(dob);
            stmt.setDate(2, new java.sql.Date(parsedDate.getTime()));

            stmt.setString(3, aadharNumber);
            stmt.setBytes(4, documentImage);
            stmt.setBytes(5, plotImage);

            stmt.executeUpdate();

            runOnUiThread(() -> Toast.makeText(this, "Form submitted successfully", Toast.LENGTH_SHORT).show());
        } catch (SQLException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (ParseException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD", Toast.LENGTH_SHORT).show());
        }
    }
}
