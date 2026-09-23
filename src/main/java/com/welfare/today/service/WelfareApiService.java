package com.welfare.today.service;

import com.welfare.today.config.WelfareApiProperties;
import com.welfare.today.dto.WelfareListResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 공공데이터포털 지자체복지서비스 목록조회(LcgvWelfarelist) 호출.
 *
 * 주의: serviceKey는 공공데이터포털에서 발급하는 "디코딩(Decoding)" 키를 사용해야 한다.
 * RestClient의 UriBuilder가 쿼리 파라미터를 자동으로 URL 인코딩하므로,
 * 이미 인코딩된 "인코딩(Encoding)" 키를 넣으면 이중 인코딩되어 인증에 실패한다.
 */
@Service
public class WelfareApiService {

    private final RestClient restClient;
    private final WelfareApiProperties properties;

    public WelfareApiService(RestClient.Builder restClientBuilder, WelfareApiProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    public WelfareListResponse fetchList(int pageNo) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/LcgvWelfarelist")
                        .queryParam("serviceKey", properties.serviceKey())
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", properties.pageSize())
                        .build())
                .retrieve()
                .body(WelfareListResponse.class);
    }
}
