package com.example.f1quizgame.niktza4;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FlagGameActivity extends AppCompatActivity {

    private String senderId, receiverId, userId, gameId;
    private DatabaseReference gameRef, flagsRef;
    private FirebaseAuth auth;

    private ImageView flagImageView;
    private View flagReveal;
    private EditText answerInput;
    private Button submitButton;
    private TextView senderTextView, receiverTextView;

    private List<String> questionIds = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int prevScore1 = 0, prevScore2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flag_game);

        // Initialize views
        flagImageView = findViewById(R.id.flagImage);
        flagReveal = findViewById(R.id.flagReveal);
        answerInput = findViewById(R.id.answerInput);
        submitButton = findViewById(R.id.submitButton);
        senderTextView = findViewById(R.id.textViewSender);
        receiverTextView = findViewById(R.id.textViewReceiver);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();
        senderId = getIntent().getStringExtra("senderId");
        receiverId = getIntent().getStringExtra("receiverId");
        gameId = getIntent().getStringExtra("gameId");
        gameRef = FirebaseDatabase.getInstance().getReference("flagGames").child(gameId);
        flagsRef = FirebaseDatabase.getInstance().getReference("Flags");

        // Load game data
        loadSenderAndReceiverData();
        monitorJoinStatus();
        loadGameData();
        monitorAnswerStatus();

        submitButton.setOnClickListener(v -> checkAnswer());
    }

    private void loadSenderAndReceiverData() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
        userRef.child(senderId).child("displayName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderTextView.setText(snapshot.exists() ? snapshot.getValue(String.class) : "Player 1");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        userRef.child(receiverId).child("displayName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                receiverTextView.setText(snapshot.exists() ? snapshot.getValue(String.class) : "Player 2");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadGameData() {
        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (int i = 1; i <= 6; i++) {
                    String q = snapshot.child("q" + i).getValue(String.class);
                    if (q != null) questionIds.add(q);
                }
                loadQuestion(questionIds.get(currentQuestionIndex));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FlagGameActivity.this, "Failed to load game.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestion(String countryKey) {
        // Reset views for new question
        flagImageView.setVisibility(View.INVISIBLE);
        flagReveal.setVisibility(View.INVISIBLE);
        answerInput.setText("");

        flagsRef.child(countryKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imageUrl = snapshot.child("imageLink").getValue(String.class);

                // Load image with Glide
                Glide.with(FlagGameActivity.this)
                        .load(imageUrl)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                // Start reveal animation when image is ready
                                animateFlagReveal();
                                return false;
                            }
                        })
                        .into(flagImageView);

                // Reset answer status
                gameRef.child("senderHasAnswered").setValue(false);
                gameRef.child("receiverHasAnswered").setValue(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FlagGameActivity.this, "Failed to load flag.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateFlagReveal() {
        // Set initial state
        flagReveal.setVisibility(View.VISIBLE);
        flagReveal.setScaleX(0f);
        flagReveal.setScaleY(0f);
        flagReveal.setAlpha(1f);

        // Create reveal animation
        flagReveal.animate()
                .scaleX(30f)  // Expand beyond screen width
                .scaleY(30f)
                .setDuration(600)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    // Show flag
                    flagImageView.setVisibility(View.VISIBLE);
                    flagImageView.setAlpha(0f);
                    flagImageView.animate()
                            .alpha(1f)
                            .setDuration(400)
                            .start();

                    // Fade out reveal effect
                    flagReveal.animate()
                            .alpha(0f)
                            .setDuration(400)
                            .withEndAction(() -> flagReveal.setVisibility(View.INVISIBLE))
                            .start();
                })
                .start();
    }

    private void checkAnswer() {
        String userAnswer = answerInput.getText().toString().trim().toLowerCase();
        String correctAnswer = questionIds.get(currentQuestionIndex).toLowerCase();

        if (userAnswer.equals(correctAnswer)) {
            if (userId.equals(senderId)) {
                gameRef.child("score1").runTransaction(scoreTransaction());
                gameRef.child("senderHasAnswered").setValue(true);
            } else {
                gameRef.child("score2").runTransaction(scoreTransaction());
                gameRef.child("receiverHasAnswered").setValue(true);
            }
            answerInput.setText("");
        } else {
            Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private Transaction.Handler scoreTransaction() {
        return new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentScore = currentData.getValue(Integer.class);
                if (currentScore == null) currentScore = 0;
                currentData.setValue(currentScore + 10);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        };
    }

    private void monitorAnswerStatus() {
        gameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean senderAnswered = snapshot.child("senderHasAnswered").getValue(Boolean.class) != null
                        && snapshot.child("senderHasAnswered").getValue(Boolean.class);
                boolean receiverAnswered = snapshot.child("receiverHasAnswered").getValue(Boolean.class) != null
                        && snapshot.child("receiverHasAnswered").getValue(Boolean.class);

                if (senderAnswered || receiverAnswered) {
                    currentQuestionIndex++;
                    if (currentQuestionIndex < questionIds.size()) {
                        loadQuestion(questionIds.get(currentQuestionIndex));
                    } else {
                        showGameOver();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showGameOver() {
        // Delete the game from database when it ends
        gameRef.removeValue().addOnCompleteListener(task -> {
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Main.class);
            startActivity(intent);
            finish();
        });
    }

    private void monitorJoinStatus() {
        if (userId.equals(senderId)) {
            gameRef.child("senderHasJoined").setValue(true);
        } else {
            gameRef.child("receiverHasJoined").setValue(true);
        }
    }

    @Override
    protected void onDestroy() {
        flagReveal.animate().cancel();
        flagImageView.animate().cancel();
        super.onDestroy();
    }
}