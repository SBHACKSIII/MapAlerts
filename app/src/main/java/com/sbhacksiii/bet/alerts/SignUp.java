package com.sbhacksiii.bet.alerts;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUp extends AppCompatActivity implements View.OnClickListener {

    private Button registerButton;
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private TextView signInTextView, registeringTextView, guestTextView;

    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            // Start main activity
            startActivity(new Intent(getApplicationContext(), MainActivity.class).putExtra("userId", firebaseAuth.getCurrentUser().getUid()));
        }

        registerButton = (Button) findViewById(R.id.registerButton);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        confirmPasswordEditText = (EditText) findViewById(R.id.confirmPasswordEditText);
        signInTextView = (TextView) findViewById(R.id.signInTextView);
        registeringTextView = (TextView) findViewById(R.id.registeringTextView);
        guestTextView = (TextView) findViewById(R.id.guestTextView);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        registerButton.setOnClickListener(this);
        signInTextView.setOnClickListener(this);
        guestTextView.setOnClickListener(this);
    }

    private void registerUser() {
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            // Email is empty
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            // Stopping the fnx exec further
            return;
        }
        if (TextUtils.isEmpty(password)) {
            // password is empty
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            // Stopping the function execution further
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            // Confirmed password is empty
            Toast.makeText(this, "Please Re-enter the password", Toast.LENGTH_SHORT).show();
            // Stopping the function execution further
            return;
        }

        // If Email and password are valid
        if (passwordIsValid(password, confirmPassword)) {
            progressBar.setVisibility(View.VISIBLE);
            registeringTextView.setVisibility(View.VISIBLE);

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBar.setVisibility(View.INVISIBLE);
                            registeringTextView.setVisibility(View.INVISIBLE);

                            if (task.isSuccessful()) {
                                // User successfully registered and logged in
                                // We will start the profile activity here
                                // Making a Toast for now, will change later
                                Toast.makeText(SignUp.this, "Registered Successfully", Toast.LENGTH_SHORT).show();

                                startActivity(new Intent(SignUp.this, MainActivity.class)
                                        .putExtra("USERUID", firebaseAuth.getCurrentUser().getUid()));
                            } else {
                                Toast.makeText(SignUp.this, "Could not register. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private boolean passwordIsValid(String password, String confirmPassword) {
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Your password and confirmation password does not match, please try again",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == registerButton) {
            registerUser();
        }
        if (view == signInTextView) {
            // Will open login Activity

            startActivity(new Intent(this, LoginActivity.class));
        }
        if (view == guestTextView) {

            startActivity(new Intent(this, MainActivity.class).putExtra("userId", ""));
        }
    }
}
