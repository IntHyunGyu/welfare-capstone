package com.welfare.today.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로필 입력 폼(@ModelAttribute) 바인딩 전용 클래스.
 * Spring MVC의 체크박스 다중값 바인딩은 기본 생성자 + setter가 있는
 * 평범한 POJO에서 가장 안정적으로 동작하므로, 불변 레코드(WelfareProfile)와 분리했다.
 */
public class WelfareProfileForm {

    private List<String> lifeCodes = new ArrayList<>();
    private List<String> householdCodes = new ArrayList<>();
    private List<String> themeCodes = new ArrayList<>();
    private Integer age;
    private String ctpvNm;
    private String sggNm;

    public List<String> getLifeCodes() {
        return lifeCodes;
    }

    public void setLifeCodes(List<String> lifeCodes) {
        this.lifeCodes = lifeCodes;
    }

    public List<String> getHouseholdCodes() {
        return householdCodes;
    }

    public void setHouseholdCodes(List<String> householdCodes) {
        this.householdCodes = householdCodes;
    }

    public List<String> getThemeCodes() {
        return themeCodes;
    }

    public void setThemeCodes(List<String> themeCodes) {
        this.themeCodes = themeCodes;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCtpvNm() {
        return ctpvNm;
    }

    public void setCtpvNm(String ctpvNm) {
        this.ctpvNm = ctpvNm;
    }

    public String getSggNm() {
        return sggNm;
    }

    public void setSggNm(String sggNm) {
        this.sggNm = sggNm;
    }

    /** 세션에 저장된 프로필로 폼을 다시 채울 때 쓴다 ("조건 다시 입력" 시 이전 선택 유지). */
    public static WelfareProfileForm from(WelfareProfile profile) {
        WelfareProfileForm form = new WelfareProfileForm();
        if (profile == null) {
            return form;
        }
        form.setLifeCodes(new ArrayList<>(nullToEmpty(profile.lifeCodes())));
        form.setHouseholdCodes(new ArrayList<>(nullToEmpty(profile.householdCodes())));
        form.setThemeCodes(new ArrayList<>(nullToEmpty(profile.themeCodes())));
        form.setAge(profile.age());
        form.setCtpvNm(profile.ctpvNm());
        form.setSggNm(profile.sggNm());
        return form;
    }

    /** 분류 칩도 시도도 하나도 안 골랐는지 — 이러면 전체 혜택이 한꺼번에 나와 매칭 의미가 없다. */
    public boolean hasNoCondition() {
        return nullToEmpty(lifeCodes).isEmpty()
                && nullToEmpty(householdCodes).isEmpty()
                && nullToEmpty(themeCodes).isEmpty()
                && (ctpvNm == null || ctpvNm.isBlank());
    }

    private static List<String> nullToEmpty(List<String> list) {
        return list == null ? List.of() : list;
    }

    public WelfareProfile toProfile() {
        return new WelfareProfile(lifeCodes, householdCodes, themeCodes, age, ctpvNm, sggNm);
    }
}
