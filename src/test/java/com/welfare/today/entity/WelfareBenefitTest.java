package com.welfare.today.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareBenefitTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Test
    void updatedWithinThirtyDaysIsRecent() {
        assertThat(withLastMod(daysAgo(0)).isRecentlyUpdated()).isTrue();
        assertThat(withLastMod(daysAgo(30)).isRecentlyUpdated()).isTrue();
    }

    @Test
    void updatedLongerAgoIsNotRecent() {
        assertThat(withLastMod(daysAgo(31)).isRecentlyUpdated()).isFalse();
    }

    @Test
    void missingOrMalformedDateIsNotRecent() {
        assertThat(withLastMod(null).isRecentlyUpdated()).isFalse();
        assertThat(withLastMod("2026-11-01").isRecentlyUpdated()).isFalse();
        assertThat(withLastMod("").isRecentlyUpdated()).isFalse();
    }

    @Test
    void newForThreeDaysIncludingTheDayItArrived() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        assertThat(firstSeen(now).isNew()).isTrue();
        assertThat(firstSeen(now.minusDays(2)).isNew()).isTrue();
        assertThat(firstSeen(now.minusDays(3)).isNew()).isFalse();
        assertThat(firstSeen(null).isNew()).isFalse();
    }

    private static WelfareBenefit firstSeen(LocalDateTime firstSeenAt) {
        WelfareBenefit benefit = withLastMod("20260101");
        benefit.markFirstSeen(firstSeenAt);
        return benefit;
    }

    private static String daysAgo(int days) {
        return TODAY.minusDays(days).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private static WelfareBenefit withLastMod(String lastModYmd) {
        return new WelfareBenefit("ID", "혜택", null, null, null, null, null, null, null,
                null, lastModYmd, null, null, null, LocalDateTime.now());
    }
}
