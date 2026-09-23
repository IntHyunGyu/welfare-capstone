package com.welfare.today.dto;

public record WelfareItem(
        String servId,
        String servNm,
        String servDgst,
        String ctpvNm,
        String sggNm,
        String bizChrDeptNm,
        String aplyMtdNm,
        String sprtCycNm,
        String srvPvsnNm,
        String servDtlLink,
        String lastModYmd,
        String lifeNmArray,
        String trgterIndvdlNmArray,
        String intrsThemaNmArray
) {
}
