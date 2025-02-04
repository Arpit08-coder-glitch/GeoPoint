package com.quantasip.plotpoint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private EditText fullNameEditText, dobEditText, aadharNumberEditText;
    private ImageView documentImageView, plotImageView;
    private Button submitButton, uploadDocumentButton, uploadPlotButton;
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestore instance
    private Bitmap documentBitmap, plotBitmap;
    private boolean isDocumentImage = false; // Flag to check which image is being uploaded

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

            if (validateInput(fullName, dob, aadharNumber)) {
                // Convert bitmaps to Base64
                String documentImageBase64 = (documentBitmap != null) ? convertBitmapToBase64(documentBitmap) : null;
                String plotImageBase64 = (plotBitmap != null) ? convertBitmapToBase64(plotBitmap) : null;

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
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreError", "Error writing to Firestore: " + e.getMessage());
                            Toast.makeText(FormActivity.this, "Failed to save data in Firestore. Try again.", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    // Show image picker dialog (Camera or Gallery)
    private void showImagePickerDialog() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
                    bitmap = (Bitmap) data.getExtras().get("data");
                }

                if (bitmap != null) {
                    if (isDocumentImage) {
                        documentBitmap = bitmap;
                        documentImageView.setImageBitmap(bitmap);
                    } else {
                        plotBitmap = bitmap;
                        plotImageView.setImageBitmap(bitmap);
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
