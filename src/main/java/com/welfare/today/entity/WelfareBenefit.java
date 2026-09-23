package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 목록조회(LcgvWelfarelist) 캐시 - 배치로 미리 적재해 로컬 DB에서 조회한다.
 */
@Entity
@Table(name = "welfare_benefit")
public class WelfareBenefit {

    @Id
    @Column(name = "serv_id", length = 20)
    private String servId;

    @Column(name = "serv_nm", nullable = false, length = 200)
    private String servNm;

    @Lob
    @Column(name = "serv_dgst")
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
}
