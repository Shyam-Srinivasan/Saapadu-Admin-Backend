package com.saapadu.admin.service;

import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SignInService {
    
    public CompletableFuture<String> signInWithCollegeName(String collegeName) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        DatabaseReference collegesRef = FirebaseDatabase.getInstance().getReference("colleges");

        Query query = collegesRef.orderByChild("college_name").equalTo(collegeName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Assuming college_name is unique, take the first result.
                    DataSnapshot collegeSnapshot = dataSnapshot.getChildren().iterator().next();
                    future.complete(collegeSnapshot.getKey());
                } else {
                    future.complete(null); // No college found
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }
}
