package com.welfare.today.code;

/**
 * 생애주기 코드 (lifeArray). 라벨은 API 응답 lifeNmArray 표기와 글자까지 같아야 매칭된다 -
 * 생애주기의 임신·출산만 가운뎃점 양옆에 공백이 있다 (관심주제의 임신·출산은 공백 없음).
 */
public enum LifeStage {
    INFANT("001", "영유아"),
    CHILD("002", "아동"),
    TEEN("003", "청소년"),
    YOUTH("004", "청년"),
    MIDDLE_AGED("005", "중장년"),
    SENIOR("006", "노년"),
    PREGNANCY("007", "임신 · 출산");

    private final String code;
    private final String label;

    LifeStage(String code, String label) {
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
        for (LifeStage stage : values()) {
            if (stage.code.equals(code)) {
                return stage.label;
            }
        }
        return null;
    }
}
