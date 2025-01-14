package com.quantasip.plotpoint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin, btnSignUp;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            // First, check in the Admins collection
            db.collection("Admins").document(username).get()
                    .addOnSuccessListener(adminSnapshot -> {
                        if (adminSnapshot.exists()) {
                            handleLogin(adminSnapshot, password, true);
                        } else {
                            // If not found in Admins, check in the Users collection
                            db.collection("Users").document(username).get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        if (userSnapshot.exists()) {
                                            handleLogin(userSnapshot, password, false);
                                        } else {
                                            Toast.makeText(LoginActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this, "Error checking Users collection", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "Error checking Admins collection", Toast.LENGTH_SHORT).show();
                    });
        });
        // Sign-Up button listener
        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
    /**
     * Handles the login logic for both Admins and Users.
     *
     * @param snapshot   DocumentSnapshot of the user data
     * @param password   Entered password
     * @param isAdmin    Boolean indicating if the user is an Admin
     */
    private void handleLogin(DocumentSnapshot snapshot, String password, boolean isAdmin) {
        String storedPassword = snapshot.getString("password");

        if (storedPassword != null && storedPassword.equals(password)) {
            String username = snapshot.getId();  // Get the username (ID of the document)

            Intent intent;
            if (isAdmin) {
                // Navigate to Admin activity and pass the username
                intent = new Intent(LoginActivity.this, DataActivity.class);
            } else {
                // Navigate to User activity and pass the username
                intent = new Intent(LoginActivity.this, MainActivity.class);
            }

            // Pass the username to the next activity
            intent.putExtra("username", username);

            // Start the activity and finish the current one
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }
    }

}
