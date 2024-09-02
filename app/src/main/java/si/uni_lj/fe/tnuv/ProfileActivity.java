package si.uni_lj.fe.tnuv;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.content.SharedPreferences;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SonicSensePrefs";
    private static final String RECORDING_COUNT_KEY = "recordingCount";
    private static final String MAX_DB_KEY = "maxDB";
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupBackButton();
        displayUserInfo();
        updateMaxDBAndContributions();
        setBadgeIcons();
    }

    private void setupBackButton() {
        Button btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void displayUserInfo() {
        TextView nameTextView = findViewById(R.id.textView2);
        TextView emailTextView = findViewById(R.id.textView3);
        TextView bioTextView = findViewById(R.id.textView3);
        ImageView profileImageView = findViewById(R.id.imageView8);

        nameTextView.setText("Guissepe");
        emailTextView.setText("guissepe@hotmail.com");
        bioTextView.setText("Bio about me");
        profileImageView.setImageResource(R.drawable.baseline_account_circle_24);
    }

    private void updateMaxDBAndContributions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float maxDB = prefs.getFloat(MAX_DB_KEY, 0f);
        int recordingCount = prefs.getInt(RECORDING_COUNT_KEY, 0);

        Log.d(TAG, "Retrieved values - Max dB: " + maxDB + ", Recording Count: " + recordingCount);

        TextView maxDBTextView = findViewById(R.id.maxDb);
        TextView contributionsTextView = findViewById(R.id.contributionsView);
        TextView thankYouTextView = findViewById(R.id.thankyouView);

        maxDBTextView.setText(String.format("Max dB: %.1f dB", maxDB));
        contributionsTextView.setText(String.format("Your contributions: %d recordings", recordingCount));
        thankYouTextView.setText("Thank you for each of them!\nYou have helped the ears of many");

        Log.d(TAG, "UI updated with max dB and recording count");
    }

    private void setBadgeIcons() {
        int[] cardIds = {R.id.card1, R.id.card2, R.id.card3, R.id.card4, R.id.card5};
        int[] iconIds = {R.mipmap.crying, R.mipmap.laughter, R.mipmap.music, R.mipmap.drums, R.mipmap.plane};

        for (int i = 0; i < cardIds.length; i++) {
            ImageView cardIcon = findViewById(cardIds[i]).findViewById(R.id.card_icon);
            cardIcon.setImageResource(iconIds[i]);
        }
    }
}