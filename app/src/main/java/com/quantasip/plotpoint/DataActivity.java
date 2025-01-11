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
import android.widget.ImageButton;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.webkit.GeolocationPermissions;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

public class DataActivity extends AppCompatActivity {
    private WebView webView;
    private ImageButton backButton;
    private FirebaseAnalytics mFirebaseAnalytics;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        FirebaseApp.initializeApp(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Log an event
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "Login");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
        // Add admin-specific functionality
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
        webView.addJavascriptInterface(new DataActivity.WebAppInterface(), "AndroidInterface");


        // Load the local HTML file with map
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/map.html"); // Load the HTML file
        // Initialize the back button (ImageButton)
        backButton = findViewById(R.id.backButton);

        // Set an OnClickListener to redirect to LoginActivity
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to LoginActivity
                Intent intent = new Intent(DataActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Optionally finish the current activity
            }
        });
    }
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location access
            } else {
                // Permission denied, handle the case where location access is needed
            }
        }
    }
    // JavaScript Interface class to handle communication from WebView
    public class WebAppInterface {
        @JavascriptInterface
        public void receiveLocation(String lat, String lon) {
            // Handle the received location data
            Log.d("WebView", "Received location: " + lat + ", " + lon);
        }
    }
}
