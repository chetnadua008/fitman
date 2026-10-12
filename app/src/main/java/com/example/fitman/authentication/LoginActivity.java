package com.example.fitman.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.fitman.MainActivity;
import com.example.fitman.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private Button btn;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) { openMain(); return; }

        setContentView(R.layout.activity_login);
        btn = findViewById(R.id.btnGoogle);
        progress = findViewById(R.id.progress);
        btn.setOnClickListener(v -> startSignIn());
    }

    private void startSignIn() {
        setLoading(true);
        CredentialManager cm = CredentialManager.create(this);

        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption
                .Builder(getString(R.string.default_web_client_id)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option).build();

        cm.getCredentialAsync(this, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential c = result.getCredential();
                        if (c instanceof CustomCredential && c.getType()
                                .equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            GoogleIdTokenCredential g = GoogleIdTokenCredential.createFrom(c.getData());
                            firebaseLogin(g.getIdToken());
                        } else {
                            fail("Unexpected credential type");
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e("Login", "Credential error", e);
                        fail("Sign-in failed: " + e.getMessage());
                    }
                });
    }

    private void firebaseLogin(String idToken) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(cred).addOnCompleteListener(this, t -> {
            if (t.isSuccessful()) openMain();
            else fail("Firebase login failed: " + t.getException());
        });
    }

    private void fail(String msg) {
        setLoading(false);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean on) {
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        btn.setEnabled(!on);
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}