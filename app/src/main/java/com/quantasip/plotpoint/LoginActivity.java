package com.quantasip.plotpoint;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
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

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnSignUp;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 1001;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
                Log.e("LoginActivity", "Google Sign-In failed", e);
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
                        if (user != null) {
                            showRoleSelectionDialog(user);
                        }
                    } else {
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Show a dialog for the user to select their role.
     */
    private void showRoleSelectionDialog(FirebaseUser user) {
        String[] roles = {"USER", "ADMIN"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Your Role")
                .setItems(roles, (dialog, which) -> {
                    String selectedRole = roles[which];
                    storeUserCredentials(user, selectedRole);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Store the user's credentials in the Firestore database.
     *
     * @param user         The signed-in FirebaseUser
     * @param selectedRole The role selected by the user
     */
    private void storeUserCredentials(FirebaseUser user, String selectedRole) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getDisplayName());
        userData.put("role", selectedRole);

        // Use email as the document ID
        String documentId = user.getEmail();

        // Determine the collection based on the role
        String collection = selectedRole.equals("ADMIN") ? "Admins" : "Users";

        db.collection(collection).document(documentId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "Welcome, " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                    navigateToNextActivity(selectedRole);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    Log.e("LoginActivity", "Error saving user data", e);
                });
    }

    /**
     * Navigate to the next activity based on the user's role.
     *
     * @param role The role selected by the user
     */
    private void navigateToNextActivity(String role) {
        Intent intent;
        if ("ADMIN".equals(role)) {
            intent = new Intent(LoginActivity.this, DataActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }
        startActivity(intent);
        finish();
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
            Intent intent;
            if (isAdmin) {
                // Navigate to Admin activity
                intent = new Intent(LoginActivity.this, DataActivity.class);
            } else {
                // Navigate to User activity
                intent = new Intent(LoginActivity.this, MainActivity.class);
            }
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }
    }
}
