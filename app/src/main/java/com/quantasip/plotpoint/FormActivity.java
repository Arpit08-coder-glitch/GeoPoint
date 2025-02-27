package com.quantasip.plotpoint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FormActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText fullNameEditText, dobEditText, aadharNumberEditText;
    private ImageView documentImageView, plotImageView;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestore instance
    private Bitmap documentBitmap, plotBitmap;
    private boolean isDocumentImage = false; // Flag to check which image is being uploaded
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Initialize views
        fullNameEditText = findViewById(R.id.fullNameEditText);
        dobEditText = findViewById(R.id.dobEditText);
        calendar = Calendar.getInstance();
        dobEditText.setOnClickListener(v -> showDatePicker());
        aadharNumberEditText = findViewById(R.id.aadharNumberEditText);
        documentImageView = findViewById(R.id.documentImageView);
        plotImageView = findViewById(R.id.plotImageView);
        Button submitButton = findViewById(R.id.submitButton);
        Button uploadDocumentButton = findViewById(R.id.uploadDocumentButton);
        Button uploadPlotButton = findViewById(R.id.uploadPlotButton);


        // Handle document image upload
        uploadDocumentButton.setOnClickListener(v -> {
            isDocumentImage = true;
            showImagePickerDialog();
        });

        // Handle plot image upload
        uploadPlotButton.setOnClickListener(v -> {
            isDocumentImage = false;
            showImagePickerDialog();
        });

        submitButton.setOnClickListener(v -> {
            String fullName = fullNameEditText.getText().toString();
            String dob = dobEditText.getText().toString();
            String aadharNumber = aadharNumberEditText.getText().toString();
            String documentImageBase64 = (documentBitmap != null) ? convertBitmapToBase64(documentBitmap) : null;
            String plotImageBase64 = (plotBitmap != null) ? convertBitmapToBase64(plotBitmap) : null;

            if (validateInput(fullName, dob, aadharNumber, documentImageBase64, plotImageBase64)) {

                // Prepare the data to be uploaded to Firestore
                Map<String, Object> formData = new HashMap<>();
                formData.put("full_name", fullName);
                formData.put("dob", dob);
                formData.put("aadhar_number", aadharNumber);
                formData.put("document_image", documentImageBase64);
                formData.put("plot_image", plotImageBase64);

                // Firestore collection name
                String collectionName = "user_forms";
                // Save data directly to Firestore
                db.collection(collectionName)
                        .document(aadharNumber)  // Use Aadhar number as the document ID
                        .set(formData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Data successfully written to Firestore!");
                            Toast.makeText(FormActivity.this, "Form submitted successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(FormActivity.this, DataActivity.class);
                                    startActivity(intent);
                                    finish(); // Optional: Close the current activity
                        }
                        )

                        .addOnFailureListener(e -> {
                            Log.e("FirestoreError", "Error writing to Firestore: " + e.getMessage());
                            Toast.makeText(FormActivity.this, "Failed to save data in Firestore. Try again.", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    dobEditText.setText(dateFormat.format(calendar.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    // Show image picker dialog (Camera or Gallery)
    @SuppressLint("IntentReset")
    private void showImagePickerDialog() {
        @SuppressLint("IntentReset") Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Intent chooser = Intent.createChooser(galleryIntent, "Select Image");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

        startActivityForResult(chooser, PICK_IMAGE_REQUEST);
    }

    // Handle image selection result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                Bitmap bitmap;
                if (data != null && data.getData() != null) {
                    // Image selected from gallery
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                } else {
                    // Image captured from camera
                    assert data != null;
                    bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                }

                if (bitmap != null) {
                    if (isDocumentImage) {
                        documentBitmap = bitmap;
                        documentImageView.setImageBitmap(bitmap);
                        documentImageView.setVisibility(View.VISIBLE); // Make visible when image is selected
                    } else {
                        plotBitmap = bitmap;
                        plotImageView.setImageBitmap(bitmap);
                        plotImageView.setVisibility(View.VISIBLE); // Make visible when image is selected
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Convert Bitmap to Base64 string
    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // 100 for highest quality
        byte[] byteArray = outputStream.toByteArray();
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Validate form inputs
    private boolean validateInput(String fullName, String dob, String aadharNumber, String documentImageBase64, String plotImageBase64) {
        if (fullName.isEmpty() || dob.isEmpty() || aadharNumber.isEmpty() || plotImageBase64==null || documentImageBase64==null) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!dob.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Invalid DOB format. Use YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (aadharNumber.length() != 12) {
            Toast.makeText(this, "Aadhar number must be 12 digits", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
