package si.uni_lj.fe.tnuv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
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
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;

import java.util.List;
import java.util.Locale;

import si.uni_lj.fe.tnuv.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements LocationListener{

    private static final String PREFS_NAME = "SonicSensePrefs";
    private static final String MAX_DB_KEY = "maxDB";
    private static final String RECORDING_COUNT_KEY = "recordingCount";
    private static final int REQUEST_CODE = 100;
    private static final int SAMPLE_RATE = 44100;
    private static final int RECORD_DURATION_MS = 3000; // Duration in milliseconds (3 seconds)
    private static final String TAG = "MainActivity";

    private boolean isRecording = false;
    private AudioRecord audioRecord;
    private int bufferSize;
    private TextView audioLevelTextView;
    private float maxSoundLevel = Float.MIN_VALUE;
    private float overallMaxDB = Float.MIN_VALUE;
    private MapView mapView;
    private PointAnnotationManager pointAnnotationManager;
    LocationManager locationManager;
    ActivityMainBinding binding;
    private double userLatitude = 0.0;
    private double userLongitude = 0.0;
    private String address = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapView = binding.mapView;


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

        // Load the overall max dB from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        overallMaxDB = prefs.getFloat(MAX_DB_KEY, Float.MIN_VALUE);

        // Check for RECORD_AUDIO permission

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }
    }

    private void resetMapView() {
        getLocation();
        if(userLatitude != 0.0 && userLongitude != 0.0) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(Point.fromLngLat(userLongitude, userLatitude)) // Set the center point
                    .pitch(0.0) // Pitch of the camera
                    .zoom(2.0) // Zoom level
                    .bearing(0.0) // Camera bearing
                    .build());
        } else {
            Toast.makeText(this, "Location not yet available", Toast.LENGTH_SHORT).show();
        }

    }

    private void showDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        } else {
            initializeAudio();
        }
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_audio_popup, null);

        final TextView maxSoundLevelTextView = dialogView.findViewById(R.id.max_sound_level_text_view);
        final TextView addressTextView = dialogView.findViewById(R.id.address_text_view);
        final TextView quietnessMessageTextView = dialogView.findViewById(R.id.quietness_message_text_view);
        Button startButton = dialogView.findViewById(R.id.start_button);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Audio level");
        builder.setView(dialogView);
        builder.setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();

        startButton.setOnClickListener(v -> {
            if(!isRecording) {
                startRecording(maxSoundLevelTextView, addressTextView, quietnessMessageTextView);
            }
        });

        dialog.show();
    }

    private void startRecording(TextView maxSoundLevelTextView, TextView addressTextView, TextView quietnessMessageTextView) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        } else {
            maxSoundLevel = Float.MIN_VALUE; // Reset the max sound level for this recording session
            isRecording = true;
            new Thread(new AudioRecorder()).start();

            // Schedule stopping after the duration
            new Handler().postDelayed(() -> {
                stopRecording();
                maxSoundLevelTextView.setText(String.format("Sound Level: %.2f dB", maxSoundLevel));
                if (maxSoundLevel > 60) {
                    maxSoundLevelTextView.setTextColor(ContextCompat.getColor(this, R.color.red));
                } else {
                    maxSoundLevelTextView.setTextColor(ContextCompat.getColor(this, R.color.green));
                }
                maxSoundLevelTextView.setVisibility(View.VISIBLE);  // Make the TextView visible
                addressTextView.setText(String.format("Address: %s", getAddress()));
                addressTextView.setVisibility(View.VISIBLE);

                quietnessMessageTextView.setText(getQuietnessMessage(maxSoundLevel));
                quietnessMessageTextView.setVisibility(View.VISIBLE);
                onRecordingComplete();
                Log.d(TAG, "Recording stopped and onRecordingComplete called");
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

    private void updateOverallMaxDB(float currentMaxDB) {
        if (currentMaxDB > overallMaxDB) {
            overallMaxDB = currentMaxDB;
            // Save to SharedPreferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(MAX_DB_KEY, overallMaxDB);
            editor.apply();
        }
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

    private void incrementRecordingCount() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentCount = prefs.getInt(RECORDING_COUNT_KEY, 0);
        int newCount = currentCount + 1;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(RECORDING_COUNT_KEY, newCount);
        editor.apply();
        Log.d(TAG, "Recording count incremented. New count: " + newCount);
    }

    private void onRecordingComplete() {
        updateOverallMaxDB(maxSoundLevel);
        incrementRecordingCount();
        Log.d(TAG, "Recording completed. Max dB: " + maxSoundLevel);
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

    @SuppressLint("MissingPermission")
    private void getLocation() {

        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, MainActivity.this);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(@NonNull android.location.Location location) {
        userLatitude = location.getLatitude();
        userLongitude = location.getLongitude();
        resetMapView();
        Toast.makeText(this, ""+location.getLatitude()+","+location.getLongitude(), Toast.LENGTH_SHORT).show();
        try {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                address = addresses.get(0).getAddressLine(0);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    private String getAddress() {
        // Assuming address is stored in userAddress variable
        return address;
    }

    private String getQuietnessMessage(float soundLevel) {
        // You can adjust this threshold as needed
        if (soundLevel < 60) {
            return "Your location is quiet and safe for your ears";
        } else {
            return "Your location is noisy. Consider using ear protection.";
        }
    }

}

