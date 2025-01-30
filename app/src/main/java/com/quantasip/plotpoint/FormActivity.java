package com.quantasip.plotpoint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class FormActivity extends Activity {

    private EditText fullNameEditText, dobEditText, aadharNumberEditText;
    private ImageView documentImageView, plotImageView;
    private Button submitButton, uploadDocumentButton, uploadPlotButton;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();  // Initialize Firestore
    private Bitmap documentBitmap, plotBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Initialize views
        fullNameEditText = findViewById(R.id.fullNameEditText);
        dobEditText = findViewById(R.id.dobEditText);
        aadharNumberEditText = findViewById(R.id.aadharNumberEditText);
        documentImageView = findViewById(R.id.documentImageView);
        plotImageView = findViewById(R.id.plotImageView);
        submitButton = findViewById(R.id.submitButton);
        uploadDocumentButton = findViewById(R.id.uploadDocumentButton);
        uploadPlotButton = findViewById(R.id.uploadPlotButton);

        // Handle document image upload
        uploadDocumentButton.setOnClickListener(v -> openGalleryForImage(1));

        // Handle plot image upload
        uploadPlotButton.setOnClickListener(v -> openGalleryForImage(2));
        submitButton.setOnClickListener(v -> {
            String fullName = fullNameEditText.getText().toString();
            String dob = dobEditText.getText().toString();
            String aadharNumber = aadharNumberEditText.getText().toString();

            if (validateInput(fullName, dob, aadharNumber)) {
                // Store form data locally in FormData class
                FormData.addForm(aadharNumber, fullName, dob);

                // Optionally, check the stored form data
                FormData.Form form = FormData.getForm(aadharNumber);
                // Display the stored form data in a Toast
                String formDataMessage = "Stored Form Data: Full Name: " + form.getFullName() +
                        ", DOB: " + form.getDob() + ", Aadhar Number: " + form.getAadharNumber();
                Toast.makeText(FormActivity.this, formDataMessage, Toast.LENGTH_LONG).show();

                // Prepare the data to be uploaded to Firestore
                Map<String, Object> formData = new HashMap<>();
                formData.put("full_name", fullName);
                formData.put("dob", dob);
                formData.put("aadhar_number", aadharNumber);

                // Specify collection name, for example, "user_forms"
                String collectionName = "user_forms";

                // Save to Firestore
                db.collection(collectionName)
                        .document(aadharNumber)  // Use Aadhar number as the document ID
                        .set(formData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Data successfully written to Firestore!");
                                        Toast.makeText(FormActivity.this, "Form submitted successfully", Toast.LENGTH_SHORT).show();

                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreError", "Error writing to Firestore: " + e.getMessage());
                            Toast.makeText(FormActivity.this, "Failed to save data in Firestore. Try again.", Toast.LENGTH_SHORT).show();
                        });
            }
        });



    }
    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Compress the bitmap to PNG format (or you can use JPEG)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // 100 for highest quality
        return outputStream.toByteArray();
    }

    // Open gallery to select image
    private void openGalleryForImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    // Handle image selection result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                if (requestCode == 1) {
                    documentBitmap = bitmap;
                    documentImageView.setImageBitmap(bitmap);
                } else if (requestCode == 2) {
                    plotBitmap = bitmap;
                    plotImageView.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Validate form inputs
    private boolean validateInput(String fullName, String dob, String aadharNumber) {
        if (fullName.isEmpty() || dob.isEmpty() || aadharNumber.isEmpty()) {
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
