package com.welfare.today.dto;

import java.time.LocalDateTime;

/**
 * @param newCount 이번 수집에서 새로 들어와 NEW로 표시될 혜택 수 (빈 DB를 처음 채운 수집이면 0)
 */
public record BatchSyncResult(
        int fetchedCount,
        int totalCount,
        int pageCount,
        boolean truncatedByMaxPages,
        LocalDateTime fetchedAt,
        int newCount
) {
}
