package com.welfare.today.code;

/** 가구상황 코드 (trgterIndvdlArray). 라벨은 API 응답 trgterIndvdlNmArray 표기와 글자까지 같아야 매칭된다. */
public enum HouseholdSituation {
    MULTICULTURAL("010", "다문화·탈북민"),
    MULTI_CHILDREN("020", "다자녀"),
    VETERAN("030", "보훈대상자"),
    DISABLED("040", "장애인"),
    LOW_INCOME("050", "저소득"),
    SINGLE_PARENT("060", "한부모·조손");

    private final String code;
    private final String label;

    HouseholdSituation(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOfCode(String code) {
        for (HouseholdSituation situation : values()) {
            if (situation.code.equals(code)) {
                return situation.label;
            }
        }
        return null;
    }
}
