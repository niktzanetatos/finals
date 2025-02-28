package com.example.f1quizgame.niktza4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
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
                                    database.child("users").child(userId).child("displayName").setValue(displayName);
                                    database.child("users").child(userId).child("score").setValue(0);
                                    database.child("users").child(userId).child("online").setValue(true);
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
    public void goTosignIn(View view){
        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
        startActivity(intent);
    }
}