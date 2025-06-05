package com.example.f1quizgame.niktza4;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Leaderboard extends AppCompatActivity {

    private LinearLayout leaderboardLayout;
    private LinearLayout loadingContainer;
    private DatabaseReference database;
    private String currentUserId;
    private Handler animationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        initializeViews();
        startBackgroundAnimations();
        loadLeaderboard();
    }

    private void initializeViews() {
        leaderboardLayout = findViewById(R.id.leaderboardLayout);
        loadingContainer = findViewById(R.id.loading_container);
        database = FirebaseDatabase.getInstance().getReference("users");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        animationHandler = new Handler(Looper.getMainLooper());
    }

    private void startBackgroundAnimations() {
        // Animate decorative elements
        ImageView decorTopRight = findViewById(R.id.decor_top_right);
        ImageView decorMiddleLeft = findViewById(R.id.decor_middle_left);
        ImageView decorBottomRight = findViewById(R.id.decor_bottom_right);

        startFloatingAnimation(decorTopRight, 3000, 20f);
        startFloatingAnimation(decorMiddleLeft, 4000, 15f);
        startFloatingAnimation(decorBottomRight, 3500, 25f);

        // Animate header elements
        animateHeaderElements();
    }

    private void startFloatingAnimation(View view, long duration, float distance) {
        ObjectAnimator floatUp = ObjectAnimator.ofFloat(view, "translationY", 0f, -distance);
        ObjectAnimator floatDown = ObjectAnimator.ofFloat(view, "translationY", -distance, 0f);

        floatUp.setDuration(duration);
        floatDown.setDuration(duration);
        floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
        floatDown.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet floatingSet = new AnimatorSet();
        floatingSet.playSequentially(floatUp, floatDown);
        floatingSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startFloatingAnimation(view, duration, distance);
            }
        });
        floatingSet.start();
    }

    private void animateHeaderElements() {
        ImageView trophyIcon = findViewById(R.id.trophy_icon);
        TextView headerTitle = findViewById(R.id.header_title);
        TextView headerSubtitle = findViewById(R.id.header_subtitle);

        // Trophy bounce animation
        trophyIcon.setScaleX(0f);
        trophyIcon.setScaleY(0f);
        trophyIcon.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new BounceInterpolator())
                .setStartDelay(200)
                .start();

        // Title slide in from top
        headerTitle.setTranslationY(-100f);
        headerTitle.setAlpha(0f);
        headerTitle.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(400)
                .start();

        // Subtitle fade in
        headerSubtitle.setAlpha(0f);
        headerSubtitle.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(800)
                .start();
    }

    private void loadLeaderboard() {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Triple> userList = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    String name = userSnapshot.child("displayName").getValue(String.class);
                    Long levelLong = userSnapshot.child("level").getValue(Long.class);

                    if (name != null && levelLong != null) {
                        int level = levelLong.intValue();
                        userList.add(new Triple(uid, name, level));
                    }
                }

                Collections.sort(userList, (a, b) -> Integer.compare(b.level, a.level));
                displayLeaderboard(userList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingAndShowError();
            }
        });
    }

    private void displayLeaderboard(List<Triple> userList) {
        // Hide loading with fade out
        loadingContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadingContainer.setVisibility(View.GONE);
                        populateLeaderboard(userList);
                    }
                })
                .start();
    }

    private void populateLeaderboard(List<Triple> userList) {
        if (userList.isEmpty()) {
            showEmptyState();
            return;
        }

        int rank = 1;
        for (Triple user : userList) {
            MaterialCardView card = createUserCard(user, rank);
            leaderboardLayout.addView(card);

            // Animate card entrance
            animateCardEntrance(card, rank - 1);
            rank++;
        }
    }

    private MaterialCardView createUserCard(Triple user, int rank) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        card.setRadius(20);
        card.setCardElevation(8);

        // Create card content
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setPadding(20, 24, 20, 24);
        cardLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Rank badge with special styling for top 3
        TextView rankView = createRankBadge(rank);

        // User info section
        LinearLayout userInfoLayout = new LinearLayout(this);
        userInfoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams userInfoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        userInfoParams.setMargins(16, 0, 0, 0);
        userInfoLayout.setLayoutParams(userInfoParams);

        TextView nameText = new TextView(this);
        nameText.setText(user.name);
        nameText.setTextSize(18);
        nameText.setTypeface(null, Typeface.BOLD);

        TextView levelText = new TextView(this);
        levelText.setText("Level " + user.level);
        levelText.setTextSize(14);
        levelText.setAlpha(0.8f);

        userInfoLayout.addView(nameText);
        userInfoLayout.addView(levelText);

        // Level progress indicator
        View progressIndicator = createLevelProgressIndicator(user.level);

        // Apply styling based on rank
        applyRankStyling(card, rankView, nameText, levelText, rank, user.uid);

        cardLayout.addView(rankView);
        cardLayout.addView(userInfoLayout);
        cardLayout.addView(progressIndicator);
        card.addView(cardLayout);

        // Add click animation
        addCardClickAnimation(card);

        return card;
    }

    private TextView createRankBadge(int rank) {
        TextView rankView = new TextView(this);
        LinearLayout.LayoutParams rankParams = new LinearLayout.LayoutParams(56, 56);
        rankView.setLayoutParams(rankParams);
        rankView.setText(String.valueOf(rank));
        rankView.setTextSize(18);
        rankView.setGravity(Gravity.CENTER);
        rankView.setTypeface(null, Typeface.BOLD);

        // Create circular background
        rankView.setBackground(ContextCompat.getDrawable(this, R.drawable.rank_badge));

        return rankView;
    }

    private View createLevelProgressIndicator(int level) {
        View progressBar = new View(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(4, 40);
        progressParams.setMargins(16, 0, 0, 0);
        progressBar.setLayoutParams(progressParams);

        // Calculate progress based on level (assuming max level around 100)
        float progress = Math.min(level / 100f, 1f);
        int color = Color.rgb(
                (int) (255 * (1 - progress)),
                (int) (255 * progress),
                50
        );
        progressBar.setBackgroundColor(color);
        progressBar.setAlpha(0.7f);

        return progressBar;
    }

    private void applyRankStyling(MaterialCardView card, TextView rankView, TextView nameText,
                                  TextView levelText, int rank, String uid) {
        if (rank == 1) {
            // Gold styling for 1st place
            rankView.setTextColor(Color.parseColor("#FFD700"));
            rankView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33FFD700")));
            nameText.setTextColor(Color.parseColor("#FFD700"));
            levelText.setTextColor(Color.parseColor("#CCFFD700"));
            card.setStrokeWidth(3);
            card.setStrokeColor(Color.parseColor("#FFD700"));
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1AFFD700")));
        } else if (rank == 2) {
            // Silver styling for 2nd place
            rankView.setTextColor(Color.parseColor("#C0C0C0"));
            rankView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33C0C0C0")));
            nameText.setTextColor(Color.parseColor("#C0C0C0"));
            levelText.setTextColor(Color.parseColor("#99C0C0C0"));
            card.setStrokeWidth(2);
            card.setStrokeColor(Color.parseColor("#C0C0C0"));
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1AC0C0C0")));
        } else if (rank == 3) {
            // Bronze styling for 3rd place
            rankView.setTextColor(Color.parseColor("#CD7F32"));
            rankView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33CD7F32")));
            nameText.setTextColor(Color.parseColor("#CD7F32"));
            levelText.setTextColor(Color.parseColor("#99CD7F32"));
            card.setStrokeWidth(2);
            card.setStrokeColor(Color.parseColor("#CD7F32"));
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1ACD7F32")));
        } else {
            // Regular styling
            rankView.setTextColor(Color.WHITE);
            rankView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4D808080")));
            nameText.setTextColor(Color.WHITE);
            levelText.setTextColor(Color.parseColor("#CCFFFFFF"));
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")));
        }

        // Highlight current user
        if (uid.equals(currentUserId)) {
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3344B7E5")));
            nameText.setTypeface(null, Typeface.BOLD_ITALIC);
            nameText.setText(nameText.getText() + " (You)");

            // Add pulsing animation for current user
            addPulsingAnimation(card);
        }
    }

    private void addCardClickAnimation(MaterialCardView card) {
        card.setOnClickListener(v -> {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.95f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.95f, 1f);
            @SuppressLint("ObjectAnimatorBinding") ObjectAnimator elevation = ObjectAnimator.ofFloat(v, "cardElevation", 8f, 16f, 8f);

            AnimatorSet clickAnimation = new AnimatorSet();
            clickAnimation.playTogether(scaleX, scaleY, elevation);
            clickAnimation.setDuration(200);
            clickAnimation.setInterpolator(new OvershootInterpolator());
            clickAnimation.start();
        });
    }

    private void addPulsingAnimation(View view) {
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();
    }

    private void animateCardEntrance(View card, int position) {
        // Start from bottom and slide up
        card.setTranslationY(100f);
        card.setAlpha(0f);
        card.setScaleX(0.8f);
        card.setScaleY(0.8f);

        card.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(100 * position)
                .start();
    }

    private void showEmptyState() {
        loadingContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadingContainer.setVisibility(View.GONE);

                        TextView emptyText = new TextView(Leaderboard.this);
                        emptyText.setText("No users found.\nBe the first to join!");
                        emptyText.setTextColor(Color.parseColor("#AAFFFFFF"));
                        emptyText.setTextSize(16);
                        emptyText.setGravity(Gravity.CENTER);
                        emptyText.setAlpha(0f);
                        leaderboardLayout.addView(emptyText);

                        emptyText.animate()
                                .alpha(1f)
                                .setDuration(500)
                                .start();
                    }
                })
                .start();
    }

    private void hideLoadingAndShowError() {
        loadingContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadingContainer.setVisibility(View.GONE);

                        TextView errorText = new TextView(Leaderboard.this);
                        errorText.setText("⚠️ Error loading leaderboard\nTap to retry");
                        errorText.setTextColor(Color.parseColor("#FFCC4444"));
                        errorText.setTextSize(16);
                        errorText.setGravity(Gravity.CENTER);
                        errorText.setAlpha(0f);
                        errorText.setOnClickListener(v -> {
                            leaderboardLayout.removeAllViews();
                            loadingContainer.setVisibility(View.VISIBLE);
                            loadingContainer.setAlpha(1f);
                            loadLeaderboard();
                        });
                        leaderboardLayout.addView(errorText);

                        errorText.animate()
                                .alpha(1f)
                                .setDuration(500)
                                .start();
                    }
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }

    private static class Triple {
        String uid;
        String name;
        int level;

        Triple(String uid, String name, int level) {
            this.uid = uid;
            this.name = name;
            this.level = level;
        }
    }
}