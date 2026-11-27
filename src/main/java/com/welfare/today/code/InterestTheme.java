package com.welfare.today.code;

/**
 * 관심주제 코드 (intrsThemaArray). 라벨은 API 응답 intrsThemaNmArray 표기와 글자까지 같아야 매칭된다.
 * 2026-11 실데이터에는 정신건강/문화·여가/법률로 분류된 지자체 혜택이 없어 단독 선택 시 0건이 정상이다.
 */
public enum InterestTheme {
    PHYSICAL_HEALTH("010", "신체건강"),
    MENTAL_HEALTH("020", "정신건강"),
    LIVING_SUPPORT("030", "생활지원"),
    HOUSING("040", "주거"),
    JOBS("050", "일자리"),
    CULTURE_LEISURE("060", "문화·여가"),
    SAFETY_CRISIS("070", "안전·위기"),
    PREGNANCY_BIRTH("080", "임신·출산"),
    CHILDCARE("090", "보육"),
    EDUCATION("100", "교육"),
    ADOPTION_FOSTER("110", "입양·위탁"),
    CARE_PROTECTION("120", "보호·돌봄"),
    MICROFINANCE("130", "서민금융"),
    LEGAL("140", "법률");

    private final String code;
    private final String label;

    InterestTheme(String code, String label) {
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
        for (InterestTheme theme : values()) {
            if (theme.code.equals(code)) {
                return theme.label;
            }
        }
        return null;
    }
}
