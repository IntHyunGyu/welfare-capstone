package com.welfare.today.controller;

import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DB에 저장된 생애주기/가구상황/관심주제 라벨과 건수를 보여준다.
 * 코드표 enum 라벨이 실제 API 표기와 다르면 매칭이 조용히 0건이 되므로, 그걸 실데이터로 대조하는 용도.
 */
@RestController
@RequestMapping("/admin/labels")
public class LabelAdminController {

    private final WelfareBenefitRepository benefitRepository;

    public LabelAdminController(WelfareBenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    @GetMapping
    public Map<String, Map<String, Long>> labels() {
        List<WelfareBenefit> all = benefitRepository.findAll();
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        result.put("lifeNmArray", countTokens(all, WelfareBenefit::getLifeNmArray));
        result.put("trgterIndvdlNmArray", countTokens(all, WelfareBenefit::getTrgterIndvdlNmArray));
        result.put("intrsThemaNmArray", countTokens(all, WelfareBenefit::getIntrsThemaNmArray));
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
