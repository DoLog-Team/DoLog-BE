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

    /**
     * 안전 계산 (절대 예외 안 던짐)
     */
    public Integer calculate(Integer prev, Integer next) {
        if (prev == null && next == null) {
            return null;
        }

        if (prev == null) return next - GAP;
        if (next == null) return prev + GAP;

        // 공간 부족 → null 반환 (rebalance 필요)
        if (Math.abs(prev - next) <= 1) {
            return null;
        }

        return (prev + next) / 2;
    }

    public boolean needsRebalance(Integer prev, Integer next) {
        return calculate(prev, next) == null;
    }

    /**
     * 전체 재정렬 (항상 GAP 기준)
     */
    public void rebalance(List<? extends Orderable> items) {
        int index = GAP;

        for (Orderable item : items) {
            item.updateOrder(index);
            index += GAP;
        }
    }
}