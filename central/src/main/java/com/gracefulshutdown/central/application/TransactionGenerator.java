package com.gracefulshutdown.central.application;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class TransactionGenerator {
    private static final List<String> TRANSACTION_TYPES = Arrays.asList("T1", "T2", "S1");
    
    private static final java.util.Map<String, List<String>> NARRATIONS = java.util.Map.ofEntries(
        java.util.Map.entry("T1", Arrays.asList(
            "Purchase of Office Supplies",
            "Equipment maintenance",
            "Software license renewal"
        )),
        java.util.Map.entry("T2", Arrays.asList(
            "Vendor payment",
            "Contractor invoice",
            "Service agreement payment"
        )),
        java.util.Map.entry("S1", Arrays.asList(
            "Refund processing",
            "Credit settlement",
            "Adjustment reversal"
        ))
    );

    private final Random random = new Random();
    private int txnSequence = 0;

    public String generateTxnId() {
        return String.format("TXN-%06d", ++txnSequence);
    }

    public String generateStan() {
        int length = 6 + random.nextInt(7);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public String generateTxnType() {
        return TRANSACTION_TYPES.get(random.nextInt(TRANSACTION_TYPES.size()));
    }

    public String generateNarration(String txnType) {
        List<String> narrations = NARRATIONS.get(txnType);
        return narrations.get(random.nextInt(narrations.size()));
    }

    public int generateQuantity() {
        return 1 + random.nextInt(100);
    }

    public double generateRate() {
        return 10.0 + (random.nextDouble() * 989.99);
    }

    public double calculateTaxes(int quantity, double rate) {
        return (quantity * rate) * 0.10;
    }

    public double calculateTotal(int quantity, double rate, double taxes) {
        return (quantity * rate) + taxes;
    }

    public void reset() {
        txnSequence = 0;
    }
}
