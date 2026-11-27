package com.welfare.today.service;

import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DB에 저장된 생애주기/가구상황/관심주제 라벨과 건수를 센다.
 * 코드표 enum 라벨이 실제 API 표기와 다르면 매칭이 조용히 0건이 되므로, 그걸 실데이터로 대조하는 용도.
 */
@Service
public class LabelStatsService {

    private final WelfareBenefitRepository benefitRepository;

    public LabelStatsService(WelfareBenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    /** 분류 이름 → (라벨 → 건수). 순서는 생애주기, 가구상황, 관심주제. */
    public Map<String, Map<String, Long>> labelCounts() {
        List<WelfareBenefit> all = benefitRepository.findAll();
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        result.put("생애주기", countTokens(all, WelfareBenefit::getLifeNmArray));
        result.put("가구상황", countTokens(all, WelfareBenefit::getTrgterIndvdlNmArray));
        result.put("관심주제", countTokens(all, WelfareBenefit::getIntrsThemaNmArray));
        return result;
    }

    private Map<String, Long> countTokens(List<WelfareBenefit> all, Function<WelfareBenefit, String> field) {
        return all.stream()
                .map(field)
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::strip)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
    }
}
