package com.welfare.today.dto;

import java.io.Serializable;
import java.util.List;

/** HttpSession에 임시 보관하는 사용자 프로필 (로그인 없이 매 방문마다 새로 입력). */
public record WelfareProfile(
        List<String> lifeCodes,
        List<String> householdCodes,
        List<String> themeCodes,
        Integer age,
        String ctpvNm,
        String sggNm
) implements Serializable {
}
