package com.welfare.today.service;

import com.welfare.today.code.HouseholdSituation;
import com.welfare.today.code.InterestTheme;
import com.welfare.today.code.LifeStage;
import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 캐싱된 로컬 DB(WelfareBenefit)를 대상으로 프로필 조건에 맞는 혜택을 찾는다.
 * 정부 API를 매 요청마다 호출하지 않고 배치로 미리 적재한 데이터를 조회하는
 * 구조라(CLAUDE.md 캐싱 전략), lifeNmArray 등은 콤마구분 "이름" 문자열로 저장되어 있어
 * 프로필의 코드값을 이름으로 변환한 뒤 LIKE로 매칭한다.
 *
 * 나이(age)는 목록 API 호출 파라미터로만 존재하고 서비스별 상한/하한 연령이
 * 로컬에 캐싱되어 있지 않아 현재는 필터링에 사용하지 않는다 (표시 용도로만 프로필에 보관).
 */
@Service
public class WelfareMatchService {

    private final WelfareBenefitRepository repository;

    public WelfareMatchService(WelfareBenefitRepository repository) {
        this.repository = repository;
    }

    public List<WelfareBenefit> match(WelfareProfile profile) {
        Specification<WelfareBenefit> spec = Specification.where(null);

        spec = andLikeAnyOf(spec, "lifeNmArray", toLabels(profile.lifeCodes(), LifeStage::labelOfCode));
        spec = andLikeAnyOf(spec, "trgterIndvdlNmArray", toLabels(profile.householdCodes(), HouseholdSituation::labelOfCode));
        spec = andLikeAnyOf(spec, "intrsThemaNmArray", toLabels(profile.themeCodes(), InterestTheme::labelOfCode));

        if (StringUtils.hasText(profile.ctpvNm())) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ctpvNm"), profile.ctpvNm()));
        }
        if (StringUtils.hasText(profile.sggNm())) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sggNm"), profile.sggNm()));
        }

        return repository.findAll(spec, Sort.by(Sort.Direction.DESC, "lastModYmd"));
    }

    private List<String> toLabels(List<String> codes, java.util.function.Function<String, String> labelLookup) {
        if (codes == null) {
            return List.of();
        }
        return codes.stream()
                .map(labelLookup)
                .filter(StringUtils::hasText)
                .toList();
    }

    private Specification<WelfareBenefit> andLikeAnyOf(Specification<WelfareBenefit> spec, String field, List<String> labels) {
        if (labels.isEmpty()) {
            return spec;
        }
        return spec.and((root, query, cb) -> {
            Predicate[] predicates = labels.stream()
                    .map(label -> cb.like(root.get(field), "%" + label + "%"))
                    .toArray(Predicate[]::new);
            return cb.or(predicates);
        });
    }
}
