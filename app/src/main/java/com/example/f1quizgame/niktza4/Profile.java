package com.example.f1quizgame.niktza4;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Profile extends AppCompatActivity {

    String userId;
    DatabaseReference database;
    FirebaseAuth auth;

    TextView textViewDisplayName, textViewLevel, statsView, dateView;
    EditText editDisplayName;
    Button buttonSave;
    CardView profileCard, statsCard, editCard;
    ImageView avatarImage, trophyImage;
    ProgressBar levelProgress;

    private int totalWins = 0, totalDraws = 0, totalLosses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        initializeViews();
        setupInitialAnimations();
        auth = FirebaseAuth.getInstance();

        // Get userId from intent
        Intent intent = getIntent();
        userId = intent.getStringExtra("id");
        if (!auth.getCurrentUser().getUid().equals(userId)) {
            buttonSave.setVisibility(View.INVISIBLE);
        }

        database = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Load data with animation
        loadUserData();
        loadMatchStats(userId);

        // Setup button click listener with animation
        buttonSave.setOnClickListener(v -> {
            animateButtonClick(v);
            saveDisplayName();
        });
    }

    private void initializeViews() {
        textViewDisplayName = findViewById(R.id.textViewDisplayName);
        textViewLevel = findViewById(R.id.textViewLevel);
        editDisplayName = findViewById(R.id.editDisplayName);
        buttonSave = findViewById(R.id.buttonSave);
        statsView = findViewById(R.id.statsView);
        profileCard = findViewById(R.id.profileCard);
        statsCard = findViewById(R.id.statsCard);
        editCard = findViewById(R.id.editCard);
        avatarImage = findViewById(R.id.avatarImage);
        levelProgress = findViewById(R.id.levelProgress);
        dateView = findViewById(R.id.textViewMemberDate);
    }

    private void setupInitialAnimations() {
        // Hide views initially for entrance animation
        profileCard.setAlpha(0f);
        profileCard.setTranslationY(-100f);

        statsCard.setAlpha(0f);
        statsCard.setTranslationX(-100f);

        editCard.setAlpha(0f);
        editCard.setTranslationY(100f);

        // Start entrance animations
        new Handler().postDelayed(this::startEntranceAnimations, 200);
    }

    private void startEntranceAnimations() {
        // Profile card animation
        ObjectAnimator profileAlpha = ObjectAnimator.ofFloat(profileCard, "alpha", 0f, 1f);
        ObjectAnimator profileTransY = ObjectAnimator.ofFloat(profileCard, "translationY", -100f, 0f);
        AnimatorSet profileSet = new AnimatorSet();
        profileSet.playTogether(profileAlpha, profileTransY);
        profileSet.setDuration(600);
        profileSet.setInterpolator(new DecelerateInterpolator());
        profileSet.start();

        // Stats card animation (delayed)
        new Handler().postDelayed(() -> {
            ObjectAnimator statsAlpha = ObjectAnimator.ofFloat(statsCard, "alpha", 0f, 1f);
            ObjectAnimator statsTransX = ObjectAnimator.ofFloat(statsCard, "translationX", -100f, 0f);
            AnimatorSet statsSet = new AnimatorSet();
            statsSet.playTogether(statsAlpha, statsTransX);
            statsSet.setDuration(600);
            statsSet.setInterpolator(new DecelerateInterpolator());
            statsSet.start();
        }, 200);

        // Edit card animation (delayed)
        new Handler().postDelayed(() -> {
            ObjectAnimator editAlpha = ObjectAnimator.ofFloat(editCard, "alpha", 0f, 1f);
            ObjectAnimator editTransY = ObjectAnimator.ofFloat(editCard, "translationY", 100f, 0f);
            AnimatorSet editSet = new AnimatorSet();
            editSet.playTogether(editAlpha, editTransY);
            editSet.setDuration(600);
            editSet.setInterpolator(new DecelerateInterpolator());
            editSet.start();
        }, 400);

    }


    private void animateButtonClick(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(scaleDownX, scaleDownY);
        scaleDown.setDuration(100);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(scaleUpX, scaleUpY);
        scaleUp.setDuration(100);

        AnimatorSet complete = new AnimatorSet();
        complete.playSequentially(scaleDown, scaleUp);
        complete.start();
    }

    private void loadUserData() {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String displayName = snapshot.child("displayName").getValue(String.class);
                    Long level = snapshot.child("level").getValue(Long.class);
                    String memberDate = snapshot.child("date").getValue(String.class);

                    if (displayName != null) {
                        animateTextChange(textViewDisplayName, displayName);
                        editDisplayName.setText(displayName);
                    }

                    if (level != null) {
                        animateTextChange(textViewLevel, "Level " + level);
                        animateLevelProgress(level.intValue());
                    }
                    if (memberDate != null) {
                        dateView.setText(memberDate);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Profile.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateTextChange(TextView textView, String newText) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
        fadeOut.setDuration(200);
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                textView.setText(newText);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    private void animateLevelProgress(int level) {
        int progress = Math.min(level * 10, 100); // Assuming max level 10
        ValueAnimator progressAnimator = ValueAnimator.ofInt(0, progress);
        progressAnimator.setDuration(1000);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            levelProgress.setProgress(animatedValue);
        });
        progressAnimator.start();
    }

    private void loadMatchStats(String userId) {
        DatabaseReference matchHistoryRef = FirebaseDatabase.getInstance().getReference("MatchHistory");

        matchHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int wins = 0;
                int draws = 0;
                int losses = 0;

                for (DataSnapshot matchSnapshot : snapshot.getChildren()) {
                    String senderId = matchSnapshot.child("senderId").getValue(String.class);
                    String receiverId = matchSnapshot.child("receiverId").getValue(String.class);
                    Long senderScore = matchSnapshot.child("senderScore").getValue(Long.class);
                    Long receiverScore = matchSnapshot.child("receiverScore").getValue(Long.class);

                    if (senderId == null || receiverId == null || senderScore == null || receiverScore == null)
                        continue;

                    boolean isSender = userId.equals(senderId);
                    boolean isReceiver = userId.equals(receiverId);

                    if (isSender || isReceiver) {
                        long myScore = isSender ? senderScore : receiverScore;
                        long opponentScore = isSender ? receiverScore : senderScore;

                        if (myScore > opponentScore) {
                            wins++;
                        } else if (myScore < opponentScore) {
                            losses++;
                        } else {
                            draws++;
                        }
                    }
                }

                totalWins = wins;
                totalDraws = draws;
                totalLosses = losses;
                animateStatsDisplay(wins, draws, losses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to load stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateStatsDisplay(int wins, int draws, int losses) {
        // Animate numbers counting up
        ValueAnimator winsAnimator = ValueAnimator.ofInt(0, wins);
        ValueAnimator drawsAnimator = ValueAnimator.ofInt(0, draws);
        ValueAnimator lossesAnimator = ValueAnimator.ofInt(0, losses);

        winsAnimator.setDuration(1500);
        drawsAnimator.setDuration(1500);
        lossesAnimator.setDuration(1500);

        winsAnimator.setInterpolator(new DecelerateInterpolator());
        drawsAnimator.setInterpolator(new DecelerateInterpolator());
        lossesAnimator.setInterpolator(new DecelerateInterpolator());

        winsAnimator.addUpdateListener(animation -> updateStatsDisplay((int) animation.getAnimatedValue(), totalDraws, totalLosses));
        drawsAnimator.addUpdateListener(animation -> updateStatsDisplay(totalWins, (int) animation.getAnimatedValue(), totalLosses));
        lossesAnimator.addUpdateListener(animation -> updateStatsDisplay(totalWins, totalDraws, (int) animation.getAnimatedValue()));

        // Start animations with slight delays
        winsAnimator.start();
        new Handler().postDelayed(drawsAnimator::start, 200);
        new Handler().postDelayed(lossesAnimator::start, 400);
    }

    private void updateStatsDisplay(int wins, int draws, int losses) {
        String winsText = "🏆 " + wins + "  ";
        String drawsText = "🤝 " + draws + "  ";
        String lossesText = "💔 " + losses;

        SpannableString spannable = new SpannableString(winsText + drawsText + lossesText);

        // Green for wins
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                0, winsText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Orange for draws
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#FFA500")),
                winsText.length(), winsText.length() + drawsText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Red for losses
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#F44336")),
                winsText.length() + drawsText.length(),
                winsText.length() + drawsText.length() + lossesText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        statsView.setText(spannable);
    }

    private void saveDisplayName() {
        String newName = editDisplayName.getText().toString().trim();
        if (!newName.isEmpty()) {
            // Add loading animation
            buttonSave.setEnabled(false);
            buttonSave.setText("Saving...");

            database.child("displayName").setValue(newName).addOnSuccessListener(aVoid -> {
                animateTextChange(textViewDisplayName, newName);
                Toast.makeText(Profile.this, "✅ Username updated successfully!", Toast.LENGTH_SHORT).show();

                buttonSave.setEnabled(true);
                buttonSave.setText("Save");

                // Success animation
                ObjectAnimator pulse = ObjectAnimator.ofFloat(textViewDisplayName, "scaleX", 1f, 1.1f, 1f);
                ObjectAnimator pulseY = ObjectAnimator.ofFloat(textViewDisplayName, "scaleY", 1f, 1.1f, 1f);
                AnimatorSet pulseSet = new AnimatorSet();
                pulseSet.playTogether(pulse, pulseY);
                pulseSet.setDuration(500);
                pulseSet.start();

            }).addOnFailureListener(e -> {
                Toast.makeText(Profile.this, "❌ Update failed", Toast.LENGTH_SHORT).show();
                buttonSave.setEnabled(true);
                buttonSave.setText("Save");
            });
        } else {
            Toast.makeText(Profile.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            // Shake animation for empty input
            ObjectAnimator shake = ObjectAnimator.ofFloat(editDisplayName, "translationX", 0f, -25f, 25f, -25f, 25f, 0f);
            shake.setDuration(500);
            shake.start();
        }
    }
}