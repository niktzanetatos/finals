package com.example.f1quizgame.niktza4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    EditText emailText, passwordText, nameText;
    FirebaseAuth auth;
    FirebaseUser user;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main4), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        emailText = findViewById(R.id.editTextText);
        passwordText = findViewById(R.id.editTextText2);
        nameText = findViewById(R.id.editTextText3);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
    }

    public void signUp(View view) {
        if (!emailText.getText().toString().isEmpty()
                && !passwordText.getText().toString().isEmpty() && !nameText.getText().toString().isEmpty()) {

            String email = emailText.getText().toString();
            String password = passwordText.getText().toString();
            String displayName = nameText.getText().toString();

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                user = auth.getCurrentUser();
                                if (user != null) {
                                    String userId = user.getUid();
                                    database.child("Statistics").child("userCount").runTransaction(new Transaction.Handler() {
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
                                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {

                                        }
                                    });
                                    database.child("users").child(userId).child("displayName").setValue(displayName);
                                    database.child("users").child(userId).child("score").setValue(0);
                                    database.child("users").child(userId).child("level").setValue(1);
                                    database.child("users").child(userId).child("online").setValue(true);
                                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                                    database.child("users").child(userId).child("date").setValue(currentDate);
                                    database.child("users").child(userId).child("online").onDisconnect().setValue(false);
                                    Intent intent = new Intent(MainActivity.this, Main.class);
                                    intent.putExtra("displayName", displayName);
                                    startActivity(intent);
                                }
                            } else {
                                showMessage("Error", task.getException().getLocalizedMessage());
                            }
                        }
                    });
        } else {
            showMessage("Error", "Please provide data to the fields");
        }
    }

    void showMessage(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setCancelable(true).show();
    }

    public void goTosignIn(View view) {
        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
        startActivity(intent);
    }
}