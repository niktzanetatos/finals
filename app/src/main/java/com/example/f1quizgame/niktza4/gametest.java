package com.example.f1quizgame.niktza4;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class gametest extends AppCompatActivity {

    DatabaseReference gameRef;
    DatabaseReference questionRef;
    DatabaseReference database;
    FirebaseAuth auth;
    String userId, senderId, receiverId, gameId;
    TextView questionTextView, senderTextView, receiverTextView;
    Button buttonA, buttonB, buttonC, buttonD;
    List<String> questionIds = new ArrayList<>();


    int currentQuestionIndex = 0, score1 = 0, score2 = 0;
    int prevScore1 = 0;
    int prevScore2 = 0;
    View[] senderDots;
    View[] receiverDots;
    String currentDifficulty;
    private CardView difficultyOverlayCard;
    private TextView difficultyOverlayText;
    private ProgressBar questionTimer;
    private TextView timerText;
    private CountDownTimer questionCountdownTimer;
    private static final long QUESTION_TIMER_MILLIS = 10000; // 10 seconds
    private boolean timerRunning;


    {
        timerRunning = false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            setContentView(R.layout.activity_game);
            difficultyOverlayCard = findViewById(R.id.difficultyOverlayCard);
            difficultyOverlayText = findViewById(R.id.difficultyOverlayText);
            questionTimer = findViewById(R.id.questionTimer);
            timerText = findViewById(R.id.timerText);


            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main4), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            questionTextView = findViewById(R.id.textView10);
            buttonA = findViewById(R.id.answerA);
            database = FirebaseDatabase.getInstance().getReference();
            buttonB = findViewById(R.id.answerB);
            buttonC = findViewById(R.id.answerC);
            buttonD = findViewById(R.id.answerD);
            senderTextView = findViewById(R.id.textView6);
            receiverTextView = findViewById(R.id.textView8);

            senderId = getIntent().getStringExtra("senderId");
            receiverId = getIntent().getStringExtra("receiverId");
            gameId = getIntent().getStringExtra("gameId");

            loadSenderAndReceiverData();

            gameRef = FirebaseDatabase.getInstance().getReference("games").child(gameId);
            questionRef = FirebaseDatabase.getInstance().getReference("Questions2");


            monitorJoinStatus();
            loadGameData();
            setAnswerListeners();
            monitorAnswerStatus();
            startDisconnectionListener();

            senderDots = new View[]{
                    findViewById(R.id.senderDot1),
                    findViewById(R.id.senderDot2),
                    findViewById(R.id.senderDot3),
                    findViewById(R.id.senderDot4),
                    findViewById(R.id.senderDot5),
                    findViewById(R.id.senderDot6)
            };

            receiverDots = new View[]{
                    findViewById(R.id.receiverDot1),
                    findViewById(R.id.receiverDot2),
                    findViewById(R.id.receiverDot3),
                    findViewById(R.id.receiverDot4),
                    findViewById(R.id.receiverDot5),
                    findViewById(R.id.receiverDot6)
            };
        }
    }

    public void loadSenderAndReceiverData() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
        databaseReference.child(senderId).child("displayName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String senderDisplayName = dataSnapshot.exists() ? dataSnapshot.getValue(String.class) : "Unknown Sender";
                senderTextView.setText(senderDisplayName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", databaseError.getMessage());
                senderTextView.setText("Error Fetching Sender");
            }
        });

        databaseReference.child(receiverId).child("displayName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String receiverDisplayName = dataSnapshot.exists() ? dataSnapshot.getValue(String.class) : "Unknown Receiver";
                receiverTextView.setText(receiverDisplayName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", databaseError.getMessage());
                receiverTextView.setText("Error Fetching Receiver");
            }
        });
    }

    public void loadGameData() {
        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (int i = 1; i <= 6; i++) {
                        String fullId = String.valueOf(snapshot.child("q" + i).getValue());
                        if (fullId != null && !fullId.equals("null")) {
                            questionIds.add(fullId);
                        }
                    }
                    currentQuestionIndex = 0;
                    loadQuestion(questionIds.get(currentQuestionIndex));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gametest.this, "Failed to load game data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadQuestion(String categoryAndId) {
        if (categoryAndId == null || !categoryAndId.contains(":")) {
            Toast.makeText(gametest.this, "Invalid question format.", Toast.LENGTH_SHORT).show();
            return;
        }
        stopQuestionTimer();

        // Reset timer appearance
        resetTimerAppearance();

        String[] parts = categoryAndId.split(":");
        String category = parts[0];
        String questionId = parts[1];

        showCategoryOverlay(category);
        startQuestionTimer(); // Start the timer for this question

        DatabaseReference questionRef = FirebaseDatabase.getInstance().getReference("Questions3")
                .child(category).child(questionId);

        questionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String text = String.valueOf(snapshot.child("text").getValue());
                    String a = String.valueOf(snapshot.child("a").getValue());
                    String b = String.valueOf(snapshot.child("b").getValue());
                    String c = String.valueOf(snapshot.child("c").getValue());
                    String d = String.valueOf(snapshot.child("d").getValue());
                    questionTextView.setText(text);
                    buttonA.setText(a);
                    buttonB.setText(b);
                    buttonC.setText(c);
                    buttonD.setText(d);
                    gameRef.child("senderHasAnswered").setValue(false);
                    gameRef.child("receiverHasAnswered").setValue(false);
                    gameRef.child("senderAnsweredCorrectly").setValue(false);
                    gameRef.child("receiverAnsweredCorrectly").setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gametest.this, "Failed to load question: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showCategoryOverlay(String category) {
        TextView overlayText = findViewById(R.id.difficultyOverlayText);
        ImageView categoryImage = findViewById(R.id.categoryImage);
        CardView overlayCard = findViewById(R.id.difficultyOverlayCard);

        overlayText.setText("Category: " + category);

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

        overlayCard.setVisibility(View.VISIBLE);
        overlayCard.setAlpha(0f);
        overlayCard.animate()
                .alpha(1f)
                .setDuration(500)
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            overlayCard.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction(() -> overlayCard.setVisibility(View.GONE))
                    .start();
        }, 2000);
    }


    public void setAnswerListeners() {
        View.OnClickListener answerListener = v -> {
            Button selectedButton = (Button) v;
            String selectedAnswer = selectedButton.getText().toString();
            stopQuestionTimer();

            String fullId = questionIds.get(currentQuestionIndex);
            if (fullId == null || !fullId.contains(":")) {
                Toast.makeText(gametest.this, "Invalid question format.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] parts = fullId.split(":");
            String category = parts[0];  // This must match exactly with Firebase category keys
            String questionId = parts[1];

            DatabaseReference questionRef = FirebaseDatabase.getInstance()
                    .getReference("Questions3").child(category).child(questionId);

            questionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String correctAnswer = String.valueOf(snapshot.child("correct").getValue());
                        String correctAnswerText = String.valueOf(snapshot.child(correctAnswer).getValue());
                        boolean isCorrect = selectedAnswer.equals(correctAnswerText);

                        // 🔹 Update category statistics (use category as-is)
                        DatabaseReference statsRef = FirebaseDatabase.getInstance()
                                .getReference("Statistics")
                                .child("categories")
                                .child(category) // no .toLowerCase()
                                .child(isCorrect ? "correct" : "wrong");

                        statsRef.runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                Integer currentCount = currentData.getValue(Integer.class);
                                if (currentCount == null) {
                                    currentData.setValue(1);
                                } else {
                                    currentData.setValue(currentCount + 1);
                                }
                                return Transaction.success(currentData);
                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                                   @Nullable DataSnapshot currentData) {
                                if (error != null) {
                                    Log.e("StatsUpdate", "Failed to update stats: " + error.getMessage());
                                }
                            }
                        });

                        // 🔹 Update player-specific game state
                        if (userId.equals(senderId)) {
                            gameRef.child("senderAnsweredCorrectly").setValue(isCorrect);
                            if (isCorrect) {
                                gameRef.child("score1").runTransaction(new Transaction.Handler() {
                                    @NonNull
                                    @Override
                                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                        int currentScore = currentData.getValue(Integer.class) != null ?
                                                currentData.getValue(Integer.class) : 0;
                                        currentData.setValue(currentScore + 10);
                                        return Transaction.success(currentData);
                                    }

                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, boolean committed,
                                                           @Nullable DataSnapshot currentData) {
                                        if (!committed) {
                                            Log.e("ScoreUpdate", "Failed to update sender score");
                                        }
                                    }
                                });
                            }
                            gameRef.child("senderHasAnswered").setValue(true);
                        } else if (userId.equals(receiverId)) {
                            gameRef.child("receiverAnsweredCorrectly").setValue(isCorrect);
                            if (isCorrect) {
                                gameRef.child("score2").runTransaction(new Transaction.Handler() {
                                    @NonNull
                                    @Override
                                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                        int currentScore = currentData.getValue(Integer.class) != null ?
                                                currentData.getValue(Integer.class) : 0;
                                        currentData.setValue(currentScore + 10);
                                        return Transaction.success(currentData);
                                    }

                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, boolean committed,
                                                           @Nullable DataSnapshot currentData) {
                                        if (!committed) {
                                            Log.e("ScoreUpdate", "Failed to update receiver score");
                                        }
                                    }
                                });
                            }
                            gameRef.child("receiverHasAnswered").setValue(true);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(gametest.this, "Failed to load correct answer: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        };

        buttonA.setOnClickListener(answerListener);
        buttonB.setOnClickListener(answerListener);
        buttonC.setOnClickListener(answerListener);
        buttonD.setOnClickListener(answerListener);
    }




    public void monitorAnswerStatus() {
        gameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean senderHasAnswered = snapshot.child("senderHasAnswered").getValue(Boolean.class) != null
                            && snapshot.child("senderHasAnswered").getValue(Boolean.class);
                    boolean receiverHasAnswered = snapshot.child("receiverHasAnswered").getValue(Boolean.class) != null
                            && snapshot.child("receiverHasAnswered").getValue(Boolean.class);

                    if (senderHasAnswered && receiverHasAnswered) {
                        Integer score1 = snapshot.child("score1").getValue(Integer.class);
                        Integer score2 = snapshot.child("score2").getValue(Integer.class);

                        if (score1 != null && score2 != null) {
                            boolean senderCorrect = score1 > prevScore1;
                            boolean receiverCorrect = score2 > prevScore2;
                            showCorrectAnswerAnimation(() -> updateDots(currentQuestionIndex, senderCorrect, receiverCorrect));
                            prevScore1 = score1;
                            prevScore2 = score2;
                        }
                        new android.os.Handler().postDelayed(() -> {
                            if (currentQuestionIndex + 1 < questionIds.size()) {
                                currentQuestionIndex++;
                                loadQuestion(questionIds.get(currentQuestionIndex));
                            } else {
                                updateScoresAndCheckGameOver();
                            }
                        }, 2000);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gametest.this, "Error monitoring answer status: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void updateDots(int index, boolean senderCorrect, boolean receiverCorrect) {
        if (index < senderDots.length && index < receiverDots.length) {
            runOnUiThread(() -> {
                int senderColor = getResources().getColor(senderCorrect ? android.R.color.holo_green_light : android.R.color.holo_red_light);
                int receiverColor = getResources().getColor(receiverCorrect ? android.R.color.holo_green_light : android.R.color.holo_red_light);
                Drawable senderBackground = getResources().getDrawable(R.drawable.circle_dot).mutate();
                senderBackground.setTint(senderColor);
                senderDots[index].setBackground(senderBackground);
                Drawable receiverBackground = getResources().getDrawable(R.drawable.circle_dot).mutate();
                receiverBackground.setTint(receiverColor);
                receiverDots[index].setBackground(receiverBackground);
                animateDot(senderDots[index]);
                animateDot(receiverDots[index]);
            });
        }
    }

    private void animateDot(View dot) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(dot,
                PropertyValuesHolder.ofFloat("scaleX", 1.3f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1.3f, 1f),
                PropertyValuesHolder.ofFloat("alpha", 0f, 1f));
        animator.setDuration(400);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }


    public void updateScoresAndCheckGameOver() {
        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer score1 = snapshot.child("score1").getValue(Integer.class);
                    Integer score2 = snapshot.child("score2").getValue(Integer.class);
                    if (score1 != null && score2 != null) {
                        showGameOverScreen(score1, score2);
                    } else {
                        new android.os.Handler().postDelayed(() -> updateScoresAndCheckGameOver(), 500);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gametest.this, "Error fetching scores: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showGameOverScreen(int score1, int score2) {
        Toast.makeText(gametest.this, "Game Over!", Toast.LENGTH_SHORT).show();
        stopQuestionTimer();
        CardView resultLayout = findViewById(R.id.resultLayout);
        TextView resultTextView = findViewById(R.id.resultTextView);
        TextView scoreTextView = findViewById(R.id.scoreTextView);
        Button returnButton = findViewById(R.id.returnButton);

        String resultMessage;
        if (score1 > score2) {
            resultMessage = "🏆 Player 1 Wins!";
        } else if (score2 > score1) {
            resultMessage = "🏆 Player 2 Wins!";
        } else {
            resultMessage = "🤝 It's a Tie!";
        }

        // Set black background for the whole result layout
        resultLayout.setRadius(Gravity.CENTER);

// Stylize result text
        resultTextView.setText(resultMessage);
        resultTextView.setTextSize(24);
        resultTextView.setTypeface(null, Typeface.BOLD);
        resultTextView.setTextColor(Color.WHITE);
        resultTextView.setGravity(Gravity.CENTER);

// Stylize score text
        scoreTextView.setText("Player 1: " + score1 + " - Player 2: " + score2);
        scoreTextView.setTextSize(18);
        scoreTextView.setTextColor(Color.WHITE);
        scoreTextView.setPadding(0, 20, 0, 40);
        scoreTextView.setGravity(Gravity.CENTER);

// Stylize return button
        returnButton.setText("Back to Main Menu");
        returnButton.setTextSize(16);
        returnButton.setTextColor(Color.WHITE);
        returnButton.setTypeface(null, Typeface.BOLD);
        returnButton.setAllCaps(false);
        returnButton.setPadding(40, 20, 40, 20);
        returnButton.setElevation(8f);
        returnButton.setStateListAnimator(null);

// Red rounded background for the button
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor("#F44336")); // Material red
        buttonBackground.setCornerRadius(30);
        returnButton.setBackground(buttonBackground);


        resultLayout.setRadius(Gravity.CENTER);
        resultLayout.setVisibility(View.VISIBLE);

        // Game result recording and score updating (unchanged)
        if (userId.equals(senderId)) {
            updateUserScore(senderId, score1);
        } else if (userId.equals(receiverId)) {
            updateUserScore(receiverId, score2);
        }

        DatabaseReference matchRecordedRef = gameRef.child("matchRecorded");

        matchRecordedRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean alreadyRecorded = currentData.getValue(Boolean.class);
                if (alreadyRecorded != null && alreadyRecorded) {
                    return Transaction.abort();
                }
                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    Log.d("DEBUG", "Recording match result...");

                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                    DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("MatchHistory");

                    usersRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot senderSnap) {
                            String senderName = senderSnap.child("displayName").getValue(String.class);

                            usersRef.child(receiverId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot receiverSnap) {
                                    String receiverName = receiverSnap.child("displayName").getValue(String.class);

                                    Map<String, Object> matchData = new HashMap<>();
                                    matchData.put("senderName", senderName != null ? senderName : "Unknown");
                                    matchData.put("receiverName", receiverName != null ? receiverName : "Unknown");
                                    matchData.put("senderScore", score1);
                                    matchData.put("receiverScore", score2);
                                    matchData.put("timestamp", System.currentTimeMillis());
                                    matchData.put("senderId", senderId);
                                    matchData.put("receiverId", receiverId);

                                    historyRef.push().setValue(matchData);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("DEBUG", "Receiver read failed", error.toException());
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("DEBUG", "Sender read failed", error.toException());
                        }
                    });
                } else {
                    Log.d("DEBUG", "Match was already recorded, skipping.");
                }
            }
        });

        returnButton.setOnClickListener(v -> {
            gameRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(gametest.this, Main.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(gametest.this, "Error removing game: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    public void updateUserScore(String userId, int additionalScore) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        DatabaseReference userScoreRef = userRef.child("score");

        userScoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentScore = 0;
                if (snapshot.exists() && snapshot.getValue(Integer.class) != null) {
                    currentScore = snapshot.getValue(Integer.class);
                }
                int newScore = currentScore + additionalScore;
                userScoreRef.setValue(newScore).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (newScore >= 100) {
                            int finalScoreAfterLevelUp = newScore - 100;
                            userRef.child("score").setValue(finalScoreAfterLevelUp);
                            userRef.child("level").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot levelSnapshot) {
                                    int currentLevel = 0;
                                    if (levelSnapshot.exists() && levelSnapshot.getValue(Integer.class) != null) {
                                        currentLevel = levelSnapshot.getValue(Integer.class);
                                    }
                                    int newLevel = currentLevel + 1;
                                    userRef.child("level").setValue(newLevel);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                        }
                    } else {
                        Toast.makeText(gametest.this, "Error updating user score.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gametest.this, "Error updating user score: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showCorrectAnswerAnimation(Runnable onComplete) {
        String fullId = questionIds.get(currentQuestionIndex);  // e.g., "Food:5"
        if (fullId == null || !fullId.contains(":")) {
            Toast.makeText(gametest.this, "Invalid question format.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] parts = fullId.split(":");
        String category = parts[0];
        String questionId = parts[1];

        DatabaseReference questionRef = FirebaseDatabase.getInstance()
                .getReference("Questions3").child(category).child(questionId);

        questionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String correctOption = String.valueOf(snapshot.child("correct").getValue());
                    String correctAnswerText = String.valueOf(snapshot.child(correctOption).getValue());
                    Button correctButton = null;

                    if (buttonA.getText().toString().equals(correctAnswerText)) {
                        correctButton = buttonA;
                    } else if (buttonB.getText().toString().equals(correctAnswerText)) {
                        correctButton = buttonB;
                    } else if (buttonC.getText().toString().equals(correctAnswerText)) {
                        correctButton = buttonC;
                    } else if (buttonD.getText().toString().equals(correctAnswerText)) {
                        correctButton = buttonD;
                    }

                    if (correctButton != null) {
                        Button finalCorrectButton = correctButton;
                        runOnUiThread(() -> {
                            int greenColor = ContextCompat.getColor(gametest.this, android.R.color.holo_green_dark);

                            int originalColor = ContextCompat.getColor(getApplicationContext(), R.color.button_background);

                            if (buttonA != finalCorrectButton) {
                                buttonA.setBackgroundColor(originalColor);
                                buttonA.setVisibility(View.INVISIBLE);
                            }
                            if (buttonB != finalCorrectButton) {
                                buttonB.setBackgroundColor(originalColor);
                                buttonB.setVisibility(View.INVISIBLE);
                            }
                            if (buttonC != finalCorrectButton) {
                                buttonC.setBackgroundColor(originalColor);
                                buttonC.setVisibility(View.INVISIBLE);
                            }
                            if (buttonD != finalCorrectButton) {
                                buttonD.setBackgroundColor(originalColor);
                                buttonD.setVisibility(View.INVISIBLE);
                            }


                            finalCorrectButton.setBackgroundTintList(ColorStateList.valueOf(greenColor));

                            new android.os.Handler().postDelayed(() -> {
                                buttonA.setBackgroundTintList(ColorStateList.valueOf(originalColor));
                                buttonA.setVisibility(View.VISIBLE);

                                buttonB.setBackgroundTintList(ColorStateList.valueOf(originalColor));
                                buttonB.setVisibility(View.VISIBLE);

                                buttonC.setBackgroundTintList(ColorStateList.valueOf(originalColor));
                                buttonC.setVisibility(View.VISIBLE);

                                buttonD.setBackgroundTintList(ColorStateList.valueOf(originalColor));
                                buttonD.setVisibility(View.VISIBLE);

                                onComplete.run();
                            }, 1500);
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gametest.this, "Failed to load correct answer: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void monitorJoinStatus() {
        if (userId.equals(senderId)) {
            gameRef.child("senderHasJoined").setValue(true);
        } else if (userId.equals(receiverId)) {
            gameRef.child("recieverHasJoined").setValue(true);
        }
        gameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean senderHasJoined = snapshot.child("senderHasJoined").getValue(Boolean.class);
                Boolean receiverHasJoined = snapshot.child("recieverHasJoined").getValue(Boolean.class);
                if (senderHasJoined != null && receiverHasJoined != null && senderHasJoined && receiverHasJoined) {
                    runOnUiThread(() -> {
                        View mainLayout = findViewById(R.id.main4);
                        if (mainLayout != null) {
                            mainLayout.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gametest.this, "Error checking join status: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void startDisconnectionListener() {
        gameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                View main = findViewById(R.id.main4);
                if (!snapshot.exists() || main.getVisibility() != View.VISIBLE) return;
                Boolean senderHasJoined = snapshot.child("senderHasJoined").getValue(Boolean.class);
                Boolean receiverHasJoined = snapshot.child("recieverHasJoined").getValue(Boolean.class);
                Integer score1 = snapshot.child("score1").getValue(Integer.class);
                Integer score2 = snapshot.child("score2").getValue(Integer.class);
                if (Boolean.FALSE.equals(senderHasJoined) || Boolean.FALSE.equals(receiverHasJoined)) {
                    showGameOverScreen(score1 != null ? score1 : 0, score2 != null ? score2 : 0);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseListener", "Database error: " + error.getMessage());
            }
        });
    }

    protected void onPause() {
        super.onPause();
        CardView resultLayout = findViewById(R.id.resultLayout);
        if (resultLayout.getVisibility() != View.VISIBLE) {
            if (gameRef != null) {
                if (userId.equals(senderId)) {
                    database.child("Statistics").child("gamesEndedEarly").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Integer currentValue = currentData.getValue(Integer.class);
                            if (currentValue == null) {
                                currentData.setValue(1); // Αν δεν υπάρχει καθόλου ακόμα
                            } else {
                                currentData.setValue(currentValue + 1);
                            }
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                            // Προαιρετικό: log ή debug message
                        }
                    });
                    gameRef.child("senderHasJoined").setValue(false);

                } else if (userId.equals(receiverId)) {
                    database.child("Statistics").child("gamesEndedEarly").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Integer currentValue = currentData.getValue(Integer.class);
                            if (currentValue == null) {
                                currentData.setValue(1); // Αν δεν υπάρχει καθόλου ακόμα
                            } else {
                                currentData.setValue(currentValue + 1);
                            }
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                            // Προαιρετικό: log ή debug message
                        }
                    });
                    gameRef.child("recieverHasJoined").setValue(false);
                }
            }
        }
    }

    private void startQuestionTimer() {
        if (questionCountdownTimer != null) {
            questionCountdownTimer.cancel();
        }

        questionTimer.setProgress(100);
        timerText.setText("10");
        timerText.setTextColor(Color.WHITE);
        questionTimer.setProgressTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, android.R.color.holo_green_light)));

        timerRunning = true;
        questionCountdownTimer = new CountDownTimer(QUESTION_TIMER_MILLIS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished * 100 / QUESTION_TIMER_MILLIS);
                questionTimer.setProgress(progress);

                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                timerText.setText(String.valueOf(secondsLeft));

                if (secondsLeft <= 3) {
                    timerText.setTextColor(ContextCompat.getColor(gametest.this, android.R.color.holo_red_light));
                    questionTimer.setProgressTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(gametest.this, android.R.color.holo_red_light)));

                    // Pulse animation when time is almost up
                    if (secondsLeft <= 2) {
                        ObjectAnimator scaleX = ObjectAnimator.ofFloat(timerText, "scaleX", 1f, 1.2f, 1f);
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(timerText, "scaleY", 1f, 1.2f, 1f);
                        scaleX.setDuration(300);
                        scaleY.setDuration(300);
                        scaleX.start();
                        scaleY.start();
                    }
                }
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                questionTimer.setProgress(0);
                timerText.setText("0");
                handleTimeOut();
            }
        }.start();
    }

    private void stopQuestionTimer() {
        if (questionCountdownTimer != null) {
            questionCountdownTimer.cancel();
            timerRunning = false;
        }
    }

    private void resetTimerAppearance() {
        timerText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        timerText.setScaleX(1.0f);
        timerText.setScaleY(1.0f);
        questionTimer.setProgressTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, android.R.color.holo_green_light)));
    }

    private void handleTimeOut() {
        if (userId.equals(senderId)) {
            gameRef.child("senderHasAnswered").setValue(true);
            gameRef.child("senderAnsweredCorrectly").setValue(false);
        } else if (userId.equals(receiverId)) {
            gameRef.child("receiverHasAnswered").setValue(true);
            gameRef.child("receiverAnsweredCorrectly").setValue(false);
        }
        showTimeoutAnimation();
    }

    private void showTimeoutAnimation() {
        runOnUiThread(() -> {
            ValueAnimator colorAnim = ValueAnimator.ofArgb(
                    Color.parseColor("#30FFFFFF"),
                    Color.parseColor("#80FF0000"),
                    Color.parseColor("#30FFFFFF")
            );
            colorAnim.setDuration(500);
            colorAnim.setRepeatCount(1);

            colorAnim.addUpdateListener(animation -> {
                int animatedValue = (int) animation.getAnimatedValue();
                questionTimer.setProgressBackgroundTintList(ColorStateList.valueOf(animatedValue));
            });

            colorAnim.start();

            Toast toast = Toast.makeText(gametest.this, "Time's up!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        });
    }

}
