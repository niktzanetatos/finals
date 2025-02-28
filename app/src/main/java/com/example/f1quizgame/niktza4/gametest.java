package com.example.f1quizgame.niktza4;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class gametest extends AppCompatActivity {

    DatabaseReference gameRef;
    DatabaseReference questionRef;
    FirebaseAuth auth;
    String userId, senderId, receiverId, gameId;
    TextView questionTextView, senderTextView, receiverTextView;
    Button buttonA, buttonB, buttonC;
    List<Integer> questionIds = new ArrayList<>();
    int currentQuestionIndex = 0, score1 = 0, score2 = 0;
    int prevScore1 = 0;
    int prevScore2 = 0;
    View[] senderDots;
    View[] receiverDots;
    String currentDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            setContentView(R.layout.activity_game);


            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            questionTextView = findViewById(R.id.textView10);
            buttonA = findViewById(R.id.a);
            buttonB = findViewById(R.id.b);
            buttonC = findViewById(R.id.c);
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

    public void  loadSenderAndReceiverData() {
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
                      String questionId = String.valueOf(snapshot.child("q" + i).getValue());
                      if (questionId != null && !questionId.equals("null")) {
                          questionIds.add(Integer.parseInt(questionId));
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

    public void loadQuestion(int questionId) {
        DatabaseReference questionRef = FirebaseDatabase.getInstance().getReference("Questions2");

        if (questionId >= 1 && questionId <= 10) {
            currentDifficulty = "easy";
        } else if (questionId >= 11 && questionId <= 20) {
            currentDifficulty = "medium";
        } else {
            currentDifficulty = "hard";
        }

        questionRef.child(currentDifficulty).child(String.valueOf(questionId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String text = String.valueOf(snapshot.child("text").getValue());
                            String a = String.valueOf(snapshot.child("a").getValue());
                            String b = String.valueOf(snapshot.child("b").getValue());
                            String c = String.valueOf(snapshot.child("c").getValue());
                            questionTextView.setText(text);
                            buttonA.setText(a);
                            buttonB.setText(b);
                            buttonC.setText(c);
                            gameRef.child("senderHasAnswered").setValue(false);
                            gameRef.child("receiverHasAnswered").setValue(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(gametest.this, "Failed to load question: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void setAnswerListeners() {
        View.OnClickListener answerListener = v -> {
            Button selectedButton = (Button) v;
            String selectedAnswer = selectedButton.getText().toString();
            DatabaseReference questionRef = FirebaseDatabase.getInstance().getReference("Questions2");
            int currentQuestionId = questionIds.get(currentQuestionIndex);
            questionRef.child(currentDifficulty).child(String.valueOf(currentQuestionId))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String correctAnswer = String.valueOf(snapshot.child("correct").getValue());
                                String correctAnswerText = String.valueOf(snapshot.child(correctAnswer).getValue());

                                if (userId.equals(senderId)) {
                                    if (selectedAnswer.equals(correctAnswerText)) {
                                        score1 += 20;
                                        gameRef.child("score1").setValue(score1);
                                    }
                                    gameRef.child("senderHasAnswered").setValue(true);
                                } else if (userId.equals(receiverId)) {
                                    if (selectedAnswer.equals(correctAnswerText)) {
                                        score2 += 20;
                                        gameRef.child("score2").setValue(score2);
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
                senderDots[index].setBackgroundColor(getResources().getColor(senderCorrect ? android.R.color.holo_green_light : android.R.color.holo_red_light));
                receiverDots[index].setBackgroundColor(getResources().getColor(receiverCorrect ? android.R.color.holo_green_light : android.R.color.holo_red_light));
            });
        }
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

        LinearLayout resultLayout = findViewById(R.id.resultLayout);
        TextView resultTextView = findViewById(R.id.resultTextView);
        TextView scoreTextView = findViewById(R.id.scoreTextView);
        Button returnButton = findViewById(R.id.returnButton);

        String resultMessage;
        if (score1 > score2) {
            resultMessage = "Player 1 Wins!";
        } else if (score2 > score1) {
            resultMessage = "Player 2 Wins!";
        } else {
            resultMessage = "It's a Tie!";
        }

        resultTextView.setText(resultMessage);
        scoreTextView.setText("Player 1: " + score1 + " - Player 2: " + score2);
        resultLayout.setVisibility(View.VISIBLE);

        if (userId.equals(senderId)) {
            updateUserScore(senderId, score1);
        } else if (userId.equals(receiverId)) {
            updateUserScore(receiverId, score2);
        }

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
        int currentQuestionId = questionIds.get(currentQuestionIndex);
        questionRef.child(currentDifficulty).child(String.valueOf(currentQuestionId)).addListenerForSingleValueEvent(new ValueEventListener() {
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
                    }
                    if (correctButton != null) {
                        Button finalCorrectButton = correctButton;
                        runOnUiThread(() -> {
                            if (buttonA != finalCorrectButton)
                                buttonA.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                            if (buttonB != finalCorrectButton)
                                buttonB.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                            if (buttonC != finalCorrectButton)
                                buttonC.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                            finalCorrectButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                            new android.os.Handler().postDelayed(() -> {
                                buttonA.setBackgroundColor(getResources().getColor(android.R.color.white));
                                buttonB.setBackgroundColor(getResources().getColor(android.R.color.white));
                                buttonC.setBackgroundColor(getResources().getColor(android.R.color.white));
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
                        View mainLayout = findViewById(R.id.main2);
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
                View main = findViewById(R.id.main2);
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
        LinearLayout resultLayout = findViewById(R.id.resultLayout);
        if (resultLayout.getVisibility() != View.VISIBLE) {
            if (gameRef != null) {
                if (userId.equals(senderId)) {
                    gameRef.child("senderHasJoined").setValue(false);
                } else if (userId.equals(receiverId)) {
                    gameRef.child("recieverHasJoined").setValue(false);
                }
            }
        }
    }
}
