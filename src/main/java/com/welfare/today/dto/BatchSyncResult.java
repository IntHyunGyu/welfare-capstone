package com.welfare.today.dto;

import java.time.LocalDateTime;

public record BatchSyncResult(
        int fetchedCount,
        int totalCount,
        int pageCount,
        boolean truncatedByMaxPages,
        LocalDateTime fetchedAt
) {
}
