package com.example.f1quizgame.niktza4;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class History extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private String userId;
    private List<Match> matchList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize views
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Add divider between items
        historyRecyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        );

        // Get user ID from intent
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMatchHistory();
    }

    private void loadMatchHistory() {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("MatchHistory");
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                matchList.clear();

                for (DataSnapshot matchSnapshot : snapshot.getChildren()) {
                    String senderName = matchSnapshot.child("senderName").getValue(String.class);
                    String receiverName = matchSnapshot.child("receiverName").getValue(String.class);
                    Long senderScore = matchSnapshot.child("senderScore").getValue(Long.class);
                    Long receiverScore = matchSnapshot.child("receiverScore").getValue(Long.class);
                    String senderId = matchSnapshot.child("senderId").getValue(String.class);
                    String receiverId = matchSnapshot.child("receiverId").getValue(String.class);
                    Long timestamp = matchSnapshot.child("timestamp").getValue(Long.class);

                    if (!isValidMatch(senderId, receiverId, senderScore, receiverScore)) {
                        continue;
                    }

                    Match match = new Match(
                            senderName,
                            receiverName,
                            senderScore,
                            receiverScore,
                            senderId,
                            receiverId,
                            userId,
                            timestamp != null ? timestamp : System.currentTimeMillis()
                    );
                    matchList.add(match);
                }

                // Sort by timestamp (newest first)
                Collections.sort(matchList, (m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));

                if (matchList.isEmpty()) {
                    Toast.makeText(History.this, "No match history found", Toast.LENGTH_SHORT).show();
                }

                historyRecyclerView.setAdapter(new HistoryAdapter());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(History.this, "Failed to load history: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidMatch(String senderId, String receiverId, Long senderScore, Long receiverScore) {
        return senderId != null &&
                receiverId != null &&
                senderScore != null &&
                receiverScore != null &&
                (userId.equals(senderId) || userId.equals(receiverId));
    }

    private class Match {
        private String senderName;
        private String receiverName;
        private Long senderScore;
        private Long receiverScore;
        private String senderId;
        private String receiverId;
        private String userId;
        private long timestamp;

        public Match(String senderName, String receiverName, Long senderScore, Long receiverScore,
                     String senderId, String receiverId, String userId, long timestamp) {
            this.senderName = senderName;
            this.receiverName = receiverName;
            this.senderScore = senderScore;
            this.receiverScore = receiverScore;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.userId = userId;
            this.timestamp = timestamp;
        }

        // Getters
        public String getSenderName() { return senderName; }
        public String getReceiverName() { return receiverName; }
        public Long getSenderScore() { return senderScore; }
        public Long getReceiverScore() { return receiverScore; }
        public String getSenderId() { return senderId; }
        public String getReceiverId() { return receiverId; }
        public String getUserId() { return userId; }
        public long getTimestamp() { return timestamp; }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_match, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Match match = matchList.get(position);

            // Format the match text
            String formattedText = String.format("%s %d - %d %s",
                    match.getSenderName(),
                    match.getSenderScore(),
                    match.getReceiverScore(),
                    match.getReceiverName());

            holder.matchText.setText(formattedText);

            // Determine if current user is sender or receiver
            boolean isUserSender = match.getUserId().equals(match.getSenderId());

            // Calculate result
            int result;
            String resultText;
            int cardColor;
            int badgeColor;

            if (match.getSenderScore().equals(match.getReceiverScore())) {
                result = R.string.draw;
                resultText = "DRAW";
                cardColor = Color.parseColor("#3E2723");  // Dark brown
                badgeColor = Color.parseColor("#FFA500");  // Orange
            }
            else if ((isUserSender && match.getSenderScore() > match.getReceiverScore()) ||
                    (!isUserSender && match.getReceiverScore() > match.getSenderScore())) {
                result = R.string.win;
                resultText = "WIN";
                cardColor = Color.parseColor("#1B5E20");  // Dark green
                badgeColor = Color.parseColor("#4CAF50"); // Green
            }
            else {
                result = R.string.loss;
                resultText = "LOSS";
                cardColor = Color.parseColor("#B71C1C");  // Dark red
                badgeColor = Color.parseColor("#F44336"); // Red
            }

            // Apply styling to card
            GradientDrawable cardBackground = new GradientDrawable();
            cardBackground.setColor(cardColor);
            cardBackground.setCornerRadius(getResources().getDimension(R.dimen.card_corner_radius));
            holder.itemView.setBackground(cardBackground);

            // Set result badge
            holder.resultBadge.setText(resultText);
            GradientDrawable badgeBackground = new GradientDrawable();
            badgeBackground.setColor(badgeColor);
            badgeBackground.setCornerRadius(getResources().getDimension(R.dimen.badge_corner_radius));
            holder.resultBadge.setBackground(badgeBackground);
        }

        @Override
        public int getItemCount() {
            return matchList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView matchText;
            TextView resultBadge;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                matchText = itemView.findViewById(R.id.matchText);
                resultBadge = itemView.findViewById(R.id.resultBadge);
            }
        }
    }
}