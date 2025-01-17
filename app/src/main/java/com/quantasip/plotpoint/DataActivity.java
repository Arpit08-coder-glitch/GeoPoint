package com.quantasip.plotpoint;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.webkit.GeolocationPermissions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;

public class DataActivity extends AppCompatActivity {

    private WebView webView;
    private ImageButton backButton;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FirebaseAnalytics mFirebaseAnalytics;
    private String username;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        FirebaseApp.initializeApp(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Button allocateButton = findViewById(R.id.allocateButton);
        // Get the username passed from the login activity
        username = getIntent().getStringExtra("username");
        // Now you can use the username in this activity
        if (username != null) {
            // For example, display the username
            Log.d("LoggedInUser", "Username: " + username);
            // You can also use this username to track interactions, e.g., "Arpit clicked 2 times on plot no. 2"
        }

        // Log an event when the user opens the MainActivity
        logUserActivity("MainActivity Opened", "User opened MainActivity");

        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        webView = findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Always grant permission for geolocation
                callback.invoke(origin, true, false);
            }
        });
        WebView.setWebContentsDebuggingEnabled(true);

        // Enable JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        // Add JavaScript interface to the WebView
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        // Load the local HTML file with the map
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/map.html"); // Load the HTML file
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onPlotClick(String plotNo, int clickCount) {
                // Log the click data to Firebase Analytics
                Bundle bundle = new Bundle();
                bundle.putString("username", username);  // Username who clicked
                bundle.putString("plot_no", plotNo);     // Plot number clicked
                bundle.putInt("click_count", clickCount); // Number of clicks on this plot

                // Log custom event to Firebase Analytics
                mFirebaseAnalytics.logEvent("plot_click_event", bundle);

                // Optionally, display a message or store data as needed
            }
        }, "Android");


        // Initialize the back button (ImageButton)
        backButton = findViewById(R.id.backButton);

        // Set an OnClickListener to redirect to LoginActivity
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Log back button click
                logUserActivity("Back Button Clicked", "User clicked the back button");
                // Redirect to LoginActivity
                Intent intent = new Intent(DataActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Optionally finish the current activity
            }
        });
        // Set OnClickListener for the Allocate button
        allocateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to navigate to FormActivity
                Intent intent = new Intent(DataActivity.this, FormActivity.class);
                startActivity(intent);  // Start the new activity
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location access
                logUserActivity("Location Permission Granted", "User granted location permission");
            } else {
                // Permission denied, handle the case where location access is needed
                logUserActivity("Location Permission Denied", "User denied location permission");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // JavaScript Interface class to handle communication from WebView
    public class WebAppInterface {
        @JavascriptInterface
        public void trackWebViewActivity(String activityName, String activityDescription) {
            // Log the WebView activity to Firebase Analytics
            Log.d("WebView", "WebView Activity: " + activityName + ", Description: " + activityDescription);
            logUserActivity(activityName, activityDescription);
        }
    }

    // Log custom events to Firebase Analytics
    private void logUserActivity(String eventName, String eventDescription) {
        // Create a bundle to hold event parameters
        Bundle bundle = new Bundle();
        bundle.putString("user_id", FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown_user");
        bundle.putString("description", eventDescription);
        bundle.putLong("timestamp", System.currentTimeMillis());

        // Log the event to Firebase Analytics
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }
}
