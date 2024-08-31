package si.uni_lj.fe.tnuv;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        Button btnBack = findViewById(R.id.buttonBack);

        btnBack.setOnClickListener(v -> {
            // Handle android button click
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(intent);
        });


        ImageView card1Icon = findViewById(R.id.card1).findViewById(R.id.card_icon);
        card1Icon.setImageResource(R.mipmap.crying); // Replace with your own icon

        // Change the icon for the second card
        ImageView card2Icon = findViewById(R.id.card2).findViewById(R.id.card_icon);
        card2Icon.setImageResource(R.mipmap.laughter); // Replace with your own icon

        ImageView card3Icon = findViewById(R.id.card3).findViewById(R.id.card_icon);
        card3Icon.setImageResource(R.mipmap.music); // Replace with your own icon

        ImageView card4Icon = findViewById(R.id.card4).findViewById(R.id.card_icon);
        card4Icon.setImageResource(R.mipmap.drums); // Replace with your own icon

        ImageView card5Icon = findViewById(R.id.card5).findViewById(R.id.card_icon);
        card5Icon.setImageResource(R.mipmap.plane); // Replace with your own icon
    }
}