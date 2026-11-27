package com.welfare.today.service;

import com.welfare.today.repository.WelfareBenefitRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 지역 선택지는 DB에 실제로 저장된 시도/시군구 이름으로 만든다. 매칭이 이름 equal 비교라
 * 하드코딩한 목록은 API 표기와 어긋나면(예: 광주광역시/전라남도) 조용히 0건이 된다.
 */
@Service
public class RegionService {

    private final WelfareBenefitRepository benefitRepository;

    public RegionService(WelfareBenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    /** 시도 → 시군구 목록 (둘 다 가나다순). 시군구가 비어 있는 시도 단위 사업은 시도 키로만 남는다. */
    public Map<String, List<String>> regions() {
        Map<String, TreeSet<String>> bySido = new TreeMap<>();
        // 값은 다듬지 않고 DB 그대로 쓴다 - 매칭이 equal 비교라 공백 하나만 달라도 결과가 0건이 된다.
        for (WelfareBenefitRepository.RegionRow row : benefitRepository.findDistinctRegions()) {
            if (!StringUtils.hasText(row.getCtpvNm())) {
                continue;
            }
            TreeSet<String> sggs = bySido.computeIfAbsent(row.getCtpvNm(), key -> new TreeSet<>());
            if (StringUtils.hasText(row.getSggNm())) {
                sggs.add(row.getSggNm());
            }
        }
        Map<String, List<String>> result = new TreeMap<>();
        bySido.forEach((sido, sggs) -> result.put(sido, new ArrayList<>(sggs)));
        return result;
    }
}
