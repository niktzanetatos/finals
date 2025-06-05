package com.example.f1quizgame.niktza4;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main extends AppCompatActivity {

    FirebaseAuth auth;
    DatabaseReference database;
    String userId;
    Button history, showProfile;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        EdgeToEdge.enable(this);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        history = findViewById(R.id.historyButton);

        history.setOnClickListener(v -> openHistory());
        showProfile = findViewById(R.id.profile);

        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        showProfile.setOnClickListener(v -> seeProfile(userId));

        monitorOnlineStatus();
        checkForInvitations();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main4), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


    }


    public void monitorOnlineStatus() {
        DatabaseReference userRef = database.child("users").child(userId);
        userRef.child("online").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.getValue(Boolean.class);
                if (isOnline != null) {
                    String status = isOnline ? "Online" : "Offline";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void showOnline(View view) {
        ScrollView scrollView = findViewById(R.id.scrollview);
        LinearLayout onlineUsersLayout = findViewById(R.id.linearlayout);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (scrollView.getVisibility() == View.INVISIBLE) {
            onlineUsersLayout.removeAllViews();

            // Enhanced background with subtle gradient
            GradientDrawable backgroundGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.parseColor("#1a1a1a"), Color.parseColor("#0d0d0d")}
            );
            backgroundGradient.setCornerRadius(24f);
            onlineUsersLayout.setBackground(backgroundGradient);
            onlineUsersLayout.setPadding(20, 16, 20, 20);

            // Enhanced header with better styling
            LinearLayout headerLayout = new LinearLayout(Main.this);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setGravity(Gravity.CENTER);
            headerLayout.setPadding(0, 16, 0, 20);

            // Online indicator dot
            View onlineDot = new View(Main.this);
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(Color.parseColor("#00ff88"));
            dotDrawable.setSize(12, 12);
            onlineDot.setBackground(dotDrawable);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(12, 12);
            dotParams.setMargins(0, 0, 12, 0);
            onlineDot.setLayoutParams(dotParams);

            TextView headerTextView = new TextView(Main.this);
            headerTextView.setText("ONLINE PLAYERS");
            headerTextView.setTextSize(20);
            headerTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            headerTextView.setTextColor(Color.parseColor("#ffffff"));
            headerTextView.setLetterSpacing(0.05f);

            headerLayout.addView(onlineDot);
            headerLayout.addView(headerTextView);

            // Subtle divider line
            View divider = new View(Main.this);
            GradientDrawable dividerDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#333333"), Color.TRANSPARENT}
            );
            divider.setBackground(dividerDrawable);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            dividerParams.setMargins(20, 8, 20, 24);
            divider.setLayoutParams(dividerParams);

            onlineUsersLayout.addView(headerLayout);
            onlineUsersLayout.addView(divider);

            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRef.orderByChild("online").equalTo(true).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Remove existing user items (keep header and divider)
                    int childCount = onlineUsersLayout.getChildCount();
                    if (childCount > 2) {
                        onlineUsersLayout.removeViews(2, childCount - 2);
                    }

                    if (snapshot.exists()) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String userId = userSnapshot.getKey();

                            if (userId != null && userId.equals(currentUserId)) {
                                continue;
                            }

                            String displayName = userSnapshot.child("displayName").getValue(String.class);
                            String receiverId = userSnapshot.getKey();

                            if (displayName != null) {
                                // Enhanced user card with modern design
                                LinearLayout userCard = new LinearLayout(Main.this);
                                userCard.setOrientation(LinearLayout.VERTICAL);

                                // Card background with elevation effect
                                GradientDrawable cardBackground = new GradientDrawable();
                                cardBackground.setColor(Color.parseColor("#262626"));
                                cardBackground.setCornerRadius(16f);
                                cardBackground.setStroke(1, Color.parseColor("#404040"));
                                userCard.setBackground(cardBackground);
                                userCard.setElevation(4f);

                                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );
                                cardParams.setMargins(0, 0, 0, 12);
                                userCard.setLayoutParams(cardParams);
                                userCard.setPadding(20, 16, 20, 16);

                                // User info section
                                LinearLayout userInfoLayout = new LinearLayout(Main.this);
                                userInfoLayout.setOrientation(LinearLayout.HORIZONTAL);
                                userInfoLayout.setGravity(Gravity.CENTER_VERTICAL);
                                userInfoLayout.setPadding(0, 0, 0, 12);

                                // Avatar placeholder with gradient
                                TextView avatarView = new TextView(Main.this);
                                avatarView.setText(displayName.substring(0, 1).toUpperCase());
                                avatarView.setTextSize(16);
                                avatarView.setTypeface(null, Typeface.BOLD);
                                avatarView.setTextColor(Color.WHITE);
                                avatarView.setGravity(Gravity.CENTER);

                                GradientDrawable avatarBg = new GradientDrawable(
                                        GradientDrawable.Orientation.TL_BR,
                                        new int[]{Color.parseColor("#ff6b6b"), Color.parseColor("#4ecdc4")}
                                );
                                avatarBg.setShape(GradientDrawable.OVAL);
                                avatarView.setBackground(avatarBg);

                                LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(40, 40);
                                avatarParams.setMargins(0, 0, 16, 0);
                                avatarView.setLayoutParams(avatarParams);

                                // User info text layout
                                LinearLayout textLayout = new LinearLayout(Main.this);
                                textLayout.setOrientation(LinearLayout.VERTICAL);
                                LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                                textLayout.setLayoutParams(textLayoutParams);

                                // Username
                                TextView userTextView = new TextView(Main.this);
                                userTextView.setText(displayName);
                                userTextView.setTextSize(16);
                                userTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                                userTextView.setTextColor(Color.parseColor("#ffffff"));
                                userTextView.setOnClickListener(v -> seeProfile(receiverId));

                                // Online status
                                TextView statusTextView = new TextView(Main.this);
                                statusTextView.setText("• Online now");
                                statusTextView.setTextSize(12);
                                statusTextView.setTextColor(Color.parseColor("#00ff88"));
                                statusTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

                                textLayout.addView(userTextView);
                                textLayout.addView(statusTextView);

                                userInfoLayout.addView(avatarView);
                                userInfoLayout.addView(textLayout);

                                // Enhanced buttons section
                                LinearLayout buttonLayout = new LinearLayout(Main.this);
                                buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
                                buttonLayout.setGravity(Gravity.CENTER);

                                // General Button with modern styling
                                Button generalButton = createStyledButton("General", Color.parseColor("#4285f4"));
                                generalButton.setOnClickListener(v -> sendInvitation(receiverId));

                                // Flags Button with modern styling
                                Button flagsButton = createStyledButton("Flags", Color.parseColor("#ea4335"));
                                flagsButton.setOnClickListener(v -> sendFlagInvitation(receiverId));

                                // Add spacing between buttons
                                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                                buttonParams.setMargins(4, 0, 4, 0);

                                buttonLayout.addView(generalButton, buttonParams);
                                buttonLayout.addView(flagsButton, buttonParams);

                                userCard.addView(userInfoLayout);
                                userCard.addView(buttonLayout);
                                onlineUsersLayout.addView(userCard);
                            }
                        }

                        // Enhanced empty state
                        if (onlineUsersLayout.getChildCount() == 2) {
                            addEmptyState(onlineUsersLayout, "No other players are currently online",
                                    "Check back later to see who's available to play!");
                        }

                        scrollView.setVisibility(View.VISIBLE);
                    } else {
                        addEmptyState(onlineUsersLayout, "No players online",
                                "Be the first to start playing!");
                        scrollView.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    addErrorState(onlineUsersLayout, "Unable to load online players",
                            "Please check your connection and try again");
                    scrollView.setVisibility(View.VISIBLE);
                }
            });
        } else {
            scrollView.setVisibility(View.INVISIBLE);
        }
    }

    private Button createStyledButton(String text, int color) {
        Button button = new Button(Main.this);
        button.setText(text);
        button.setTextSize(13);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setPadding(24, 12, 24, 12);

        // Create gradient background
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setColor(color);
        buttonBg.setCornerRadius(20f);
        button.setBackground(buttonBg);
        button.setElevation(2f);

        // Add ripple effect for API 21+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            button.setStateListAnimator(null);
        }

        return button;
    }

    private void addEmptyState(LinearLayout layout, String title, String subtitle) {
        LinearLayout emptyLayout = new LinearLayout(Main.this);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setGravity(Gravity.CENTER);
        emptyLayout.setPadding(40, 40, 40, 40);

        TextView titleView = new TextView(Main.this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(Color.parseColor("#999999"));
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setGravity(Gravity.CENTER);

        TextView subtitleView = new TextView(Main.this);
        subtitleView.setText(subtitle);
        subtitleView.setTextSize(14);
        subtitleView.setTextColor(Color.parseColor("#666666"));
        subtitleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.setMargins(0, 8, 0, 0);
        subtitleView.setLayoutParams(subtitleParams);

        emptyLayout.addView(titleView);
        emptyLayout.addView(subtitleView);
        layout.addView(emptyLayout);
    }

    private void addErrorState(LinearLayout layout, String title, String subtitle) {
        LinearLayout errorLayout = new LinearLayout(Main.this);
        errorLayout.setOrientation(LinearLayout.VERTICAL);
        errorLayout.setGravity(Gravity.CENTER);
        errorLayout.setPadding(40, 40, 40, 40);

        TextView titleView = new TextView(Main.this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(Color.parseColor("#ff6b6b"));
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setGravity(Gravity.CENTER);

        TextView subtitleView = new TextView(Main.this);
        subtitleView.setText(subtitle);
        subtitleView.setTextSize(14);
        subtitleView.setTextColor(Color.parseColor("#999999"));
        subtitleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.setMargins(0, 8, 0, 0);
        subtitleView.setLayoutParams(subtitleParams);

        errorLayout.addView(titleView);
        errorLayout.addView(subtitleView);
        layout.addView(errorLayout);
    }


    public void sendInvitation(String receiverId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference invitesRef = FirebaseDatabase.getInstance().getReference("invitations");
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        usersRef.child(senderId).child("level").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                if (senderSnapshot.exists()) {
                    int senderLevel = senderSnapshot.getValue(Integer.class);
                    usersRef.child(receiverId).child("level").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot receiverSnapshot) {
                            if (receiverSnapshot.exists()) {
                                int receiverLevel = receiverSnapshot.getValue(Integer.class);
                                if (Math.abs(senderLevel - receiverLevel) > 1) {
                                    Toast.makeText(Main.this,
                                            "Cannot send invitation. Level difference is too big",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    String invitationId = invitesRef.push().getKey();
                                    Map<String, Object> invitationData = new HashMap<>();
                                    invitationData.put("senderId", senderId);
                                    invitationData.put("receiverId", receiverId);
                                    invitationData.put("status", "pending");
                                    invitationData.put("type", "General");

                                    if (invitationId != null) {
                                        invitesRef.child(invitationId).setValue(invitationData)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(Main.this, "Invitation sent. Waiting for response...", Toast.LENGTH_SHORT).show();

                                                        // 🔁 Listen for status change
                                                        invitesRef.child(invitationId).child("status")
                                                                .addValueEventListener(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        String status = snapshot.getValue(String.class);
                                                                        if ("accepted".equals(status)) {
                                                                            joinGame(senderId, receiverId);
                                                                            // ✅ Remove invitation (cleanup)
                                                                            invitesRef.child(invitationId).removeValue();
                                                                        } else if ("declined".equals(status)) {
                                                                            Toast.makeText(Main.this, "Invitation was declined.", Toast.LENGTH_SHORT).show();
                                                                            invitesRef.child(invitationId).removeValue();
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Log.e("InviteStatus", "Error monitoring invite: " + error.getMessage());
                                                                    }
                                                                });
                                                    } else {
                                                        Toast.makeText(Main.this, "Failed to send invitation: " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            } else {
                                Toast.makeText(Main.this, "Failed to retrieve receiver's level.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(Main.this, "Error retrieving receiver's level: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(Main.this, "Failed to retrieve sender's level.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Main.this, "Error retrieving sender's level: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void checkForInvitations() {
        DatabaseReference invitesRef = FirebaseDatabase.getInstance().getReference("invitations");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        invitesRef.orderByChild("receiverId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TextView invitationIndicator = findViewById(R.id.textView7);
                boolean hasPendingInvites = false;
                for (DataSnapshot inviteSnapshot : snapshot.getChildren()) {
                    String status = inviteSnapshot.child("status").getValue(String.class);
                    if ("pending".equals(status)) {
                        hasPendingInvites = true;
                        break;
                    }
                }

                if (hasPendingInvites) {
                    invitationIndicator.setVisibility(View.VISIBLE);
                } else {
                    invitationIndicator.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Invitations", "Failed to read invitations: " + error.getMessage());
            }
        });
    }

    public void seeInvites(View view) {
        ScrollView scrollView = findViewById(R.id.scrollview);
        LinearLayout pendingInvitesLayout = findViewById(R.id.linearlayout);

        if (scrollView.getVisibility() == View.INVISIBLE) {
            pendingInvitesLayout.removeAllViews();

            // Clean, minimal background
            pendingInvitesLayout.setBackgroundColor(Color.parseColor("#1e1e1e"));
            pendingInvitesLayout.setPadding(24, 20, 24, 24);

            // Simple, clean header
            TextView headerTextView = new TextView(Main.this);
            headerTextView.setText("Invitations");
            headerTextView.setTextSize(24);
            headerTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            headerTextView.setTextColor(Color.parseColor("#ffffff"));
            headerTextView.setGravity(Gravity.CENTER);
            headerTextView.setPadding(0, 8, 0, 32);

            pendingInvitesLayout.addView(headerTextView);

            DatabaseReference invitesRef = FirebaseDatabase.getInstance().getReference("invitations");
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            invitesRef.orderByChild("receiverId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Remove existing invite items (keep header)
                    int childCount = pendingInvitesLayout.getChildCount();
                    if (childCount > 1) {
                        pendingInvitesLayout.removeViews(1, childCount - 1);
                    }

                    if (snapshot.exists()) {
                        boolean hasInvites = false;
                        for (DataSnapshot inviteSnapshot : snapshot.getChildren()) {
                            String status = inviteSnapshot.child("status").getValue(String.class);
                            if ("pending".equals(status)) {
                                hasInvites = true;
                                String senderId = inviteSnapshot.child("senderId").getValue(String.class);
                                String gameType = inviteSnapshot.child("type").getValue(String.class);
                                DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
                                senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                                        String senderName = senderSnapshot.child("displayName").getValue(String.class);
                                        if (senderName == null) senderName = "Unknown User";

                                        // Clean invitation row
                                        LinearLayout inviteRow = new LinearLayout(Main.this);
                                        inviteRow.setOrientation(LinearLayout.HORIZONTAL);
                                        inviteRow.setGravity(Gravity.CENTER_VERTICAL);
                                        inviteRow.setPadding(20, 20, 20, 20);
                                        inviteRow.setBackgroundColor(Color.parseColor("#2a2a2a"));

                                        // Subtle rounded corners
                                        GradientDrawable rowBackground = new GradientDrawable();
                                        rowBackground.setColor(Color.parseColor("#2a2a2a"));
                                        rowBackground.setCornerRadius(12f);
                                        inviteRow.setBackground(rowBackground);

                                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        );
                                        rowParams.setMargins(0, 0, 0, 16);
                                        inviteRow.setLayoutParams(rowParams);

                                        // Left side - Player info
                                        LinearLayout playerInfo = new LinearLayout(Main.this);
                                        playerInfo.setOrientation(LinearLayout.VERTICAL);
                                        LinearLayout.LayoutParams playerInfoParams = new LinearLayout.LayoutParams(
                                                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                                        playerInfo.setLayoutParams(playerInfoParams);

                                        TextView playerName = new TextView(Main.this);
                                        playerName.setText(senderName);
                                        playerName.setTextSize(16);
                                        playerName.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                                        playerName.setTextColor(Color.parseColor("#ffffff"));

                                        TextView gameInfo = new TextView(Main.this);
                                        gameInfo.setText(gameType + " game");
                                        gameInfo.setTextSize(14);
                                        gameInfo.setTextColor(Color.parseColor("#b0b0b0"));
                                        gameInfo.setPadding(0, 4, 0, 0);

                                        playerInfo.addView(playerName);
                                        playerInfo.addView(gameInfo);

                                        // Right side - Action buttons
                                        LinearLayout buttonContainer = new LinearLayout(Main.this);
                                        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
                                        buttonContainer.setGravity(Gravity.CENTER_VERTICAL);

                                        // Accept button - clean green
                                        Button acceptButton = new Button(Main.this);
                                        acceptButton.setText("✓");
                                        acceptButton.setTextSize(18);
                                        acceptButton.setTextColor(Color.WHITE);
                                        acceptButton.setTypeface(null, Typeface.BOLD);
                                        acceptButton.setPadding(0, 0, 0, 0);
                                        acceptButton.setAllCaps(false);

                                        GradientDrawable acceptBg = new GradientDrawable();
                                        acceptBg.setShape(GradientDrawable.OVAL);
                                        acceptBg.setColor(Color.parseColor("#27ae60"));
                                        acceptButton.setBackground(acceptBg);

                                        LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(48, 48);
                                        acceptParams.setMargins(0, 0, 12, 0);
                                        acceptButton.setLayoutParams(acceptParams);

                                        String receiverId = currentUserId;
                                        acceptButton.setOnClickListener(v -> {
                                            inviteSnapshot.getRef().child("status").setValue("accepted")
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(Main.this, "Invitation accepted!", Toast.LENGTH_SHORT).show();
                                                        pendingInvitesLayout.removeView(inviteRow);

                                                        // Game joining logic (same as before)
                                                        DatabaseReference gamesRef = FirebaseDatabase.getInstance().getReference(
                                                                "Flags".equalsIgnoreCase(gameType) ? "flagGames" : "games");

                                                        ValueEventListener gameListener = new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                for (DataSnapshot gameSnapshot : dataSnapshot.getChildren()) {
                                                                    String sId = gameSnapshot.child("senderId").getValue(String.class);
                                                                    String rId = gameSnapshot.child("receiverId").getValue(String.class);

                                                                    if (senderId.equals(sId) && receiverId.equals(rId)) {
                                                                        String gameId = gameSnapshot.getKey();
                                                                        gamesRef.removeEventListener(this);

                                                                        Intent intent;
                                                                        if ("Flags".equalsIgnoreCase(gameType)) {
                                                                            intent = new Intent(Main.this, FlagGameActivity.class);
                                                                        } else {
                                                                            intent = new Intent(Main.this, gametest.class);
                                                                        }

                                                                        intent.putExtra("senderId", senderId);
                                                                        intent.putExtra("receiverId", receiverId);
                                                                        intent.putExtra("gameId", gameId);
                                                                        database.child("Statistics").child("totalGames").runTransaction(new Transaction.Handler() {
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
                                                                                // Optional: log or debug
                                                                            }
                                                                        });

                                                                        startActivity(intent);
                                                                        break;
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {
                                                                Toast.makeText(Main.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        };

                                                        gamesRef.addValueEventListener(gameListener);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(Main.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        });

                                        // Decline button - clean red
                                        Button declineButton = new Button(Main.this);
                                        declineButton.setText("✕");
                                        declineButton.setTextSize(16);
                                        declineButton.setTextColor(Color.WHITE);
                                        declineButton.setTypeface(null, Typeface.BOLD);
                                        declineButton.setPadding(0, 0, 0, 0);
                                        declineButton.setAllCaps(false);

                                        GradientDrawable declineBg = new GradientDrawable();
                                        declineBg.setShape(GradientDrawable.OVAL);
                                        declineBg.setColor(Color.parseColor("#e74c3c"));
                                        declineButton.setBackground(declineBg);

                                        LinearLayout.LayoutParams declineParams = new LinearLayout.LayoutParams(48, 48);
                                        declineButton.setLayoutParams(declineParams);

                                        declineButton.setOnClickListener(v -> {
                                            inviteSnapshot.getRef().removeValue()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(Main.this, "Invitation declined", Toast.LENGTH_SHORT).show();
                                                        pendingInvitesLayout.removeView(inviteRow);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(Main.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        });

                                        buttonContainer.addView(acceptButton, acceptParams);
                                        buttonContainer.addView(declineButton, declineParams);

                                        inviteRow.addView(playerInfo);
                                        inviteRow.addView(buttonContainer);
                                        pendingInvitesLayout.addView(inviteRow);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(Main.this, "Error loading player info", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        if (!hasInvites) {
                            addSimpleEmptyState(pendingInvitesLayout);
                        }

                        scrollView.setVisibility(View.VISIBLE);
                    } else {
                        addSimpleEmptyState(pendingInvitesLayout);
                        scrollView.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    TextView errorView = new TextView(Main.this);
                    errorView.setText("Unable to load invitations");
                    errorView.setTextSize(16);
                    errorView.setTextColor(Color.parseColor("#999999"));
                    errorView.setGravity(Gravity.CENTER);
                    errorView.setPadding(0, 32, 0, 0);
                    pendingInvitesLayout.addView(errorView);
                    scrollView.setVisibility(View.VISIBLE);
                }
            });
        } else {
            scrollView.setVisibility(View.INVISIBLE);
        }
    }

    private void addSimpleEmptyState(LinearLayout layout) {
        TextView emptyView = new TextView(Main.this);
        emptyView.setText("No pending invitations");
        emptyView.setTextSize(16);
        emptyView.setTextColor(Color.parseColor("#999999"));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(0, 32, 0, 0);
        layout.addView(emptyView);
    }

    public void joinGame(String senderId, String receiverId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
        userRef.child("navigateToGame").setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference gamesRef = FirebaseDatabase.getInstance().getReference("games");
                gamesRef.orderByChild("senderId").equalTo(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean gameExists = false;
                        String existingGameId = null;

                        for (DataSnapshot gameSnapshot : snapshot.getChildren()) {
                            String currentReceiverId = gameSnapshot.child("receiverId").getValue(String.class);
                            if (receiverId.equals(currentReceiverId)) {
                                gameExists = true;
                                existingGameId = gameSnapshot.getKey();
                                break;
                            }
                        }

                        if (gameExists) {
                            Intent intent = new Intent(Main.this, gametest.class);
                            intent.putExtra("senderId", senderId);
                            intent.putExtra("receiverId", receiverId);
                            intent.putExtra("gameId", existingGameId);
                            startActivity(intent);
                        } else {
                            DatabaseReference questionsRef = FirebaseDatabase.getInstance().getReference("Questions3");
                            questionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    List<String> categories = new ArrayList<>();
                                    for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                                        categories.add(categorySnapshot.getKey());
                                    }

                                    List<String> selectedQuestions = new ArrayList<>();
                                    Random random = new Random();

                                    while (selectedQuestions.size() < 6) {
                                        String category = categories.get(random.nextInt(categories.size()));
                                        DataSnapshot categoryQuestions = dataSnapshot.child(category);
                                        List<String> validIds = new ArrayList<>();

                                        if (categoryQuestions.getValue() instanceof Map) {
                                            for (DataSnapshot q : categoryQuestions.getChildren()) {
                                                try {
                                                    if (q.exists()) {
                                                        validIds.add(q.getKey());
                                                    }
                                                } catch (Exception ignored) {
                                                }
                                            }
                                        } else if (categoryQuestions.getValue() instanceof List || categoryQuestions.hasChildren()) {
                                            for (DataSnapshot q : categoryQuestions.getChildren()) {
                                                try {
                                                    if (q.exists()) {
                                                        validIds.add(q.getKey());
                                                    }
                                                } catch (Exception ignored) {
                                                }
                                            }
                                        }

                                        if (!validIds.isEmpty()) {
                                            String questionId = validIds.get(random.nextInt(validIds.size()));
                                            String uniqueKey = category + ":" + questionId;
                                            if (!selectedQuestions.contains(uniqueKey)) {
                                                selectedQuestions.add(uniqueKey);
                                            }
                                        }
                                    }

                                    String gameId = gamesRef.push().getKey();
                                    if (gameId == null) {
                                        Toast.makeText(Main.this, "Failed to create a game ID.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Map<String, Object> gameData = new HashMap<>();
                                    gameData.put("gameId", gameId);
                                    gameData.put("receiverId", receiverId);
                                    gameData.put("senderId", senderId);
                                    gameData.put("score1", 0);
                                    gameData.put("score2", 0);
                                    gameData.put("senderHasAnswered", false);
                                    gameData.put("receiverHasAnswered", false);
                                    gameData.put("senderHasJoined", false);
                                    gameData.put("recieverHasJoined", false);
                                    gameData.put("q1", selectedQuestions.get(0));
                                    gameData.put("q2", selectedQuestions.get(1));
                                    gameData.put("q3", selectedQuestions.get(2));
                                    gameData.put("q4", selectedQuestions.get(3));
                                    gameData.put("q5", selectedQuestions.get(4));
                                    gameData.put("q6", selectedQuestions.get(5));

                                    gamesRef.child(gameId).setValue(gameData).addOnCompleteListener(gameTask -> {
                                        if (gameTask.isSuccessful()) {
                                            Intent intent = new Intent(Main.this, gametest.class);
                                            intent.putExtra("senderId", senderId);
                                            intent.putExtra("receiverId", receiverId);
                                            intent.putExtra("gameId", gameId);
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(Main.this, "Failed to create game: " + gameTask.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(Main.this, "Failed to fetch questions: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Main.this, "Database query failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(Main.this, "Failed to transition to game: "
                        + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void signOut(View view) {
        database.child("users").child(userId).child("online").setValue(false)
                .addOnCompleteListener(task -> {
                    auth.signOut();
                    startActivity(new Intent(Main.this, MainActivity.class));
                    finish();
                });
    }

    public void openHistory() {
        Intent intent = new Intent(Main.this, History.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }


    public void startSinglePlayerGame(View view) {
        DatabaseReference questionsRef = FirebaseDatabase.getInstance().getReference("Questions3");
        database.child("Statistics").child("survivalTries").runTransaction(new Transaction.Handler() {
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


        questionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> categories = new ArrayList<>();
                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    categories.add(categorySnapshot.getKey());
                }

                List<Question> selectedQuestions = new ArrayList<>();
                Random random = new Random();

                while (selectedQuestions.size() < 10) {
                    String category = categories.get(random.nextInt(categories.size()));
                    DataSnapshot categoryQuestions = dataSnapshot.child(category);
                    List<DataSnapshot> validQuestions = new ArrayList<>();

                    for (DataSnapshot q : categoryQuestions.getChildren()) {
                        if (q.exists()) {
                            validQuestions.add(q);
                        }
                    }

                    if (!validQuestions.isEmpty()) {
                        DataSnapshot randomQ = validQuestions.get(random.nextInt(validQuestions.size()));

                        Question question = randomQ.getValue(Question.class);
                        if (question != null && !selectedQuestions.contains(question)) {
                            selectedQuestions.add(question);
                        }
                    }
                }


                Intent intent = new Intent(Main.this, SingleGameActivity.class);
                intent.putExtra("questionList", new ArrayList<>(selectedQuestions));
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Main.this, "Failed to fetch questions: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void sendFlagInvitation(String receiverId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference invitesRef = FirebaseDatabase.getInstance().getReference("invitations");
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        usersRef.child(senderId).child("level").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                if (senderSnapshot.exists()) {
                    int senderLevel = senderSnapshot.getValue(Integer.class);
                    usersRef.child(receiverId).child("level").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot receiverSnapshot) {
                            if (receiverSnapshot.exists()) {
                                int receiverLevel = receiverSnapshot.getValue(Integer.class);
                                if (Math.abs(senderLevel - receiverLevel) > 1) {
                                    Toast.makeText(Main.this,
                                            "Cannot send invitation. Level difference is too big",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    String invitationId = invitesRef.push().getKey();
                                    Map<String, Object> invitationData = new HashMap<>();
                                    invitationData.put("senderId", senderId);
                                    invitationData.put("receiverId", receiverId);
                                    invitationData.put("status", "pending");
                                    invitationData.put("type", "Flags"); // Changed to "Flags" for flag games

                                    if (invitationId != null) {
                                        invitesRef.child(invitationId).setValue(invitationData)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(Main.this, "Flag game invitation sent. Waiting for response...", Toast.LENGTH_SHORT).show();

                                                        // Listen for status change
                                                        invitesRef.child(invitationId).child("status")
                                                                .addValueEventListener(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        String status = snapshot.getValue(String.class);
                                                                        if ("accepted".equals(status)) {
                                                                            joinFlagsGame(senderId, receiverId);
                                                                            // Remove invitation (cleanup)
                                                                            invitesRef.child(invitationId).removeValue();
                                                                        } else if ("declined".equals(status)) {
                                                                            Toast.makeText(Main.this, "Flag game invitation was declined.", Toast.LENGTH_SHORT).show();
                                                                            invitesRef.child(invitationId).removeValue();
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Log.e("FlagInviteStatus", "Error monitoring invite: " + error.getMessage());
                                                                    }
                                                                });
                                                    } else {
                                                        Toast.makeText(Main.this, "Failed to send invitation: " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            } else {
                                Toast.makeText(Main.this, "Failed to retrieve receiver's level.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(Main.this, "Error retrieving receiver's level: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(Main.this, "Failed to retrieve sender's level.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Main.this, "Error retrieving sender's level: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void joinFlagsGame(String senderId, String receiverId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
        userRef.child("navigateToGame").setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference flagGamesRef = FirebaseDatabase.getInstance().getReference("flagGames");
                flagGamesRef.orderByChild("senderId").equalTo(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean gameExists = false;
                        String existingGameId = null;

                        for (DataSnapshot gameSnapshot : snapshot.getChildren()) {
                            String currentReceiverId = gameSnapshot.child("receiverId").getValue(String.class);
                            if (receiverId.equals(currentReceiverId)) {
                                gameExists = true;
                                existingGameId = gameSnapshot.getKey();
                                break;
                            }
                        }

                        if (gameExists) {
                            Intent intent = new Intent(Main.this, FlagGameActivity.class);
                            intent.putExtra("senderId", senderId);
                            intent.putExtra("receiverId", receiverId);
                            intent.putExtra("gameId", existingGameId);
                            startActivity(intent);
                        } else {
                            DatabaseReference flagsRef = FirebaseDatabase.getInstance().getReference("Flags");
                            flagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    List<String> flagIds = new ArrayList<>();
                                    for (DataSnapshot flagSnapshot : dataSnapshot.getChildren()) {
                                        flagIds.add(flagSnapshot.getKey());
                                    }

                                    if (flagIds.size() < 6) {
                                        Toast.makeText(Main.this, "Not enough flags available.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Collections.shuffle(flagIds);
                                    List<String> selectedFlags = flagIds.subList(0, 6);

                                    String gameId = flagGamesRef.push().getKey();
                                    if (gameId == null) {
                                        Toast.makeText(Main.this, "Failed to create a game ID.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Map<String, Object> gameData = new HashMap<>();
                                    gameData.put("gameId", gameId);
                                    gameData.put("receiverId", receiverId);
                                    gameData.put("senderId", senderId);
                                    gameData.put("score1", 0);
                                    gameData.put("score2", 0);
                                    gameData.put("senderHasAnswered", false);
                                    gameData.put("receiverHasAnswered", false);
                                    gameData.put("senderHasJoined", false);
                                    gameData.put("receiverHasJoined", false);
                                    gameData.put("q1", selectedFlags.get(0));
                                    gameData.put("q2", selectedFlags.get(1));
                                    gameData.put("q3", selectedFlags.get(2));
                                    gameData.put("q4", selectedFlags.get(3));
                                    gameData.put("q5", selectedFlags.get(4));
                                    gameData.put("q6", selectedFlags.get(5));

                                    flagGamesRef.child(gameId).setValue(gameData).addOnCompleteListener(gameTask -> {
                                        if (gameTask.isSuccessful()) {
                                            Intent intent = new Intent(Main.this, FlagGameActivity.class);
                                            intent.putExtra("senderId", senderId);
                                            intent.putExtra("receiverId", receiverId);
                                            intent.putExtra("gameId", gameId);
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(Main.this, "Failed to create game: " + gameTask.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(Main.this, "Failed to fetch flags: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Main.this, "Database query failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(Main.this, "Failed to transition to game: "
                        + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void seeProfile(String userId) {
        Intent intent = new Intent(Main.this, Profile.class);
        intent.putExtra("id", userId);
        startActivity(intent);

    }


    public void seeLeaderboard(View view) {
        Intent intent = new Intent(Main.this, Leaderboard.class);
        startActivity(intent);

    }

    public void seeStatistics(View view) {
        Intent intent = new Intent(Main.this, Statistics.class);
        startActivity(intent);

    }



}