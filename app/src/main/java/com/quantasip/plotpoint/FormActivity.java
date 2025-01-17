package com.quantasip.plotpoint;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class FormActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_DOC_IMAGE = 1;
    private static final int REQUEST_CODE_PLOT_IMAGE = 2;

    private EditText fullNameEditText, dobEditText, aadharNumberEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form); // This is your form layout

        // Initialize Views
        fullNameEditText = findViewById(R.id.fullNameEditText);
        dobEditText = findViewById(R.id.dobEditText);
        aadharNumberEditText = findViewById(R.id.aadharNumberEditText);
        Button uploadDocButton = findViewById(R.id.uploadDocButton);
        Button uploadPlotButton = findViewById(R.id.uploadPlotButton);
        Button submitButton = findViewById(R.id.submitButton);

        // Setup Upload Button for Government Document
        uploadDocButton.setOnClickListener(v -> openCameraForDocument());

        // Setup Upload Button for Plot Photo
        uploadPlotButton.setOnClickListener(v -> openCameraForPlot());

        // Setup Submit Button
        submitButton.setOnClickListener(v -> submitForm());
    }

    // Open camera to capture document photo
    private void openCameraForDocument() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_DOC_IMAGE);
    }

    // Open camera to capture plot photo
    private void openCameraForPlot() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_PLOT_IMAGE);
    }

    // Handle the result of the camera capture
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Objects.requireNonNull(data.getExtras()).get("data");
            if (requestCode == REQUEST_CODE_DOC_IMAGE) {
            } else if (requestCode == REQUEST_CODE_PLOT_IMAGE) {
            }
        }
    }

    // Submit form data
    private void submitForm() {
        String fullName = fullNameEditText.getText().toString();
        String dob = dobEditText.getText().toString();
        String aadharNumber = aadharNumberEditText.getText().toString();

        // Here, you can process and store the form data and images.
        // You may send them to a server or save them locally.
        // For now, just display a message.
        if (fullName.isEmpty() || dob.isEmpty() || aadharNumber.isEmpty()) {
            // Show error if any field is empty
        }

        // You can send data and photos to the server or save them locally
        // Example: Save or upload logic here
    }
}
