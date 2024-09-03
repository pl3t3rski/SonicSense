package si.uni_lj.fe.tnuv;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
        int[] iconIds = {R.drawable.ic_2mega, R.drawable.ic_2kite, R.drawable.ic_musicnote, R.mipmap.drums, R.mipmap.plane};
        String[] badgeTitles = {
                "Loud Noise Badge",
                "Kindergarten Badge",
                "Local Music Badge",
                "School Drums Badge",
                "Airport Badge"
        };
        String[] badgeDescriptions = {
                "You've recorded a sound over 120dB! This badge represents your encounter with extremely loud noises. Remember, prolonged exposure to such high levels can be harmful to hearing.",
                "You've successfully recorded sounds in a kindergarten! This badge celebrates the joyful and often chaotic soundscape of young children at play.",
                "Congratulations on recording sound at a local music venue! This badge recognizes your contribution to capturing the vibrant local music scene.",
                "Not achieved yet. To earn this badge, record a sound at a local school. Perhaps you'll capture the rhythmic beats of a school band or drum circle!",
                "Not achieved yet. To unlock this badge, you need to record a sound at the airport. The roar of jet engines awaits your microphone!"
        };

        for (int i = 0; i < cardIds.length; i++) {
            View cardView = findViewById(cardIds[i]);
            ImageView cardIcon = cardView.findViewById(R.id.card_icon);
            cardIcon.setImageResource(iconIds[i]);

            final int index = i;
            cardView.setOnClickListener(v -> showBadgeDescription(badgeTitles[index], badgeDescriptions[index]));

            if (i >= 3) {
                cardIcon.setAlpha(0.1f);
            } else {
                cardIcon.setAlpha(1.0f);
                // Set yellow background for achieved badges
                cardView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            }
        }
    }

    private void showBadgeDescription(String title, String description) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_badge_description);

        TextView titleView = dialog.findViewById(R.id.badgeTitle);
        TextView descriptionView = dialog.findViewById(R.id.badgeDescription);
        Button dismissButton = dialog.findViewById(R.id.dismissButton2);

        titleView.setText(title);
        descriptionView.setText(description);
        dismissButton.setOnClickListener(v -> dialog.dismiss());

        // Set the dialog width to 90% of the screen width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(lp);

        dialog.show();
    }
}