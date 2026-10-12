package com.example.fitman.calendar;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class DirectoryHelper {
    private DirectoryHelper() {}

    /** Call once after login so friends can find this user by email. */
    public static void registerSelf(FirebaseUser user) {
        if (user == null || user.getEmail() == null) return;
        Map<String, Object> entry = new HashMap<>();
        entry.put("uid", user.getUid());
        entry.put("email", user.getEmail());
        entry.put("name", user.getDisplayName());
        FirebaseFirestore.getInstance()
                .collection("userDirectory")
                .document(user.getEmail().toLowerCase())
                .set(entry, SetOptions.merge());
    }
}