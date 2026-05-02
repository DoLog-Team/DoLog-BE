package com.dolog.server.global.order;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderProcessor {

    private static final int GAP = 10;

    public int assignNext(List<? extends Orderable> items) {
        if (items.isEmpty()) return GAP;
        return items.get(items.size() - 1).getOrderIndex() + GAP;
    }

    public int calculate(Integer prev, Integer next) {
        if (prev == null && next == null) {
            throw new IllegalArgumentException("둘 다 null이면 안됨");
        }

        if (prev == null) return next - GAP;
        if (next == null) return prev + GAP;

        if (Math.abs(prev - next) <= 1) {
            throw new IllegalStateException("REBALANCE_REQUIRED");
        }

        return (prev + next) / 2;
    }

    public void rebalance(List<? extends Orderable> items) {
        int index = GAP;
        for (Orderable item : items) {
            item.updateOrder(index);
            index += GAP;
        }
    }
}