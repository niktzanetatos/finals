package com.example.f1quizgame.niktza4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main extends AppCompatActivity {
    TextView displayNameTextView;
    FirebaseAuth auth;
    DatabaseReference database;
    String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        EdgeToEdge.enable(this);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        displayNameTextView = findViewById(R.id.textView4);
        String displayName = getIntent().getStringExtra("displayName");
        if (displayName != null) {
            displayNameTextView.setText("Welcome, " + userId);
        } else {
            displayNameTextView.setText("Welcome, User");
        }
        monitorOnlineStatus();
        checkForInvitations();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
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
                    displayNameTextView.append("\nStatus: " + status);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                displayNameTextView.append("\nError reading online status.");
            }
        });
    }
    public void showOnline(View view) {
        ScrollView scrollView = findViewById(R.id.scrollview);
        LinearLayout onlineUsersLayout = findViewById(R.id.linearlayout);
        if (scrollView.getVisibility() == View.INVISIBLE) {
            onlineUsersLayout.removeAllViews();
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRef.orderByChild("online").equalTo(true).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    onlineUsersLayout.removeAllViews();
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String displayName = userSnapshot.child("displayName").getValue(String.class);
                            String receiverId = userSnapshot.getKey();
                            if (displayName != null) {
                                TextView userTextView = new TextView(Main.this);
                                userTextView.setText(displayName);
                                userTextView.setTextSize(18);
                                userTextView.setPadding(10, 20, 10, 20);
                                userTextView.setTextColor(getResources().getColor(android.R.color.black));
                                userTextView.setCompoundDrawablesWithIntrinsicBounds(
                                        0,
                                        0,
                                        R.drawable.green_circle,
                                        0
                                );
                                userTextView.setCompoundDrawablePadding(16);
                               userTextView.setOnClickListener(v -> sendInvitation(receiverId));
                               onlineUsersLayout.addView(userTextView);
                            }
                        }
                        scrollView.setVisibility(View.VISIBLE);
                    } else {
                        TextView noUsersTextView = new TextView(Main.this);
                        noUsersTextView.setText("No users are currently online.");
                        noUsersTextView.setTextSize(18);
                        noUsersTextView.setPadding(10, 20, 10, 20);
                        onlineUsersLayout.addView(noUsersTextView);
                        scrollView.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    TextView errorTextView = new TextView(Main.this);
                    errorTextView.setText("Error loading online users: " + error.getMessage());
                    errorTextView.setTextSize(18);
                    errorTextView.setPadding(10, 10, 10, 10);
                    onlineUsersLayout.addView(errorTextView);
                    scrollView.setVisibility(View.VISIBLE);
                }
            });
        }else {
            scrollView.setVisibility(View.INVISIBLE);}
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
                                    if (invitationId != null) {
                                        invitesRef.child(invitationId).setValue(invitationData)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        joinGame(senderId, receiverId);
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
                    Toast.makeText(Main.this,
                            "Failed to retrieve sender's level.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Main.this,
                        "Error retrieving sender's level: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
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
            DatabaseReference invitesRef = FirebaseDatabase.getInstance().getReference("invitations");
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            invitesRef.orderByChild("receiverId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    pendingInvitesLayout.removeAllViews();

                    if (snapshot.exists()) {
                        boolean hasInvites = false;
                        for (DataSnapshot inviteSnapshot : snapshot.getChildren()) {
                            String status = inviteSnapshot.child("status").getValue(String.class);
                            if ("pending".equals(status)) {
                                hasInvites = true;
                                String senderId = inviteSnapshot.child("senderId").getValue(String.class);
                                DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
                                senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                                        String senderName = senderSnapshot.child("displayName").getValue(String.class);
                                        if (senderName == null) senderName = "Unknown User";
                                        LinearLayout inviteLayout = new LinearLayout(Main.this);
                                        inviteLayout.setOrientation(LinearLayout.HORIZONTAL);
                                        inviteLayout.setPadding(10, 20, 10, 20);

                                        TextView inviteTextView = new TextView(Main.this);
                                        inviteTextView.setText(senderName);
                                        inviteTextView.setTextSize(14);
                                        inviteTextView.setTextColor(getResources().getColor(android.R.color.black));

                                        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                                        inviteTextView.setLayoutParams(textParams);

                                        Button acceptButton = new Button(Main.this);
                                        acceptButton.setText("Accept");
                                        acceptButton.setTextSize(14);
                                        String receiverId=userId;
                                        acceptButton.setOnClickListener(v -> {
                                            joinGame( senderId,receiverId);
                                            inviteSnapshot.getRef().removeValue()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(Main.this, "Invitation accepted!", Toast.LENGTH_SHORT).show();
                                                        pendingInvitesLayout.removeView(inviteLayout);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(Main.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        });

                                        Button declineButton = new Button(Main.this);
                                        declineButton.setText("Decline");
                                        declineButton.setTextSize(14);
                                        declineButton.setOnClickListener(v -> {
                                            inviteSnapshot.getRef().removeValue()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(Main.this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                                                        pendingInvitesLayout.removeView(inviteLayout);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(Main.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        });

                                        inviteLayout.addView(inviteTextView);
                                        inviteLayout.addView(acceptButton);
                                        inviteLayout.addView(declineButton);
                                        pendingInvitesLayout.addView(inviteLayout);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(Main.this, "Error loading sender details.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        if (!hasInvites) {
                            TextView noInvitesTextView = new TextView(Main.this);
                            noInvitesTextView.setText("No pending invitations.");
                            noInvitesTextView.setTextSize(18);
                            noInvitesTextView.setPadding(10, 20, 10, 20);
                            pendingInvitesLayout.addView(noInvitesTextView);
                        }

                        scrollView.setVisibility(View.VISIBLE);
                    } else {
                        TextView noInvitesTextView = new TextView(Main.this);
                        noInvitesTextView.setText("No pending invitations.");
                        noInvitesTextView.setTextSize(18);
                        noInvitesTextView.setPadding(10, 20, 10, 20);
                        pendingInvitesLayout.addView(noInvitesTextView);
                        scrollView.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    TextView errorTextView = new TextView(Main.this);
                    errorTextView.setText("Error loading invitations: " + error.getMessage());
                    errorTextView.setTextSize(18);
                    errorTextView.setPadding(10, 10, 10, 10);
                    pendingInvitesLayout.addView(errorTextView);
                    scrollView.setVisibility(View.VISIBLE);
                }
            });
        } else {
            scrollView.setVisibility(View.INVISIBLE);
        }
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
                           String gameId = gamesRef.push().getKey();
                           if (gameId == null) {
                               Toast.makeText(Main.this, "Failed to create a game ID.", Toast.LENGTH_SHORT).show();
                               return;
                           }
                           List<Integer> questionIds = new ArrayList<>();
                           Random random = new Random();
                           while (questionIds.size() < 3) {
                               int randomId = random.nextInt(10) + 1;
                               if (!questionIds.contains(randomId)) {
                                   questionIds.add(randomId);
                               }
                           }
                           while (questionIds.size() < 5) {
                               int randomId = random.nextInt(10) + 11;
                               if (!questionIds.contains(randomId)) {
                                   questionIds.add(randomId);
                               }
                           }
                           while (questionIds.size() < 6) {
                               int randomId = random.nextInt(10) + 21;
                               if (!questionIds.contains(randomId)) {
                                   questionIds.add(randomId);
                               }
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
                           gameData.put("q1", questionIds.get(0));
                           gameData.put("q2", questionIds.get(1));
                           gameData.put("q3", questionIds.get(2));
                           gameData.put("q4", questionIds.get(3));
                           gameData.put("q5", questionIds.get(4));
                           gameData.put("q6", questionIds.get(5));

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
    public  void signOut(View view) {
        database.child("users").child(userId).child("online").setValue(false)
                .addOnCompleteListener(task -> {
                    auth.signOut();
                    startActivity(new Intent(Main.this, MainActivity.class));
                    finish();
                });
    }

}