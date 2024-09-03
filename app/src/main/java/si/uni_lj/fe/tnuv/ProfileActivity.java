package si.uni_lj.fe.tnuv;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SonicSensePrefs";
    private static final String RECORDING_COUNT_KEY = "recordingCount";
    private static final String MAX_DB_KEY = "maxDB";
    private static final String USER_NAME_KEY = "userName";
    private static final String USER_BIO_KEY = "userBio";
    private static final String TAG = "ProfileActivity";

    private EditText nameEditText, bioEditText;
    private TextView emailTextView;
    private ImageView profileImageView;
    private TextView maxDBTextView, contributionsTextView, thankYouTextView;
    private Button editButton, saveButton;
    private View statsCardView, badgesSection;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupBackButton();
        loadUserInfo();
        updateMaxDBAndContributions();
        setBadgeIcons();
        setupEditButton();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailTextView = findViewById(R.id.emailTextView);
        bioEditText = findViewById(R.id.bioEditText);
        profileImageView = findViewById(R.id.profileImageView);
        maxDBTextView = findViewById(R.id.maxDb);
        contributionsTextView = findViewById(R.id.contributionsView);
        thankYouTextView = findViewById(R.id.thankyouView);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);
        statsCardView = findViewById(R.id.statsCardView);
        badgesSection = findViewById(R.id.badgesSection);

        // Set up EditTexts to behave like TextViews initially
        nameEditText.setInputType(InputType.TYPE_NULL);
        nameEditText.setTextIsSelectable(true);
        bioEditText.setInputType(InputType.TYPE_NULL);
        bioEditText.setTextIsSelectable(true);
    }

    private void setupBackButton() {
        Button btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> navigateToMainActivity());
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadUserInfo() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userName = prefs.getString(USER_NAME_KEY, "Giuseppe");
        String userBio = prefs.getString(USER_BIO_KEY, "Bio about me");

        nameEditText.setText(userName);
        bioEditText.setText(userBio);
        emailTextView.setText("giuseppe@hotmail.com");
        profileImageView.setImageResource(R.drawable.baseline_account_circle_24);
    }

    private void updateMaxDBAndContributions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float maxDB = prefs.getFloat(MAX_DB_KEY, 0f);
        int recordingCount = prefs.getInt(RECORDING_COUNT_KEY, 0);

        Log.d(TAG, "Retrieved values - Max dB: " + maxDB + ", Recording Count: " + recordingCount);

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
            setupBadgeCard(cardIds[i], iconIds[i], badgeTitles[i], badgeDescriptions[i], i < 3);
        }
    }

    private void setupBadgeCard(int cardId, int iconId, String title, String description, boolean isAchieved) {
        View cardView = findViewById(cardId);
        ImageView cardIcon = cardView.findViewById(R.id.card_icon);
        cardIcon.setImageResource(iconId);

        cardView.setOnClickListener(v -> showBadgeDescription(title, description));

        cardIcon.setAlpha(isAchieved ? 1.0f : 0.1f);
        if (isAchieved) {
            cardView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
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

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(lp);

        dialog.show();
    }

    private void setupEditButton() {
        editButton.setOnClickListener(v -> toggleEditMode());
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            nameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            bioEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            nameEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            bioEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

            nameEditText.setFocusableInTouchMode(true);
            bioEditText.setFocusableInTouchMode(true);

            editButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setAlpha(0.75f);  // Lower opacity in edit mode

            // Change background and text color for edit mode
            int editBackgroundColor = ContextCompat.getColor(this, R.color.editBackgroundColor);
            int editTextColor = ContextCompat.getColor(this, R.color.editTextColor);

            nameEditText.setBackgroundColor(editBackgroundColor);
            bioEditText.setBackgroundColor(editBackgroundColor);
            nameEditText.setTextColor(editTextColor);
            bioEditText.setTextColor(editTextColor);
        } else {
            nameEditText.setInputType(InputType.TYPE_NULL);
            bioEditText.setInputType(InputType.TYPE_NULL);
            nameEditText.setTextIsSelectable(true);
            bioEditText.setTextIsSelectable(true);

            nameEditText.setFocusable(false);
            bioEditText.setFocusable(false);

            editButton.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.GONE);
            editButton.setAlpha(1.0f);  // Full opacity in normal mode

            // Reset background and text color
            nameEditText.setBackgroundColor(Color.TRANSPARENT);
            bioEditText.setBackgroundColor(Color.TRANSPARENT);
            nameEditText.setTextColor(Color.BLACK);
            bioEditText.setTextColor(Color.BLACK);
        }
    }

    private void saveProfile() {
        String newName = nameEditText.getText().toString();
        String newBio = bioEditText.getText().toString();

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(USER_NAME_KEY, newName);
        editor.putString(USER_BIO_KEY, newBio);
        editor.apply();

        toggleEditMode();
    }
}