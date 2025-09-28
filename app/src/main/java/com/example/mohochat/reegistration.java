package com.example.mohochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class reegistration extends AppCompatActivity {
    TextView loginbutton;
    EditText reg_email, reg_password,reg_username;
    Button reg_signup;
    FirebaseAuth auth;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    FirebaseDatabase databsae;
    ProgressDialog progressDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme before setting content view
        SettingsActivity.applyTheme(this);

        setContentView(R.layout.activity_reegistration);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating The Account");
        progressDialog.setCancelable(false);

        databsae = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        loginbutton = findViewById(R.id.textView5 );
        reg_username = findViewById(R.id.editRegUserName);
        reg_email = findViewById(R.id.editRegEmailAddress);
        reg_password = findViewById(R.id.editRegPassword);
        reg_signup = findViewById(R.id.buttonr);


        loginbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(reegistration.this,login.class);
                startActivity(intent);
                finish();
            }
        });

        reg_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = reg_username.getText().toString().trim();
                String email = reg_email.getText().toString();
                String password = reg_password.getText().toString();
                String status = "Hey I'm Using This Application";

                if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
                    progressDialog.dismiss();
                    Toast.makeText(reegistration.this, "Please Enter Valid Information", Toast.LENGTH_SHORT).show();
                } else if (!email.matches(emailPattern)) {
                    progressDialog.dismiss();
                    reg_email.setError("Type A Valid Email Here");
                } else if (fullName.length() < 2) {
                    progressDialog.dismiss();
                    reg_username.setError("Enter valid full name");
                } else if (password.length()<6) {
                    progressDialog.dismiss();
                    reg_password.setError("Password Must be in 6 Character or More");
                }
                else {
                    progressDialog.show();
                    auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                String id = Objects.requireNonNull(task.getResult().getUser()).getUid();
                                DatabaseReference reference =  databsae.getReference().child("user").child(id);

                                Users users = new Users(id,fullName,email,null,password,"default",status);
                                users.setFullName(fullName);
                                reference.setValue(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            progressDialog.dismiss();
                                            Intent intent = new Intent(reegistration.this,ProfileSetupActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                        else {
                                            progressDialog.dismiss();
                                            Toast.makeText(reegistration.this, "Error in Creating The User: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                            else {
                                progressDialog.dismiss();
                                Toast.makeText(reegistration.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}