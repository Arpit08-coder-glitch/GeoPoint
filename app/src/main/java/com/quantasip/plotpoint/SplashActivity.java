package com.quantasip.plotpoint;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Objects;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Thread thread = new Thread() {
            public void run() {
                try {
                    sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);

                    Intent intent;
                    if (isLoggedIn) {
                        UserRole role = getUserRole();
                        if (role == UserRole.ADMIN) {
                            intent = new Intent(SplashActivity.this, DataActivity.class);
                        } else if (role == UserRole.USER) {
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        } else {
                            intent = new Intent(SplashActivity.this, LoginActivity.class);
                        }
                    } else {
                        intent = new Intent(SplashActivity.this, LoginActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }
            }
        };
        thread.start();
    }

    private UserRole getUserRole() {
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String roleString = preferences.getString("role", null);
        return roleString != null ? UserRole.fromString(roleString) : null;
    }
}
