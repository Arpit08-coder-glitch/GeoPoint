package com.quantasip.plotpoint;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etConfirmPassword, etEmail;
    private RadioGroup rgRole;
    private Button btnSignUp;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DatabaseReference realtimeDb;
    private static final int RC_SIGN_IN = 1001;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth, Firestore, and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference("");
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> signInWithGoogle());

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etEmail = findViewById(R.id.etEmail);
        rgRole = findViewById(R.id.rgRole);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            int selectedRoleId = rgRole.getCheckedRadioButtonId();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty() || selectedRoleId == -1) {
                Toast.makeText(SignUpActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Password strength check
            if (!isPasswordStrong(password)) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 8 characters long, with a mix of letters, numbers, and special characters.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRole = findViewById(selectedRoleId);
            UserRole role = UserRole.fromString(selectedRole.getText().toString());

            if (role == null) {
                Toast.makeText(SignUpActivity.this, "Invalid role selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if username already exists in Firestore
            db.collection("data")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                // Username already exists
                                Toast.makeText(SignUpActivity.this, "Username already exists. Please choose a different username.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Proceed with account creation
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(authTask -> {
                                            if (authTask.isSuccessful()) {
                                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                                if (user != null) {
                                                    user.sendEmailVerification()
                                                            .addOnCompleteListener(emailVerificationTask -> {
                                                                if (emailVerificationTask.isSuccessful()) {
                                                                    Toast.makeText(SignUpActivity.this, "Verification email sent. Please verify your email.", Toast.LENGTH_SHORT).show();

                                                                    // Add listener for email verification
                                                                    checkEmailVerification(user, username, email, role, password);
                                                                } else {
                                                                    Toast.makeText(SignUpActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }
                                            } else {
                                                Toast.makeText(SignUpActivity.this, "Error creating account. Try again.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "Error checking username availability. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(Exception.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (Exception e) {
                Log.e("SignUpActivity", "Google Sign-In failed", e);
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Sign-In successful: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                        // Navigate to the next activity
                    } else {
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkEmailVerification(FirebaseUser user, String username, String email, UserRole role, String password) {
        new Thread(() -> {
            while (!user.isEmailVerified()) {
                try {
                    Thread.sleep(2000); // Wait for 2 seconds before checking again
                    user.reload(); // Reload the user to get the latest verification status
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(() -> {
                // Store user data in Firestore and Realtime Database after email is verified
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", username);
                userData.put("email", email);
                userData.put("role", role.toString());
                userData.put("password", password); // Note: Storing passwords in plaintext is not secure. Use a secure hashing method instead.

                // Determine collection based on role
                String collectionName = role == UserRole.ADMIN ? "Admins" : "Users";

                // Save to Firestore
                db.collection(collectionName)
                        .document(username)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            // Save to Realtime Database
                            realtimeDb.child(collectionName).child(username).setValue(userData).addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(SignUpActivity.this, "Registration successful! You can now log in.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(SignUpActivity.this, "Failed to save data in Realtime Database.", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SignUpActivity.this, "Failed to register. Try again.", Toast.LENGTH_SHORT).show();
                        });
            });
        }).start();
    }

    // Password strength check (minimum 8 characters, mix of letters, numbers, and special chars)
    private boolean isPasswordStrong(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Za-z].*") &&
                password.matches(".*[0-9].*") &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }
}
