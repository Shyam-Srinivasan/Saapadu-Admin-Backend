package com.saapadu.admin.service;

import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    /**
     * A helper method to asynchronously fetch a DataSnapshot from a Firebase DatabaseReference.
     * This encapsulates the boilerplate of using a ValueEventListener with a CompletableFuture.
     *
     * @param ref The DatabaseReference to fetch data from.
     * @return A CompletableFuture that will be completed with the DataSnapshot.
     */
    private CompletableFuture<DataSnapshot> getDataSnapshot(DatabaseReference ref) {
        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                future.complete(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    /**
     * A generic helper method to process orders related to a specific college.
     * It fetches all shops and all orders, identifies the shops belonging to the college,
     * and then applies a given processing function to the orders of those shops.
     *
     * @param collegeId The ID of the college.
     * @param processor A BiFunction that takes the orders DataSnapshot and a Set of relevant shop IDs,
     *                  and returns a result of type T.
     * @param <T>       The type of the result returned by the processor.
     * @return A CompletableFuture containing the result of the processing.
     */
    private <T> CompletableFuture<T> processOrdersForCollege(Long collegeId, BiFunction<DataSnapshot, Set<String>, T> processor) {
        DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("shops");
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        CompletableFuture<DataSnapshot> shopsFuture = getDataSnapshot(shopsRef);
        CompletableFuture<DataSnapshot> ordersFuture = getDataSnapshot(ordersRef);

        return CompletableFuture.allOf(shopsFuture, ordersFuture).thenApply(v -> {
            DataSnapshot shopsSnapshot = shopsFuture.join();
            DataSnapshot ordersSnapshot = ordersFuture.join();

            // Find all shop IDs for the given college ID
            Set<String> relevantShopIds = new HashSet<>();
            if (shopsSnapshot.exists()) {
                for (DataSnapshot shop : shopsSnapshot.getChildren()) {
                    String dbCollegeId = shop.child("college_id").getValue(String.class);
                    if (dbCollegeId != null && dbCollegeId.equals(String.valueOf(collegeId))) {
                        relevantShopIds.add(shop.getKey());
                    }
                }
            }

            // Apply the specific processing logic
            return processor.apply(ordersSnapshot, relevantShopIds);
        });
    }

    public CompletableFuture<Long> getTotalOrdersByCollegeId(Long collegeId) {
        return processOrdersForCollege(collegeId, (ordersSnapshot, relevantShopIds) -> {
            long count = 0;
            if (ordersSnapshot.exists()) {
                for (DataSnapshot order : ordersSnapshot.getChildren()) {
                    String orderShopId = order.child("shop_id").getValue(String.class);
                    if (orderShopId != null && relevantShopIds.contains(orderShopId)) {
                        count++;
                    }
                }
            }
            return count;
        });
    }

    public CompletableFuture<Long> getPendingOrdersByCollegeId(Long collegeId) {
        return processOrdersForCollege(collegeId, (ordersSnapshot, relevantShopIds) -> {
            long count = 0;
            if (ordersSnapshot.exists()) {
                for (DataSnapshot order : ordersSnapshot.getChildren()) {
                    String orderShopId = order.child("shop_id").getValue(String.class);
                    Boolean isPurchased = order.child("isPurchased").getValue(Boolean.class);
                    if (orderShopId != null && relevantShopIds.contains(orderShopId) && isPurchased != null && !isPurchased) {
                        count++;
                    }
                }
            }
            return count;
        });
    }

    public CompletableFuture<Long> getCompletedOrdersByCollegeId(Long collegeId) {
        return processOrdersForCollege(collegeId, (ordersSnapshot, relevantShopIds) -> {
            long count = 0;
            if (ordersSnapshot.exists()) {
                for (DataSnapshot order : ordersSnapshot.getChildren()) {
                    String orderShopId = order.child("shop_id").getValue(String.class);
                    Boolean isPurchased = order.child("isPurchased").getValue(Boolean.class);
                    if (orderShopId != null && relevantShopIds.contains(orderShopId) && isPurchased != null && isPurchased) {
                        count++;
                    }
                }
            }
            return count;
        });
    }

    public CompletableFuture<BigDecimal> getTotalRevenueByCollegeId(Long collegeId) {
        return processOrdersForCollege(collegeId, (ordersSnapshot, relevantShopIds) -> {
            BigDecimal amount = BigDecimal.ZERO;
            if (ordersSnapshot.exists()) {
                for (DataSnapshot order : ordersSnapshot.getChildren()) {
                    String orderShopId = order.child("shop_id").getValue(String.class);
                    Boolean isPurchased = order.child("isPurchased").getValue(Boolean.class);

                    // Revenue should only be calculated for completed (purchased) orders.
                    if (orderShopId != null && relevantShopIds.contains(orderShopId) && isPurchased != null && isPurchased) {
                        // In Firebase JSON, numbers can be Long or Double. Reading as Number is safest.
                        // Make the code robust against data type inconsistencies (e.g., amount stored as a string).
                        Object rawAmount = order.child("total_amount").getValue();
                        if (rawAmount instanceof Number) {
                            amount = amount.add(new BigDecimal(rawAmount.toString()));
                        } else if (rawAmount instanceof String) {
                            try {
                                amount = amount.add(new BigDecimal((String) rawAmount));
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse 'total_amount' string '{}' for order key '{}'", rawAmount, order.getKey());
                            }
                        } else if (rawAmount != null) {
                            logger.warn("Unexpected type for 'total_amount' ({}) for order key '{}'", rawAmount.getClass().getName(), order.getKey());
                        }
                    }
                }
            }
            return amount;
        });
    }

}
