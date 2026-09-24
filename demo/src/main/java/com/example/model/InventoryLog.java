package com.example.model;

/** Một lần biến động số lượng bản sao trong kho. */
public record InventoryLog(int id, int bookId, String action, int quantity,
                           String reason, int userId, String userName, String createdAt) {}
