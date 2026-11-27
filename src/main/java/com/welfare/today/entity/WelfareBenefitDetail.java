package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 상세조회(LcgvWelfaredetailed) 캐시 - WelfareBenefit과 servId로 1:1 매핑된다.
 * llmSummary는 LLM 호출 비용/횟수를 줄이기 위해 결과를 캐싱해두는 컬럼이다.
 */
@Entity
@Table(name = "welfare_benefit_detail")
public class WelfareBenefitDetail {

    @Id
    @Column(name = "serv_id", length = 20)
    private String servId;

    @Lob
    @Column(name = "sprt_trgt_cn", length = WelfareBenefit.LONG_TEXT)
    private String sprtTrgtCn;

    @Lob
    @Column(name = "slct_crit_cn", length = WelfareBenefit.LONG_TEXT)
    private String slctCritCn;

    @Lob
    @Column(name = "alw_serv_cn", length = WelfareBenefit.LONG_TEXT)
    private String alwServCn;

    @Lob
    @Column(name = "aply_mtd_cn", length = WelfareBenefit.LONG_TEXT)
    private String aplyMtdCn;

    @Column(name = "enfc_bgng_ymd", length = 8)
    private String enfcBgngYmd;

    @Column(name = "enfc_end_ymd", length = 8)
    private String enfcEndYmd;

    @Lob
    @Column(name = "llm_summary", length = WelfareBenefit.LONG_TEXT)
    private String llmSummary;

    protected WelfareBenefitDetail() {
    }

    public WelfareBenefitDetail(String servId, String sprtTrgtCn, String slctCritCn, String alwServCn,
                                 String aplyMtdCn, String enfcBgngYmd, String enfcEndYmd, String llmSummary) {
        this.servId = servId;
        this.sprtTrgtCn = sprtTrgtCn;
        this.slctCritCn = slctCritCn;
        this.alwServCn = alwServCn;
        this.aplyMtdCn = aplyMtdCn;
        this.enfcBgngYmd = enfcBgngYmd;
        this.enfcEndYmd = enfcEndYmd;
        this.llmSummary = llmSummary;
    }

    public String getServId() {
        return servId;
    }

    public String getSprtTrgtCn() {
        return sprtTrgtCn;
    }

    public String getSlctCritCn() {
        return slctCritCn;
    }

    public String getAlwServCn() {
        return alwServCn;
    }

    public String getAplyMtdCn() {
        return aplyMtdCn;
    }

    public String getEnfcBgngYmd() {
        return enfcBgngYmd;
    }

    public String getEnfcEndYmd() {
        return enfcEndYmd;
    }

    public String getLlmSummary() {
        return llmSummary;
    }

    public void updateLlmSummary(String llmSummary) {
        this.llmSummary = llmSummary;
    }
}
