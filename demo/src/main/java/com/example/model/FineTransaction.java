package com.example.model;

/** Giao dịch thu phạt hoặc miễn/giảm phạt. */
public record FineTransaction(int id, int borrowId, int readerId, double amount,
                              String type, String reason, int userId,
                              String userName, String receiptNo, String createdAt) {}
