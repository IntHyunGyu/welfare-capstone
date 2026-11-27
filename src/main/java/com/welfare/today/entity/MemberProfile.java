package com.welfare.today.entity;

import com.welfare.today.dto.WelfareProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 회원이 저장해 둔 조건 (회원 1명당 1행, 키는 members.id).
 * 코드 목록은 콤마 구분 문자열로 둔다 — 조회는 늘 회원 단위로 통째로 읽기 때문에 정규화할 이유가 없다.
 */
@Entity
@Table(name = "member_profiles")
public class MemberProfile {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "life_codes", length = 100)
    private String lifeCodes;

    @Column(name = "household_codes", length = 100)
    private String householdCodes;

    @Column(name = "theme_codes", length = 100)
    private String themeCodes;

    @Column(name = "age")
    private Integer age;

    @Column(name = "ctpv_nm", length = 50)
    private String ctpvNm;

    @Column(name = "sgg_nm", length = 50)
    private String sggNm;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MemberProfile() {
    }

    public MemberProfile(Long memberId, WelfareProfile profile) {
        this.memberId = memberId;
        update(profile);
    }

    public void update(WelfareProfile profile) {
        this.lifeCodes = join(profile.lifeCodes());
        this.householdCodes = join(profile.householdCodes());
        this.themeCodes = join(profile.themeCodes());
        this.age = profile.age();
        this.ctpvNm = profile.ctpvNm();
        this.sggNm = profile.sggNm();
        this.updatedAt = LocalDateTime.now();
    }

    public WelfareProfile toProfile() {
        return new WelfareProfile(split(lifeCodes), split(householdCodes), split(themeCodes), age, ctpvNm, sggNm);
    }

    public Long getMemberId() {
        return memberId;
    }

    private static String join(List<String> codes) {
        return codes == null || codes.isEmpty() ? null : String.join(",", codes);
    }

    private static List<String> split(String codes) {
        return codes == null || codes.isBlank() ? List.of() : Arrays.asList(codes.split(","));
    }
}
