package si.uni_lj.fe.tnuv;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;

import si.uni_lj.fe.tnuv.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private boolean isRecording = false;
    private AudioRecord audioRecord;
    private int bufferSize;
    private TextView audioLevelTextView;
    private static final int SAMPLE_RATE = 44100;
    private static final int RECORD_DURATION_MS = 3000; // Duration in milliseconds (3 seconds)
    private float maxSoundLevel = Float.MIN_VALUE;
    private MapView mapView;
    private PointAnnotationManager pointAnnotationManager;

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapView = binding.mapView;

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, this::onStyleLoaded);

        // Setup Search Bar
        EditText searchBar = findViewById(R.id.search_bar);
        // You can add functionality for the search bar here if needed

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.map) {
                resetMapView();
            } else if (item.getItemId() == R.id.mic) {
                showDialog();
            } else if (item.getItemId() == R.id.profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
            return true;
        });

        // Initialize audio-related variables
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        // Check for RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        } else {
            initializeAudio();
        }
    }

    private void onStyleLoaded(Style style) {
        createPointAnnotationManager();
        addMarkerToMap(Point.fromLngLat(-98.0, 39.5)); // Example point, adjust as needed
        resetMapView();
    }

    private void createPointAnnotationManager() {
        AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
        if (annotationPlugin != null) {
            pointAnnotationManager = annotationPlugin.createPointAnnotationManager();
        } else {
            Log.e("MainActivity", "Failed to get Annotation plugin");
        }
    }

    private void addMarkerToMap(Point point) {
        if (pointAnnotationManager != null) {
            PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(BitmapFactory.decodeResource(getResources(), R.drawable.red_marker));
            pointAnnotationManager.create(pointAnnotationOptions);
        }
    }

    private void resetMapView() {
        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                .center(Point.fromLngLat(-98.0, 39.5)) // Set the center point
                .pitch(0.0) // Pitch of the camera
                .zoom(2.0) // Zoom level
                .bearing(0.0) // Camera bearing
                .build());
    }

    private void showDialog() {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_audio_popup, null);

        final TextView audioLevelTextView = dialogView.findViewById(R.id.audio_level_text_view);
        Button startButton = dialogView.findViewById(R.id.start_button);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Audio level");
        builder.setView(dialogView);
        builder.setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();

        startButton.setOnClickListener(v -> {
            if(!isRecording) {
                startRecording(audioLevelTextView);
            }
        });

        dialog.show();
    }

    private void startRecording(TextView audioLevelTextView) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        } else {
            maxSoundLevel = Float.MIN_VALUE; // Reset the max sound level
            isRecording = true;
            new Thread(new AudioRecorder()).start();

            // Schedule stopping after the duration
            new Handler().postDelayed(() -> {
                stopRecording();
                audioLevelTextView.setText(String.format("Max Sound Level: %.2f dB", maxSoundLevel));
                audioLevelTextView.setVisibility(View.VISIBLE);  // Make the TextView visible
            }, RECORD_DURATION_MS);
        }
    }

    private void stopRecording() {
        if (isRecording) {
            isRecording = false;
            audioRecord.stop();
        }
    }

    private void updateUI(final float soundLevel) {
        runOnUiThread(() -> {
            // Update the max sound level if the current level is higher
            if (soundLevel > maxSoundLevel) {
                maxSoundLevel = soundLevel;
            }
        });
    }

    private float calculateSoundLevel(short[] buffer, int readSize) {
        double sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += buffer[i] * buffer[i];
        }
        double rms = Math.sqrt(sum / readSize);
        return (float) (20 * Math.log10(rms));
    }

    private class AudioRecorder implements Runnable {
        @Override
        public void run() {
            try {
                audioRecord.startRecording();
                short[] buffer = new short[bufferSize];
                while (isRecording) {
                    int readSize = audioRecord.read(buffer, 0, bufferSize);
                    if (readSize > 0) {
                        final float soundLevel = calculateSoundLevel(buffer, readSize);
                        updateUI(soundLevel);
                    }
                }
                audioRecord.stop();
            } catch (SecurityException e) {
                Toast.makeText(MainActivity.this, "Permission required for audio recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAudio();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeAudio() {
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission required for audio recording", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}