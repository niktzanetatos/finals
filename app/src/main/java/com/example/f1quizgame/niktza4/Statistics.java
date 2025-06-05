package com.example.f1quizgame.niktza4;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Statistics extends AppCompatActivity {

    private TextView totalUsersValue, totalGamesValue, gamesEndedEarlyValue, survivalWinnersValue;
    private TextView easiestCategoryValue, hardestCategoryValue;
    private TextView headerTitle, categoryPerformanceTitle;
    private BarChart barChart;
    private MaterialCardView totalUsersCard, totalGamesCard, gamesEndedEarlyCard, survivalWinnersCard;
    private MaterialCardView easiestCategoryCard, hardestCategoryCard;
    private View legendLayout;

    private Handler animationHandler = new Handler();
    private static final int ANIMATION_DELAY = 150;
    private static final int CARD_ANIMATION_DURATION = 600;
    private static final int NUMBER_COUNTER_DURATION = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initializeViews();
        hideAllViewsInitially();
        fetchStatisticsFromFirebase();
    }

    private void initializeViews() {
        // TextViews
        totalUsersValue = findViewById(R.id.totalUsersValue);
        totalGamesValue = findViewById(R.id.totalGamesValue);
        gamesEndedEarlyValue = findViewById(R.id.gamesEndedEarlyValue);
        survivalWinnersValue = findViewById(R.id.survivalWinnersValue);
        easiestCategoryValue = findViewById(R.id.easiestCategoryValue);
        hardestCategoryValue = findViewById(R.id.hardestCategoryValue);

        // Headers (you'll need to add IDs to these in your XML)
        headerTitle = findViewById(R.id.headerTitle); // Add this ID to your "QUIZ STATISTICS" TextView
        categoryPerformanceTitle = findViewById(R.id.categoryPerformanceTitle); // Add this ID to your category header

        // Chart and legend
        barChart = findViewById(R.id.barChart);
        legendLayout = findViewById(R.id.legendLayout); // Add this ID to your legend LinearLayout

        // Cards (you'll need to add IDs to these in your XML)
        totalUsersCard = findViewById(R.id.totalUsersCard);
        totalGamesCard = findViewById(R.id.totalGamesCard);
        gamesEndedEarlyCard = findViewById(R.id.gamesEndedEarlyCard);
        survivalWinnersCard = findViewById(R.id.survivalWinnersCard);
        easiestCategoryCard = findViewById(R.id.easiestCategoryCard);
        hardestCategoryCard = findViewById(R.id.hardestCategoryCard);
    }

    private void hideAllViewsInitially() {
        // Hide header
        if (headerTitle != null) {
            headerTitle.setAlpha(0f);
            headerTitle.setTranslationY(-50f);
        }

        // Hide stat cards
        View[] statCards = {totalUsersCard, totalGamesCard, gamesEndedEarlyCard, survivalWinnersCard};
        for (View card : statCards) {
            if (card != null) {
                card.setAlpha(0f);
                card.setScaleX(0.8f);
                card.setScaleY(0.8f);
                card.setTranslationY(50f);
            }
        }

        // Hide category cards
        if (easiestCategoryCard != null) {
            easiestCategoryCard.setAlpha(0f);
            easiestCategoryCard.setTranslationX(-100f);
        }
        if (hardestCategoryCard != null) {
            hardestCategoryCard.setAlpha(0f);
            hardestCategoryCard.setTranslationX(100f);
        }

        // Hide chart section
        if (categoryPerformanceTitle != null) {
            categoryPerformanceTitle.setAlpha(0f);
            categoryPerformanceTitle.setScaleX(0.9f);
            categoryPerformanceTitle.setScaleY(0.9f);
        }

        barChart.setAlpha(0f);
        barChart.setTranslationY(100f);

        if (legendLayout != null) {
            legendLayout.setAlpha(0f);
            legendLayout.setTranslationY(50f);
        }
    }

    private void startEntranceAnimations() {
        // Animate header first
        animateHeaderEntrance();

        // Animate stat cards with staggered delay
        animationHandler.postDelayed(() -> animateStatCards(), 300);

        // Animate category cards
        animationHandler.postDelayed(() -> animateCategoryCards(), 800);

        // Animate chart section
        animationHandler.postDelayed(() -> animateChartSection(), 1200);
    }

    private void animateHeaderEntrance() {
        if (headerTitle != null) {
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(headerTitle, "alpha", 0f, 1f);
            ObjectAnimator slideDown = ObjectAnimator.ofFloat(headerTitle, "translationY", -50f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(headerTitle, "scaleX", 0.8f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(headerTitle, "scaleY", 0.8f, 1f);

            AnimatorSet headerSet = new AnimatorSet();
            headerSet.playTogether(fadeIn, slideDown, scaleX, scaleY);
            headerSet.setDuration(800);
            headerSet.setInterpolator(new OvershootInterpolator(1.2f));
            headerSet.start();
        }
    }

    private void animateStatCards() {
        View[] cards = {totalUsersCard, totalGamesCard, gamesEndedEarlyCard, survivalWinnersCard};

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                final View card = cards[i];
                animationHandler.postDelayed(() -> animateCardEntrance(card), i * ANIMATION_DELAY);
            }
        }
    }

    private void animateCardEntrance(View card) {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(card, "translationY", 50f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f);

        AnimatorSet cardSet = new AnimatorSet();
        cardSet.playTogether(fadeIn, slideUp, scaleX, scaleY);
        cardSet.setDuration(CARD_ANIMATION_DURATION);
        cardSet.setInterpolator(new BounceInterpolator());

        // Add subtle rotation for extra flair
        ObjectAnimator rotation = ObjectAnimator.ofFloat(card, "rotation", 0f, 5f, 0f);
        rotation.setDuration(CARD_ANIMATION_DURATION);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet fullSet = new AnimatorSet();
        fullSet.playTogether(cardSet, rotation);
        fullSet.start();
    }

    private void animateCategoryCards() {
        if (easiestCategoryCard != null) {
            ObjectAnimator fadeIn1 = ObjectAnimator.ofFloat(easiestCategoryCard, "alpha", 0f, 1f);
            ObjectAnimator slideRight = ObjectAnimator.ofFloat(easiestCategoryCard, "translationX", -100f, 0f);

            AnimatorSet easiestSet = new AnimatorSet();
            easiestSet.playTogether(fadeIn1, slideRight);
            easiestSet.setDuration(600);
            easiestSet.setInterpolator(new OvershootInterpolator(1.1f));
            easiestSet.start();
        }

        if (hardestCategoryCard != null) {
            animationHandler.postDelayed(() -> {
                ObjectAnimator fadeIn2 = ObjectAnimator.ofFloat(hardestCategoryCard, "alpha", 0f, 1f);
                ObjectAnimator slideLeft = ObjectAnimator.ofFloat(hardestCategoryCard, "translationX", 100f, 0f);

                AnimatorSet hardestSet = new AnimatorSet();
                hardestSet.playTogether(fadeIn2, slideLeft);
                hardestSet.setDuration(600);
                hardestSet.setInterpolator(new OvershootInterpolator(1.1f));
                hardestSet.start();
            }, 200);
        }
    }

    private void animateChartSection() {
        // Animate category performance title
        if (categoryPerformanceTitle != null) {
            ObjectAnimator titleFade = ObjectAnimator.ofFloat(categoryPerformanceTitle, "alpha", 0f, 1f);
            ObjectAnimator titleScaleX = ObjectAnimator.ofFloat(categoryPerformanceTitle, "scaleX", 0.9f, 1f);
            ObjectAnimator titleScaleY = ObjectAnimator.ofFloat(categoryPerformanceTitle, "scaleY", 0.9f, 1f);

            AnimatorSet titleSet = new AnimatorSet();
            titleSet.playTogether(titleFade, titleScaleX, titleScaleY);
            titleSet.setDuration(500);
            titleSet.setInterpolator(new DecelerateInterpolator());
            titleSet.start();
        }

        // Animate chart
        animationHandler.postDelayed(() -> {
            ObjectAnimator chartFade = ObjectAnimator.ofFloat(barChart, "alpha", 0f, 1f);
            ObjectAnimator chartSlide = ObjectAnimator.ofFloat(barChart, "translationY", 100f, 0f);

            AnimatorSet chartSet = new AnimatorSet();
            chartSet.playTogether(chartFade, chartSlide);
            chartSet.setDuration(800);
            chartSet.setInterpolator(new DecelerateInterpolator());
            chartSet.start();
        }, 300);

        // Animate legend
        if (legendLayout != null) {
            animationHandler.postDelayed(() -> {
                ObjectAnimator legendFade = ObjectAnimator.ofFloat(legendLayout, "alpha", 0f, 1f);
                ObjectAnimator legendSlide = ObjectAnimator.ofFloat(legendLayout, "translationY", 50f, 0f);

                AnimatorSet legendSet = new AnimatorSet();
                legendSet.playTogether(legendFade, legendSlide);
                legendSet.setDuration(600);
                legendSet.setInterpolator(new BounceInterpolator());
                legendSet.start();
            }, 600);
        }
    }

    private void animateNumberCounter(TextView textView, long finalValue) {
        ValueAnimator animator = ValueAnimator.ofInt(0, (int) finalValue);
        animator.setDuration(NUMBER_COUNTER_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(animatedValue));
        });


        ObjectAnimator pulseX = ObjectAnimator.ofFloat(textView, "scaleX", 1f, 1.1f, 1f);
        pulseX.setRepeatCount(2);
        pulseX.setRepeatMode(ValueAnimator.RESTART);

        ObjectAnimator pulseY = ObjectAnimator.ofFloat(textView, "scaleY", 1f, 1.1f, 1f);
        pulseY.setRepeatCount(2);
        pulseY.setRepeatMode(ValueAnimator.RESTART);

        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(pulseX, pulseY);
        pulse.setDuration(NUMBER_COUNTER_DURATION / 3);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet numberSet = new AnimatorSet();
        numberSet.playTogether(animator, pulse);
        numberSet.start();
    }

    private void addCardHoverEffects() {
        View[] allCards = {totalUsersCard, totalGamesCard, gamesEndedEarlyCard,
                survivalWinnersCard, easiestCategoryCard, hardestCategoryCard};

        for (View card : allCards) {
            if (card != null) {
                card.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            animateCardPress(v, true);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            animateCardPress(v, false);
                            break;
                    }
                    return false;
                });
            }
        }
    }

    private void animateCardPress(View card, boolean pressed) {
        float scale = pressed ? 0.95f : 1f;
        float elevation = pressed ? 2f : 8f;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", scale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", scale);

        AnimatorSet scaleSet = new AnimatorSet();
        scaleSet.playTogether(scaleX, scaleY);
        scaleSet.setDuration(100);
        scaleSet.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleSet.start();

        if (card instanceof MaterialCardView) {
            ((MaterialCardView) card).setCardElevation(elevation);
        }
    }

    private void fetchStatisticsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Statistics");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Start entrance animations
                    startEntranceAnimations();

                    // Add interactive effects
                    addCardHoverEffects();

                    // Get basic statistics
                    Long userCount = snapshot.child("userCount").getValue(Long.class);
                    Long totalGames = snapshot.child("totalGames").getValue(Long.class);
                    Long gamesEndedEarly = snapshot.child("gamesEndedEarly").getValue(Long.class);
                    Long survivalWinners = snapshot.child("survivalWinners").getValue(Long.class);

                    // Animate numbers with delay to sync with card animations
                    animationHandler.postDelayed(() -> {
                        if (userCount != null) animateNumberCounter(totalUsersValue, userCount);
                        else totalUsersValue.setText("0");
                    }, 1000);

                    animationHandler.postDelayed(() -> {
                        if (totalGames != null) animateNumberCounter(totalGamesValue, totalGames);
                        else totalGamesValue.setText("0");
                    }, 1150);

                    animationHandler.postDelayed(() -> {
                        if (gamesEndedEarly != null) animateNumberCounter(gamesEndedEarlyValue, gamesEndedEarly);
                        else gamesEndedEarlyValue.setText("0");
                    }, 1300);

                    animationHandler.postDelayed(() -> {
                        if (survivalWinners != null) animateNumberCounter(survivalWinnersValue, survivalWinners);
                        else survivalWinnersValue.setText("0");
                    }, 1450);

                    // Process category statistics
                    DataSnapshot categoriesSnapshot = snapshot.child("categories");
                    List<BarEntry> correctEntries = new ArrayList<>();
                    List<BarEntry> wrongEntries = new ArrayList<>();
                    List<String> categoryNames = new ArrayList<>();

                    float maxRatio = -1f;
                    float minRatio = 1f;
                    String easiest = "-";
                    String hardest = "-";

                    int index = 0;
                    for (DataSnapshot category : categoriesSnapshot.getChildren()) {
                        String name = category.getKey();
                        Long correctLong = category.child("correct").getValue(Long.class);
                        Long wrongLong = category.child("wrong").getValue(Long.class);

                        long correct = correctLong != null ? correctLong : 0;
                        long wrong = wrongLong != null ? wrongLong : 0;
                        long total = correct + wrong;

                        categoryNames.add(name);
                        correctEntries.add(new BarEntry(index, correct));
                        wrongEntries.add(new BarEntry(index, wrong));
                        index++;

                        if (total > 0) {
                            float ratio = (float) correct / total;

                            if (ratio > maxRatio) {
                                maxRatio = ratio;
                                easiest = name;
                            }
                            if (ratio < minRatio) {
                                minRatio = ratio;
                                hardest = name;
                            }
                        }
                    }

                    // Animate category results
                    final String finalEasiest = easiest;
                    final String finalHardest = hardest;

                    animationHandler.postDelayed(() -> {
                        animateTextChange(easiestCategoryValue, finalEasiest);
                    }, 1600);

                    animationHandler.postDelayed(() -> {
                        animateTextChange(hardestCategoryValue, finalHardest);
                    }, 1800);

                    setupEnhancedBarChart(correctEntries, wrongEntries, categoryNames);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Statistics", "Firebase load failed.", error.toException());
            }
        });
    }

    private void animateTextChange(TextView textView, String newText) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
        fadeOut.setDuration(200);

        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                textView.setText(newText);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 0.8f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 0.8f, 1f);

                AnimatorSet fadeInSet = new AnimatorSet();
                fadeInSet.playTogether(fadeIn, scaleX, scaleY);
                fadeInSet.setDuration(300);
                fadeInSet.setInterpolator(new BounceInterpolator());
                fadeInSet.start();
            }
        });

        fadeOut.start();
    }

    private void setupEnhancedBarChart(List<BarEntry> correctEntries, List<BarEntry> wrongEntries, List<String> categoryNames) {
        BarDataSet correctDataSet = new BarDataSet(correctEntries, "Correct");
        correctDataSet.setColor(Color.parseColor("#FF4CAF50"));
        correctDataSet.setValueTextColor(Color.WHITE);
        correctDataSet.setValueTextSize(10f);

        BarDataSet wrongDataSet = new BarDataSet(wrongEntries, "Incorrect");
        wrongDataSet.setColor(Color.parseColor("#FFF44336"));
        wrongDataSet.setValueTextColor(Color.WHITE);
        wrongDataSet.setValueTextSize(10f);

        BarData barData = new BarData(correctDataSet, wrongDataSet);
        barData.setBarWidth(0.4f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(categoryNames));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(categoryNames.size());
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextColor(Color.WHITE);

        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(true);
        barChart.getLegend().setTextColor(Color.WHITE);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);

        barChart.setData(barData);

        if (correctEntries.size() > 0) {
            float groupSpace = 0.4f;
            float barSpace = 0.05f;
            float barWidth = 0.25f;
            barData.setBarWidth(barWidth);
            barChart.groupBars(0f, groupSpace, barSpace);
        }

        // Delay chart animation to sync with entrance animations
        animationHandler.postDelayed(() -> {
            barChart.animateY(1200);
            barChart.invalidate();
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }
}