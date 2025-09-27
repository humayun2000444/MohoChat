package com.example.mohochat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class reegistration extends AppCompatActivity {
    TextView loginbutton;
    EditText reg_email, reg_password,reg_username;
    Button reg_signup;
    CircleImageView reg_profileimage;
    FirebaseAuth auth;
    Uri imageURI;
    String imageUri;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    FirebaseDatabase databsae;
    FirebaseStorage storage;
    ProgressDialog progressDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reegistration);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating The Account");
        progressDialog.setCancelable(false);

        databsae = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        loginbutton = findViewById(R.id.textView5 );
        reg_username = findViewById(R.id.editRegUserName);
        reg_email = findViewById(R.id.editRegEmailAddress);
        reg_password = findViewById(R.id.editRegPassword);
        reg_profileimage = findViewById(R.id.profilerg0);
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
                String userName = reg_username.getText().toString();
                String email = reg_email.getText().toString();
                String password = reg_password.getText().toString();
                String status = "Hey I'm Using This Application";

                if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
                    progressDialog.dismiss();
                    Toast.makeText(reegistration.this, "Please Enter Valid Information", Toast.LENGTH_SHORT).show();
                } else if (!email.matches(emailPattern)) {
                    progressDialog.dismiss();
                    reg_email.setError("Type A Valid Email Here");
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
                                StorageReference storageReference = storage.getReference().child("Upload").child(id);

                                if(imageURI!= null){
                                    storageReference.putFile(imageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            if (task.isSuccessful()){
                                                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        imageUri = uri.toString();
                                                        Users users = new Users(id,userName,email,password,imageUri,status);
                                                        reference.setValue(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    progressDialog.dismiss();
                                                                    Intent intent = new Intent(reegistration.this,MainActivityNew.class);
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
                                                });
                                            }
                                            else {
                                                progressDialog.dismiss();
                                                Toast.makeText(reegistration.this, "Error uploading image: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                                else {
                                    String status = "Hey I'm Using This Application";
                                    imageUri = "https://firebasestorage.googleapis.com/v0/b/mohochat-f5560.appspot.com/o/man.png?alt=media&token=a9abcaf4-9c30-41df-a126-9f73749a952f";
                                    Users users = new Users(id,userName,email,password,imageUri,status);
                                    reference.setValue(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                progressDialog.dismiss();
                                                Intent intent = new Intent(reegistration.this,MainActivityNew.class);
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

        reg_profileimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),10);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==10){
            if(data!=null){
                imageURI = data.getData();
                reg_profileimage.setImageURI(imageURI);
            }
        }
    }
}