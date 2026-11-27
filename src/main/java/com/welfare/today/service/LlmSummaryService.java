package com.welfare.today.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlock;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.welfare.today.config.LlmProperties;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.entity.LlmUsage;
import com.welfare.today.entity.WelfareBenefitDetail;
import com.welfare.today.repository.LlmUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상세조회의 행정 문구(지원대상/선정기준/지원내용/신청방법)를 쉬운 우리말 요약으로 바꾼다.
 */
@Service
public class LlmSummaryService {

    private static final Logger log = LoggerFactory.getLogger(LlmSummaryService.class);

    private static final String SYSTEM_PROMPT = """
            너는 지자체 복지 안내문을 어르신이나 혼자 사는 분도 한 번에 이해할 수 있는 쉬운 우리말로 바꿔 주는 도우미야.

            요약은 상세 화면 맨 위에 보이고, 그 아래에 원문 전체가 함께 나온다.
            그러니 모든 조건을 옮기지 말고, 처음 보는 사람이 "나도 해당되나?"를 바로 판단할 핵심만 담아 줘.

            아래 규칙대로 요약해 줘.
            - "• "로 시작하는 줄 3~4개로 쓴다. 한 줄은 한 문장, 40자 안팎으로 쓴다. 전체는 300자를 넘기지 않는다.
            - 누가 받을 수 있는지, 무엇을 받는지, 어떻게 신청하는지를 이 순서로 한 줄씩 담는다. 원문에 없는 항목은 건너뛴다.
            - 대상이나 지원 내용이 여러 갈래로 나뉘면 대표적인 것 하나만 쓰고 "등"으로 묶는다. 세부 기준은 "자세한 기준은 아래 원문을 보세요"로 넘긴다.
            - 금액, 나이, 소득 기준(%) 같은 핵심 숫자는 원문 그대로 옮긴다.
            - "기준중위소득"처럼 어려운 행정 용어는 괄호 안에 몇 글자로 짧게 풀어 준다.
            - 원문에 없는 내용은 절대 지어내지 않는다. 신청 방법이 원문에 없으면 "신청 방법은 담당 부서에 확인하세요"라고 쓴다.
            - 제목, 인사말, 마크다운 문법은 쓰지 않는다. "~해요", "~예요" 같은 부드러운 말투를 쓴다.
            """;

    private final LlmProperties properties;
    private final LlmUsageRepository usageRepository;
    private final AnthropicClient client;

    public LlmSummaryService(LlmProperties properties, LlmUsageRepository usageRepository) {
        this.properties = properties;
        this.usageRepository = usageRepository;
        this.client = properties.enabled()
                ? AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build()
                : null;
    }

    public boolean enabled() {
        return client != null;
    }

    /**
     * 요약 문자열을 돌려준다. 모델이 요청을 거절하면 빈 값 (Haiku 5.5는 서버측 fallback이 없어 화면은 원문만 보여준다).
     */
    public Optional<String> summarize(WelfareBenefit benefit, WelfareBenefitDetail detail) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(16000L)
                .system(SYSTEM_PROMPT)
                .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.LOW).build())
                .addUserMessage(buildUserMessage(benefit, detail))
                .build();

        BetaMessage response = client.beta().messages().create(params);
        log.info("LLM 요약 servId={} model={} stop={} input_tokens={} output_tokens={}",
                benefit.getServId(),
                response.model().asString(),
                response.stopReason().map(BetaStopReason::asString).orElse("-"),
                response.usage().inputTokens(),
                response.usage().outputTokens());
        // 거절된 호출도 토큰은 과금되므로 함께 기록한다.
        usageRepository.save(new LlmUsage(benefit.getServId(), response.model().asString(),
                response.usage().inputTokens(), response.usage().outputTokens(),
                LocalDateTime.now(MetricService.SEOUL)));
        if (response.stopReason().filter(BetaStopReason.REFUSAL::equals).isPresent()) {
            return Optional.empty();
        }

        String text = response.content().stream()
                .map(BetaContentBlock::text)
                .flatMap(Optional::stream)
                .map(BetaTextBlock::text)
                .collect(Collectors.joining())
                .strip();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    private String buildUserMessage(WelfareBenefit benefit, WelfareBenefitDetail detail) {
        return """
                다음 복지 서비스 안내문을 요약해 줘.

                <서비스명>%s</서비스명>
                <지원대상>%s</지원대상>
                <선정기준>%s</선정기준>
                <지원내용>%s</지원내용>
                <신청방법>%s</신청방법>
                """.formatted(
                benefit.getServNm(),
                orEmpty(detail.getSprtTrgtCn()),
                orEmpty(detail.getSlctCritCn()),
                orEmpty(detail.getAlwServCn()),
                orEmpty(detail.getAplyMtdCn()));
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
