package com.welfare.today.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 지자체복지서비스 상세조회(LcgvWelfaredetailed) 응답.
 * 목록조회(wantedList/servList)와 같은 규칙으로 루트 태그를 wantedDtl로 가정했다 -
 * 실제 응답 샘플로 검증 필요 (문의처/관련사이트/근거법령/서식 리스트는 MVP에서 사용하지 않아 매핑 생략).
 */
@JacksonXmlRootElement(localName = "wantedDtl")
public record WelfareDetailResponse(
        int resultCode,
        String resultMessage,
        String servId,
        String servNm,
        String servDgst,
        String ctpvNm,
        String sggNm,
        String bizChrDeptNm,
        String lifeNmArray,
        String trgterIndvdlNmArray,
        String intrsThemaNmArray,
        String sprtCycNm,
        String srvPvsnNm,
        String aplyMtdNm,
        String sprtTrgtCn,
        String slctCritCn,
        String alwServCn,
        String aplyMtdCn,
        String enfcBgngYmd,
        String enfcEndYmd,
        String lastModYmd
) {
}
