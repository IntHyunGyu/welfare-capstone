package com.welfare.today.code;

/** 가구상황 코드 (trgterIndvdlArray) */
public enum HouseholdSituation {
    MULTICULTURAL("010", "다문화·탈북민"),
    MULTI_CHILDREN("020", "다자녀"),
    PROTECTED("030", "보호대상자"),
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
