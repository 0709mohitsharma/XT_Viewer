package com.sharmaji.mdviewer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import es.dmoral.markdownview.MarkdownView;

public class MainActivity extends AppCompatActivity {
    MarkdownView markdownView;
    Uri fileUri;
    File mdfile;
    String TAG = "Peter Main";
    // Handle incoming intent
    Intent intent;
    String action;
    String type;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
//        getWindow().setStatusBarColor(Color.parseColor("#ffffff")); // Replace with your color value
        // Checking permissions
        checkStoragePermissions();
        // Initializations
        markdownView = findViewById(R.id.markdown_view);

        intent = getIntent();
        action = intent.getAction();
        type = intent.getType();

        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            // Activity opened via another app
            // Handle file opening here
            // Handle Markdown file
            Uri uri = intent.getData();
            if (uri != null) {
                mdfile = uriToFile(uri, this);
                if (mdfile != null) {
                    markdownView.loadFromFile(mdfile);
                } else {
                    String filePath = uri.getPath();
                    if (filePath != null) {
                        mdfile = new File(filePath);
                        if (mdfile.exists()) {
                            markdownView.loadFromFile(mdfile);
                        } else {
                            Toast.makeText(this, "Error: File not found!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Error: Invalid URI!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            } else {
                Toast.makeText(this, "Invalid URI!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFileIntent.setType("*/*"); // To accept any file type
            pickFileLauncher.launch(chooseFileIntent);
        }

    }

    public void checkStoragePermissions() {
        if (Environment.isExternalStorageManager()) {
            return;
        } else {
            requestStoragePermission();
        }
    }

    private void requestStoragePermission() {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", this.getPackageName(), null);
            intent.setData(uri);
            storageActivityResultLauncher.launch(intent);
        } catch (Exception e) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            storageActivityResultLauncher.launch(intent);
        }
    }

    private final ActivityResultLauncher<Intent> storageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    o -> {
                        //Android is 11 (R) or above
                        if (Environment.isExternalStorageManager()) {
                            //Manage External Storage Permissions Granted
                            Log.d(TAG, "onActivityResult: Manage External Storage Permissions Granted");
                        } else {
                            Toast.makeText(MainActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                        }
                    });

    private File uriToFile(Uri uri, Context context) {
        if (uri == null) return null;

        // Get file name from URI
        String fileName = "temp_file"; // Provide a default file name
        if (uri.getLastPathSegment() != null) {
            fileName = uri.getLastPathSegment();
        }

        // Create a temporary file to copy content
        File tempFile = new File(context.getCacheDir(), fileName);

        try {
            // Open an input stream for the given URI
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            // Copy content from input stream to temporary file
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (IOException e) {
            Log.e("uriToFile Error:", Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }

    private final ActivityResultLauncher<Intent> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // The user picked a file
                    try {
                        fileUri = result.getData().getData();
                        mdfile = uriToFile(fileUri, this);
                        if (mdfile != null) {
                            markdownView.loadFromFile(mdfile);
                        } else {
                            String filePath = fileUri.getPath();
                            if (filePath != null) {
                                mdfile = new File(filePath);
                                if (mdfile.exists()) {
                                    markdownView.loadFromFile(mdfile);
                                } else {
                                    Toast.makeText(this, "Error: File not found!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            } else {
                                Toast.makeText(this, "Error: Invalid URI!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("pickFileLauncher Error:", Objects.requireNonNull(e.getMessage()));
                    }

                }
            });


}