package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 목록조회(LcgvWelfarelist) 캐시 - 배치로 미리 적재해 로컬 DB에서 조회한다.
 */
@Entity
@Table(name = "welfare_benefit")
public class WelfareBenefit {

    // @Lob만 쓰면 Hibernate가 MySQL에서 tinytext(255바이트)로 만들어 한글 설명이 잘린다 - mediumtext가 되도록 길이 지정.
    static final int LONG_TEXT = 16_777_215;
    private static final int RECENT_UPDATE_DAYS = 30;
    public static final int NEW_DAYS = 3;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Id
    @Column(name = "serv_id", length = 20)
    private String servId;

    @Column(name = "serv_nm", nullable = false, length = 200)
    private String servNm;

    @Lob
    @Column(name = "serv_dgst", length = LONG_TEXT)
    private String servDgst;

    @Column(name = "ctpv_nm", length = 50)
    private String ctpvNm;

    @Column(name = "sgg_nm", length = 50)
    private String sggNm;

    @Column(name = "biz_chr_dept_nm", length = 200)
    private String bizChrDeptNm;

    @Column(name = "aply_mtd_nm", length = 100)
    private String aplyMtdNm;

    @Column(name = "sprt_cyc_nm", length = 50)
    private String sprtCycNm;

    @Column(name = "srv_pvsn_nm", length = 100)
    private String srvPvsnNm;

    @Column(name = "serv_dtl_link", length = 500)
    private String servDtlLink;

    @Column(name = "last_mod_ymd", length = 8)
    private String lastModYmd;

    @Column(name = "life_nm_array", length = 200)
    private String lifeNmArray;

    @Column(name = "trgter_indvdl_nm_array", length = 200)
    private String trgterIndvdlNmArray;

    @Column(name = "intrs_thema_nm_array", length = 200)
    private String intrsThemaNmArray;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    /**
     * 우리 DB에 처음 들어온 시각 (NEW 표시용). 배치가 처음 본 혜택에만 기록하고 이후 수집에서도 유지한다.
     * 이 컬럼이 생기기 전부터 있던 혜택과, 빈 DB를 처음 채운 수집분은 언제 생긴 건지 몰라 비워 둔다(= NEW 아님).
     */
    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    protected WelfareBenefit() {
    }

    public WelfareBenefit(String servId, String servNm, String servDgst, String ctpvNm, String sggNm,
                           String bizChrDeptNm, String aplyMtdNm, String sprtCycNm, String srvPvsnNm,
                           String servDtlLink, String lastModYmd, String lifeNmArray,
                           String trgterIndvdlNmArray, String intrsThemaNmArray, LocalDateTime fetchedAt) {
        this.servId = servId;
        this.servNm = servNm;
        this.servDgst = servDgst;
        this.ctpvNm = ctpvNm;
        this.sggNm = sggNm;
        this.bizChrDeptNm = bizChrDeptNm;
        this.aplyMtdNm = aplyMtdNm;
        this.sprtCycNm = sprtCycNm;
        this.srvPvsnNm = srvPvsnNm;
        this.servDtlLink = servDtlLink;
        this.lastModYmd = lastModYmd;
        this.lifeNmArray = lifeNmArray;
        this.trgterIndvdlNmArray = trgterIndvdlNmArray;
        this.intrsThemaNmArray = intrsThemaNmArray;
        this.fetchedAt = fetchedAt;
    }

    public String getServId() {
        return servId;
    }

    public String getServNm() {
        return servNm;
    }

    public String getServDgst() {
        return servDgst;
    }

    public String getCtpvNm() {
        return ctpvNm;
    }

    public String getSggNm() {
        return sggNm;
    }

    public String getBizChrDeptNm() {
        return bizChrDeptNm;
    }

    public String getAplyMtdNm() {
        return aplyMtdNm;
    }

    public String getSprtCycNm() {
        return sprtCycNm;
    }

    public String getSrvPvsnNm() {
        return srvPvsnNm;
    }

    public String getServDtlLink() {
        return servDtlLink;
    }

    public String getLastModYmd() {
        return lastModYmd;
    }

    public String getLifeNmArray() {
        return lifeNmArray;
    }

    public String getTrgterIndvdlNmArray() {
        return trgterIndvdlNmArray;
    }

    public String getIntrsThemaNmArray() {
        return intrsThemaNmArray;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void markFirstSeen(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    /** 처음 들어온 날을 포함해 3일간(오늘, 어제, 그제 들어온 혜택) NEW. */
    public boolean isNew() {
        return firstSeenAt != null
                && !firstSeenAt.toLocalDate().isBefore(LocalDate.now(SEOUL).minusDays(NEW_DAYS - 1));
    }

    public boolean isRecentlyUpdated() {
        if (lastModYmd == null) {
            return false;
        }
        try {
            LocalDate modified = LocalDate.parse(lastModYmd, DateTimeFormatter.BASIC_ISO_DATE);
            return !modified.isBefore(LocalDate.now(SEOUL).minusDays(RECENT_UPDATE_DAYS));
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
