package com.example.f1quizgame.niktza4;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SingleGameActivity extends AppCompatActivity {
    TextView questionTextView;
    Button buttonA, buttonB, buttonC, buttonD;
    private CardView difficultyOverlayCard;
    private TextView difficultyOverlayText;
    DatabaseReference database;
    private ImageView categoryImage;
    private ArrayList<String> questionIds = new ArrayList<>();
    private int currentQuestionIndex = 0;
    View[] senderDots;
    private FrameLayout gameOverOverlay;
    private TextView gameOverMessage;
    private Button returnButton;
    private int correctAnswers = 0;
    Button helper5050;
    boolean used5050 = false;
    private FrameLayout audienceOverlay;
    private LinearLayout audienceLayout;
    private boolean usedAudience = false;
    private Button helperAudience;
    private Button helperEinstein;
    private boolean usedEinstein = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_single_game);

        questionTextView = findViewById(R.id.textView10);
        buttonA = findViewById(R.id.answerA);
        buttonB = findViewById(R.id.answerB);
        buttonC = findViewById(R.id.answerC);
        buttonD = findViewById(R.id.answerD);
        difficultyOverlayCard = findViewById(R.id.difficultyOverlayCard);
        difficultyOverlayText = findViewById(R.id.difficultyOverlayText);
        categoryImage = findViewById(R.id.categoryImage);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);
        gameOverMessage = findViewById(R.id.gameOverMessage);
        returnButton = findViewById(R.id.returnButton);
        helper5050 = findViewById(R.id.helper5050);
        audienceOverlay = findViewById(R.id.audience_overlay);
        audienceLayout = findViewById(R.id.audience_layout);
        helperAudience = findViewById(R.id.helperAudience);
        helperAudience.setOnClickListener(v -> useAskTheAudience());
        helperEinstein = findViewById(R.id.helperEinstein);
        helperEinstein.setOnClickListener(v -> useEinstein());
        database = FirebaseDatabase.getInstance().getReference();





        helper5050.setOnClickListener(v -> use5050());

        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(SingleGameActivity.this, Main.class);
            startActivity(intent);
            finish();
        });
        senderDots = new View[]{
                findViewById(R.id.senderDot1),
                findViewById(R.id.senderDot2),
                findViewById(R.id.senderDot3),
                findViewById(R.id.senderDot4),
                findViewById(R.id.senderDot5),
                findViewById(R.id.senderDot6),
                findViewById(R.id.senderDot7),
                findViewById(R.id.senderDot8),
                findViewById(R.id.senderDot9),
                findViewById(R.id.senderDot10)
        };

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadAllQuestionIds();
    }

    private void loadAllQuestionIds() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Questions3");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    String category = categorySnap.getKey();
                    for (DataSnapshot questionSnap : categorySnap.getChildren()) {
                        questionIds.add(category + ":" + questionSnap.getKey());
                    }
                }
                Collections.shuffle(questionIds);
                loadQuestion(questionIds.get(currentQuestionIndex));
                setAnswerListeners();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SingleGameActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryOverlay(String category) {
        difficultyOverlayText.setText("Category: " + category);
        int imageRes;
        switch (category.toLowerCase()) {
            case "food":
                imageRes = R.drawable.food;
                break;
            case "sports":
                imageRes = R.drawable.sport;
                break;
            case "music":
                imageRes = R.drawable.music;
                break;
            case "celebrities":
                imageRes = R.drawable.people;
                break;
            case "geography":
                imageRes = R.drawable.geography;
                break;
            default:
                imageRes = R.drawable.people;
                break;
        }
        categoryImage.setImageResource(imageRes);
        difficultyOverlayCard.setVisibility(View.VISIBLE);
        difficultyOverlayCard.setAlpha(0f);
        difficultyOverlayCard.animate().alpha(1f).setDuration(500).start();
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                difficultyOverlayCard.animate().alpha(0f).setDuration(500).withEndAction(() ->
                        difficultyOverlayCard.setVisibility(View.GONE)).start(), 1500);
    }

    private void loadQuestion(String categoryAndId) {
        // Reset all answer buttons
        Button[] buttons = {buttonA, buttonB, buttonC, buttonD};
        for (Button btn : buttons) {
            btn.setVisibility(View.VISIBLE);
            btn.setAlpha(1f);
            btn.setScaleX(1f);
            btn.setScaleY(1f);
        }

        String[] parts = categoryAndId.split(":");
        String category = parts[0];
        String questionId = parts[1];

        showCategoryOverlay(category);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Questions3").child(category).child(questionId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    questionTextView.setText(snapshot.child("text").getValue(String.class));
                    buttonA.setText(snapshot.child("a").getValue(String.class));
                    buttonB.setText(snapshot.child("b").getValue(String.class));
                    buttonC.setText(snapshot.child("c").getValue(String.class));
                    buttonD.setText(snapshot.child("d").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SingleGameActivity.this, "Error loading question", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setAnswerListeners() {
        View.OnClickListener listener = v -> {
            Button selectedButton = (Button) v;
            String selectedAnswer = selectedButton.getText().toString();
            String[] parts = questionIds.get(currentQuestionIndex).split(":");
            String category = parts[0];
            String questionId = parts[1];

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Questions3").child(category).child(questionId);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String correctKey = snapshot.child("correct").getValue(String.class);
                    String correctAnswerText = snapshot.child(correctKey).getValue(String.class);
                    boolean correct = selectedAnswer.equals(correctAnswerText);
                    showCorrectAnswerAnimation(() -> {
                        if (correct) {
                            correctAnswers++;
                            updateDots(correctAnswers);
                            if (correctAnswers== 10) {
                                Toast.makeText(SingleGameActivity.this, "You won!", Toast.LENGTH_LONG).show();
                                showGameOverScreen();
                            } else {
                                currentQuestionIndex++;
                                loadQuestion(questionIds.get(currentQuestionIndex));
                            }
                        } else {
                            showGameOverScreen();
                            Toast.makeText(SingleGameActivity.this, "Wrong answer! You lost!", Toast.LENGTH_LONG).show();
                        }
                    }, correctAnswerText);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(SingleGameActivity.this, "Error checking answer", Toast.LENGTH_SHORT).show();
                }
            });
        };

        buttonA.setOnClickListener(listener);
        buttonB.setOnClickListener(listener);
        buttonC.setOnClickListener(listener);
        buttonD.setOnClickListener(listener);
    }

    private void showCorrectAnswerAnimation(Runnable onComplete, String correctText) {
        Button correctButton = null;
        if (buttonA.getText().toString().equals(correctText)) correctButton = buttonA;
        else if (buttonB.getText().toString().equals(correctText)) correctButton = buttonB;
        else if (buttonC.getText().toString().equals(correctText)) correctButton = buttonC;
        else if (buttonD.getText().toString().equals(correctText)) correctButton = buttonD;

        Button finalCorrectButton = correctButton;
        runOnUiThread(() -> {
            int greenColor = ContextCompat.getColor(this, android.R.color.holo_green_dark);
            int originalColor = ContextCompat.getColor(this, R.color.button_background);

            if (buttonA != finalCorrectButton) buttonA.setVisibility(View.INVISIBLE);
            if (buttonB != finalCorrectButton) buttonB.setVisibility(View.INVISIBLE);
            if (buttonC != finalCorrectButton) buttonC.setVisibility(View.INVISIBLE);
            if (buttonD != finalCorrectButton) buttonD.setVisibility(View.INVISIBLE);

            if (finalCorrectButton != null) {
                finalCorrectButton.setBackgroundTintList(ColorStateList.valueOf(greenColor));
            }

            new Handler().postDelayed(() -> {
                buttonA.setBackgroundTintList(ColorStateList.valueOf(originalColor));
                buttonB.setBackgroundTintList(ColorStateList.valueOf(originalColor));
                buttonC.setBackgroundTintList(ColorStateList.valueOf(originalColor));
                buttonD.setBackgroundTintList(ColorStateList.valueOf(originalColor));

                buttonA.setVisibility(View.VISIBLE);
                buttonB.setVisibility(View.VISIBLE);
                buttonC.setVisibility(View.VISIBLE);
                buttonD.setVisibility(View.VISIBLE);

                onComplete.run();
            }, 1500);
        });
    }
    private void updateDots(int correctAnswers) {
        for (int i = 0; i < senderDots.length; i++) {
            if (i < correctAnswers) {
                senderDots[i].setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, android.R.color.holo_green_light)
                ));
            } else {
                senderDots[i].setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, android.R.color.darker_gray)
                ));
            }
        }
    }
    private void showGameOverScreen() {
        gameOverMessage.setText("You got " + correctAnswers + "/10 correct!");
        if(correctAnswers == 10){
            database.child("Statistics").child("survivalWinners").runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Integer currentValue = currentData.getValue(Integer.class);
                    if (currentValue == null) {
                        currentData.setValue(1);
                    } else {
                        currentData.setValue(currentValue + 1);
                    }
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                }
            });

        }
        helper5050.setVisibility(View.INVISIBLE);
        helperAudience.setVisibility(View.INVISIBLE);
        helperEinstein.setVisibility(View.INVISIBLE);
        gameOverOverlay.setVisibility(View.VISIBLE);
        gameOverOverlay.setAlpha(0f);
        gameOverOverlay.animate().alpha(1f).setDuration(2500).start();
    }

    private void use5050() {
        if (used5050) return;

        String[] parts = questionIds.get(currentQuestionIndex).split(":");
        String category = parts[0];
        String questionId = parts[1];

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Questions3").child(category).child(questionId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String correctKey = snapshot.child("correct").getValue(String.class);
                String correctAnswer = snapshot.child(correctKey).getValue(String.class);

                ArrayList<Button> wrongButtons = new ArrayList<>();
                if (!buttonA.getText().toString().equals(correctAnswer)) wrongButtons.add(buttonA);
                if (!buttonB.getText().toString().equals(correctAnswer)) wrongButtons.add(buttonB);
                if (!buttonC.getText().toString().equals(correctAnswer)) wrongButtons.add(buttonC);
                if (!buttonD.getText().toString().equals(correctAnswer)) wrongButtons.add(buttonD);

                Collections.shuffle(wrongButtons);
                animateButtonDisappear(wrongButtons.get(0));
                animateButtonDisappear(wrongButtons.get(1));

                used5050 = true;
                helper5050.setAlpha(0.5f);
                helper5050.setEnabled(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SingleGameActivity.this, "Error using 50/50", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateButtonDisappear(Button button) {
        button.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction(() -> button.setVisibility(View.INVISIBLE))
                .start();
    }

    private void useAskTheAudience() {
        if (usedAudience) return;

        String[] parts = questionIds.get(currentQuestionIndex).split(":");
        String category = parts[0];
        String questionId = parts[1];

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Questions3").child(category).child(questionId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String correctKey = snapshot.child("correct").getValue(String.class);
                String correctAnswer = snapshot.child(correctKey).getValue(String.class);

                int correctPercent = 55 + new Random().nextInt(21); // 55-75%
                int remain = 100 - correctPercent;
                int r1 = new Random().nextInt(remain);
                int r2 = new Random().nextInt(remain - r1);
                int r3 = remain - r1 - r2;

                List<Integer> wrongPercents = new ArrayList<>(Arrays.asList(r1, r2, r3));
                Collections.shuffle(wrongPercents);

                Map<String, Integer> percentages = new HashMap<>();
                List<String> keys = Arrays.asList("a", "b", "c", "d");
                for (String key : keys) {
                    String ans = snapshot.child(key).getValue(String.class);
                    if (ans.equals(correctAnswer)) {
                        percentages.put(key, correctPercent);
                    } else {
                        percentages.put(key, wrongPercents.remove(0));
                    }
                }

                runOnUiThread(() -> {
                    audienceLayout.removeAllViews();
                    audienceOverlay.setVisibility(View.VISIBLE);
                    audienceOverlay.setAlpha(0f);
                    audienceOverlay.animate().alpha(1f).setDuration(500).start();

                    String[] labels = {"A", "B", "C", "D"};
                    for (int i = 0; i < 4; i++) {
                        String key = keys.get(i);
                        int percent = percentages.get(key);

                        TextView label = new TextView(SingleGameActivity.this);
                        label.setText(labels[i] + ": " + percent + "%");
                        label.setTextSize(18f);
                        label.setTextColor(Color.BLACK);
                        label.setPadding(0, 20, 0, 8);

                        ProgressBar bar = new ProgressBar(SingleGameActivity.this, null, android.R.attr.progressBarStyleHorizontal);
                        bar.setMax(100);
                        bar.setProgress(0);
                        bar.setProgressDrawable(ContextCompat.getDrawable(SingleGameActivity.this, android.R.drawable.progress_horizontal));
                        bar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 30));

                        audienceLayout.addView(label);
                        audienceLayout.addView(bar);

                        ObjectAnimator anim = ObjectAnimator.ofInt(bar, "progress", 0, percent);
                        anim.setDuration(700);
                        anim.setInterpolator(new AccelerateDecelerateInterpolator());
                        anim.start();
                    }

                    usedAudience = true;
                    helperAudience.setAlpha(0.5f);
                    helperAudience.setEnabled(false);

                    new Handler().postDelayed(() -> {
                        audienceOverlay.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                            audienceOverlay.setVisibility(View.GONE);
                        }).start();
                    }, 4000);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SingleGameActivity.this, "Error with audience help", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void useEinstein() {
        if (usedEinstein) return;

        String[] parts = questionIds.get(currentQuestionIndex).split(":");
        String category = parts[0];
        String questionId = parts[1];

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Questions3").child(category).child(questionId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String correctKey = snapshot.child("correct").getValue(String.class);
                String correctAnswer = snapshot.child(correctKey).getValue(String.class);

                Button correctButton = null;
                if (buttonA.getText().toString().equals(correctAnswer)) correctButton = buttonA;
                else if (buttonB.getText().toString().equals(correctAnswer)) correctButton = buttonB;
                else if (buttonC.getText().toString().equals(correctAnswer)) correctButton = buttonC;
                else if (buttonD.getText().toString().equals(correctAnswer)) correctButton = buttonD;

                if (correctButton != null) {
                    ObjectAnimator scaleX = ObjectAnimator.ofFloat(correctButton, "scaleX", 1f, 1.2f, 1f);
                    ObjectAnimator scaleY = ObjectAnimator.ofFloat(correctButton, "scaleY", 1f, 1.2f, 1f);
                    @SuppressLint("ObjectAnimatorBinding") ObjectAnimator colorFade = ObjectAnimator.ofArgb(
                            correctButton, "backgroundTint",
                            Color.parseColor("#FFD700"), // Gold glow
                            Color.parseColor("#A9A9A9"), // Back to normal
                            ContextCompat.getColor(SingleGameActivity.this, R.color.button_background)
                    );
                    scaleX.setDuration(600);
                    scaleY.setDuration(600);
                    scaleX.start();
                    scaleY.start();
                    colorFade.setDuration(1000);
                    colorFade.start();
                }

                usedEinstein = true;
                helperEinstein.setAlpha(0.5f);
                helperEinstein.setEnabled(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SingleGameActivity.this, "Error using Einstein", Toast.LENGTH_SHORT).show();
            }
        });
    }






}
