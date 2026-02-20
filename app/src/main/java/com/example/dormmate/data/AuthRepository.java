package com.example.dormmate.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public Task<AuthResult> signUp(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> login(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public void signOut() {
        firebaseAuth.signOut();
    }
}
