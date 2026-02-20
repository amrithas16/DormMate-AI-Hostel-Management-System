package com.example.dormmate.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirestoreRepository {
    private final FirebaseFirestore db;
    private final CollectionReference usersCollection;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.usersCollection = db.collection("users");
    }

    public Task<Void> createUserProfile(String uid, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("role", role);
        // Add more fields as needed

        return usersCollection.document(uid).set(user);
    }

    public Task<DocumentSnapshot> getUserProfile(String uid) {
        return usersCollection.document(uid).get();
    }
}
