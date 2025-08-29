package com.saapadu.admin.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.*;
import com.saapadu.admin.SignUpRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class SignUpService {
    public CompletableFuture<String> createOrganization(SignUpRequest request){
        CompletableFuture<String> future = new CompletableFuture<>();;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("colleges");
        
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long maxId = 0;
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        try {
                            long id = Long.parseLong(snapshot.getKey());
                            if(id > maxId){
                                maxId = id;
                            }
                        } catch (NumberFormatException e){
                            
                        }
                    }
                }
                String newCollegeId = String.valueOf(maxId + 1);
                DatabaseReference orgRef = dbRef.child(newCollegeId);
                
                Map<String, Object> collegeData = new LinkedHashMap<>();
                collegeData.put("college_name", request.collegeName);
                collegeData.put("address", request.address);
                collegeData.put("domain_address", request.domainAddress);
                collegeData.put("email_id", request.emailId);
                try{
                    collegeData.put("contact_no", Long.parseLong(request.contactNo));
                } catch (NumberFormatException e) {
                    future.completeExceptionally(new IllegalArgumentException("Contact number must be a valid number."));
//                    return maxId;
                }

                ApiFuture<Void> apiFuture = orgRef.setValueAsync(collegeData);
                apiFuture.addListener(()->{
                    try {
                        apiFuture.get();
                        future.complete(newCollegeId);
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                }, Runnable::run );
//                return maxId;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

}
