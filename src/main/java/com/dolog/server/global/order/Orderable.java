package com.dolog.server.global.order;

public interface Orderable {
    Integer getOrderIndex();
    void updateOrder(Integer orderIndex);
}