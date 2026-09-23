package com.welfare.today.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * 지자체복지서비스 목록조회(LcgvWelfarelist) 응답.
 * 실제 응답 샘플로 루트/래퍼 태그명은 배치 연동 시 검증 필요.
 */
@JacksonXmlRootElement(localName = "wantedList")
public record WelfareListResponse(
        int totalCount,
        int pageNo,
        int numOfRows,
        int resultCode,
        String resultMessage,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "servList")
        List<WelfareItem> servList
) {
}
