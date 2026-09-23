# CLAUDE.md

이 문서는 새 Claude Code 세션이 이 캡스톤 프로젝트를 이어서 진행할 수 있도록 지금까지의 논의/결정 사항과 진행 상황을 정리한 기획 문서입니다.
**2026-09-23 기준 진행 상황**: 프로젝트 스캐폴딩부터 프로필 입력→매칭 화면까지 구현 완료, 실제 서비스키로 배치 실행까지 검증 완료. 자세한 내용은 하단 [진행 상황](#진행-상황-2026-09-23-기준) 섹션 참고.

## 프로젝트 개요

- **과목**: 대학교 융합캡스톤
- **진행**: 1인 단독 진행, 1학기(약 16주)
- **주제**: 1인 가구·취약계층 대상, 프로필 기반으로 "오늘 나에게 해당되는" 지자체 복지혜택을 보여주는 웹 서비스
- **가제**: "오늘의 복지"

### 문제 정의
복지 혜택은 많지만 검색 기반 포털(복지로 등)은 본인이 무엇을 찾아야 하는지 알아야 검색이 가능함. 특히 고령층·1인 가구는 자기 조건(생애주기/가구상황/관심주제)을 스스로 코드화해서 검색하기 어려움.

### 핵심 아이디어
사용자가 체크박스로 프로필(생애주기, 가구상황, 관심주제, 나이, 지역)을 입력하면, 그 값을 그대로 공공데이터포털 "지자체복지서비스" API의 요청 파라미터로 변환해 호출 → 매칭된 혜택 리스트를 보여줌. **로컬에서 복잡한 매칭 알고리즘을 새로 짤 필요 없음** — API가 서버 사이드로 필터링해줌.

> **구현 노트**: 실제로는 매 요청마다 라이브 API를 호출하지 않고, 아래 "데이터 캐싱 전략"대로 배치가 미리 적재한 **로컬 DB(WelfareBenefit)를 대상으로 같은 필터링 의미를 JPA Specification으로 재현**해서 매칭한다 (`WelfareMatchService`). 코드값(001, 010 등)은 한글 라벨로 변환 후 로컬에 저장된 콤마구분 문자열에 `LIKE` 매칭하는 방식.

### "그날그날" 컨셉 관련 결정사항
상세조회 응답의 `enfcEndYmd`(사업 종료일)는 대부분 `99991231`(무기한)이라 "신청 마감 D-day" 데이터가 없음. 따라서 "마감임박 알림" 기능은 **드롭**. 대신:
- `sprtCycNm`(지원주기: 월/분기/1회성/수시)으로 "어떤 주기로 받는 혜택인지" 표시
- `lastModYmd`(최종수정일)로 "최근 업데이트" 뱃지
- "그날그날"의 의미를 "오늘 접속해서 즉시 내 조건에 맞는 혜택을 확인한다"로 재정의

## 기술 스택 (확정)

- **백엔드**: Spring Boot 3.x + Spring Web MVC (Maven) — ✅ Spring Boot 3.3.5, Java 17로 적용됨
- **뷰**: Thymeleaf (서버 렌더링 — 혼자 진행이라 프론트 프로젝트 분리 안 함, CORS/배포 이원화 회피)
- **DB**: MySQL + Spring Data JPA
- **외부 API 호출**: `RestClient` (Spring Boot 3.2+)
- **XML 파싱**: `jackson-dataformat-xml`
- **LLM 연동**: OpenAI/Claude REST API를 `RestClient`로 직접 호출 (별도 SDK 불필요) — 미구현
- **인증**: 없음. 로그인 없이 `HttpSession`으로 프로필 임시 보관. 즐겨찾기는 브라우저 `localStorage`에 `servId`만 저장 (백엔드 부담 최소화)
- **배포**: Railway / Render / AWS 프리티어 중 택1 — **3~4주차에 미리 배포 파이프라인 확인 필수** (막판 배포 이슈가 캡스톤 최대 리스크) — ⚠️ 아직 미착수, 이 프로젝트에서 가장 리스크 큰 항목

## MVP 기능 범위

**P0 (필수)**
1. ✅ 프로필 입력 폼 (생애주기/가구상황/관심주제 체크박스, 나이, 시도/시군구)
2. ✅ 복지혜택 매칭 — 프로필을 로컬 캐시 조회 조건으로 변환, 결과 리스트 표시
3. ✅ 혜택 카드 UI (제목, 요약, 지원주기, 신청방법, 상세링크)
4. ⬜ LLM 요약 — 상세조회의 `sprtTrgtCn`(지원대상)/`slctCritCn`(선정기준)/`alwServCn`(지원내용) 같은 딱딱한 행정 문구를 쉬운 말로 변환 — **다음 작업 대상**

**P1 (여유 있으면)**
5. ⬜ "최근 업데이트" 뱃지 (`lastModYmd` 기준)
6. ⬜ 즐겨찾기 (localStorage)

**P2 (발표 시 "향후 계획"으로만 언급, 구현 안 함)**
- 실시간 푸시 알림, 전국 완전 자동 연동, 회원가입/로그인

## 데이터 캐싱 전략 (중요)

정부 API 개발계정은 **일 1,000 트래픽 제한**이 있고, 발표 당일 API가 느리거나 그날 호출량을 이미 소진했을 위험이 있음. → **배치로 데이터를 미리 긁어 로컬 DB에 캐싱**하고, 화면은 기본적으로 로컬 DB를 조회하는 구조로 설계. 상세조회는 저트래픽이라 온디맨드 라이브 호출도 가능하나, 안정성을 위해 이것도 캐싱 권장.

> ✅ **목록조회 배치 구현 및 실측 완료**: `WelfareBenefitBatchService` + `POST /admin/batch/welfare-list` (수동 트리거, 스케줄러 없음). 실제 서비스키로 실행한 결과 **전체 4,818건을 25회 호출(page-size=200)로 전량 수집** — 일 1,000회 quota 대비 매우 여유 있음. 상세조회(`WelfareBenefitDetail`) 배치는 아직 미구현.

## ERD (초안)

```
WelfareBenefit (목록조회 캐시)  — ✅ 엔티티/Repository 구현 및 실데이터 적재 검증 완료
- servId (PK, String)
- servNm
- servDgst
- ctpvNm, sggNm
- bizChrDeptNm
- aplyMtdNm
- sprtCycNm
- srvPvsnNm
- servDtlLink
- lastModYmd
- lifeNmArray, trgterIndvdlNmArray, intrsThemaNmArray  (콤마구분 String 그대로 저장, MVP에선 정규화 테이블 불필요)
- fetchedAt (배치 갱신 시각)

WelfareBenefitDetail (상세조회 캐시, servId 1:1 FK)  — ⬜ 엔티티/Repository만 존재, 배치/조회 로직 미구현
- servId (PK/FK)
- sprtTrgtCn (지원대상)
- slctCritCn (선정기준)
- alwServCn (지원내용)
- aplyMtdCn (신청방법 상세)
- enfcBgngYmd, enfcEndYmd
- llmSummary (LLM이 쉬운 말로 요약한 텍스트, 캐싱해서 LLM 호출 비용/횟수 절감)
```

즐겨찾기는 DB 테이블 없이 프런트 `localStorage`로 처리.

## 외부 API 스펙 (공공데이터포털 — 지자체복지서비스)

- **제공기관**: 한국사회보장정보원
- **Base URL**: `https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations`
- **데이터 포맷**: XML
- **개발계정 트래픽**: 일 1,000회
- **인증키**: `.gitignore` 처리한 로컬 프로퍼티/환경변수로 관리, **절대 커밋 금지** — ✅ `application-local.yml`(gitignore 처리, `local` 프로필)에 실키 적용 및 인증 성공 확인

### 1) 목록조회 `/LcgvWelfarelist`

요청 파라미터:

| 파라미터 | 설명 | 예시 |
|---|---|---|
| serviceKey | 인증키 | - |
| pageNo | 페이지시작위치 | 1 |
| numOfRows | 출력건수 | 10 |
| lifeArray | 생애주기 코드(콤마구분) | 001,002,003 |
| trgterIndvdlArray | 가구상황 코드(콤마구분) | 010,020,030 |
| intrsThemaArray | 관심주제 코드(콤마구분) | 010,020,030 |
| age | 나이 | 5 |
| ctpvNm | 시도명 | 서울특별시 |
| sggNm | 시군구명 | 송파구 |
| srchKeyCode | 검색분류 코드 | 001 |
| searchWrd | 검색어 | 국가 |
| arrgOrd | 정렬순서 코드 | 001 |

응답 필드 (`wantedList/servList` 반복— ✅ 실제 응답으로 루트/래퍼 태그명 검증됨):
`servId`, `servNm`, `servDgst`, `ctpvNm`, `sggNm`, `bizChrDeptNm`, `aplyMtdNm`, `sprtCycNm`, `srvPvsnNm`, `servDtlLink`, `lastModYmd`, `inqNum`, `lifeNmArray`, `trgterIndvdlNmArray`, `intrsThemaNmArray`
(+ `totalCount`, `pageNo`, `numOfRows`, `resultCode`, `resultMessage`)

> ⚠️ `resultCode`의 정확한 성공/실패 값은 아직 미확인 (정상 케이스만 테스트해봄 — 실패 응답 구조는 못 봄). 현재 배치 코드는 `servList`가 null이면 방어적으로 중단하는 정도만 처리하고 resultCode 자체는 검사하지 않음.

### 2) 상세조회 `/LcgvWelfaredetailed`

요청 파라미터: `serviceKey`, `servId` (예: `WLF00001138`)

응답 필드 (`wantedDtl`):
`servId`, `servNm`, `enfcBgngYmd`, `enfcEndYmd`, `bizChrDeptNm`, `ctpvNm`, `sggNm`, `servDgst`, `lifeNmArray`, `trgterIndvdlNmArray`, `intrsThemaNmArray`, `sprtCycNm`, `srvPvsnNm`, `aplyMtdNm`, `sprtTrgtCn`(지원대상 상세), `slctCritCn`(선정기준 상세), `alwServCn`(지원내용 상세), `aplyMtdCn`(신청방법 상세), `inqNum`, `lastModYmd`
+ 관련 리스트: `inqplCtadrList`(문의처), `inqplHmpgReldList`(관련 사이트), `baslawList`(근거법령), `basfrmList`(서식/자료)

> ⬜ 미구현 — 다음 로드맵 항목(LLM 요약)과 함께 연동 예정.

### 코드표

**생애주기 (lifeArray)** — ✅ `LifeStage` enum으로 구현
001 영유아, 002 아동, 003 청소년, 004 청년, 005 중장년, 006 노년, 007 임신·출산

**가구상황 (trgterIndvdlArray)** — ✅ `HouseholdSituation` enum으로 구현
010 다문화·탈북민, 020 다자녀, 030 보호대상자, 040 장애인, 050 저소득, 060 한부모·조손

**관심주제 (intrsThemaArray)** — ✅ `InterestTheme` enum으로 구현
010 신체건강, 020 정신건강, 030 생활지원, 040 주거, 050 일자리, 060 문화·여가, 070 안전·위기, 080 임신·출산, 090 보육, 100 교육, 110 입양·위탁, 120 보호·돌봄, 130 서민금융, 140 법률

**서비스구분코드 (wlfareInfoDtlCd, 상세조회 부가정보용)**
010 문의, 020 사이트, 030 근거법령, 040 서식/자료

**검색분류 (srchKeyCode)**
001 서비스명, 002 서비스내용, 003 서비스명+서비스내용

**정렬순서 (arrgOrd)**
001 최신순, 002 인기순

## 참고 코드 스케치 (✅ 구현됨)

아래 구조 그대로 `dto/WelfareListResponse.java`, `dto/WelfareItem.java`로 구현되었고 실제 XML 응답 파싱까지 검증됨:

```java
public record WelfareListResponse(
    int totalCount, int resultCode, String resultMessage,
    List<WelfareItem> servList
) {}

public record WelfareItem(
    String servId, String servNm, String servDgst,
    String ctpvNm, String sggNm, String bizChrDeptNm,
    String aplyMtdNm, String sprtCycNm, String srvPvsnNm,
    String servDtlLink, String lastModYmd,
    String lifeNmArray, String trgterIndvdlNmArray, String intrsThemaNmArray
) {}
```

패키지 구조는 `controller` / `service` / `repository` / `entity` / `dto` 표준 Spring Boot 레이어 구성 (+ 코드표용 `code` 패키지, 설정 바인딩용 `config` 패키지 추가).

## 16주 로드맵

| 주차 | 내용 | 상태 |
|---|---|---|
| 1~2주 | 주제 확정, 공공데이터 API 활용신청, ERD 확정 | ✅ |
| 3~4주 | 프로젝트 셋업(Spring Initializr), 엔티티/Repository, **배포 파이프라인 미리 확인** | 프로젝트 셋업/엔티티는 ✅, 배포 파이프라인 확인은 ⬜ (리스크) |
| 5~7주 | 배치 수집 로직 + 매칭 서비스(Controller/Thymeleaf) | ✅ (목록조회 배치·매칭·화면 전부 실데이터로 검증됨) |
| 8주 | 중간발표 (프로필 입력 → 혜택 목록 데모) | 데모 가능한 상태 |
| 9~11주 | LLM 요약 기능 연동 | ⬜ 다음 작업 |
| 12~13주 | 최근 업데이트 뱃지, 즐겨찾기(localStorage) | ⬜ |
| 14주 | 배포 + 통합 테스트 | ⬜ |
| 15~16주 | 발표 준비 | ⬜ |

## 진행 상황 (2026-09-23 기준)

**완료**   
1. Spring Boot 3.3.5 / Java 17 프로젝트 스캐폴딩 (`pom.xml`, `TodayWelfareApplication`)
2. `application.yml` 설정 (DB/API키 env-var 플레이스홀더) + `.gitignore` — 실키는 gitignore 처리된 `application-local.yml`(`local` 프로필)에 별도 보관
3. ERD 기반 `WelfareBenefit`/`WelfareBenefitDetail` 엔티티 + Repository
4. 배치 수집: `WelfareApiService`(RestClient 목록조회 1페이지 호출) + `WelfareBenefitBatchService`(페이지네이션·upsert) + `BatchAdminController`(`POST /admin/batch/welfare-list` 수동 트리거)
5. 프로필 입력/매칭 화면: `ProfileController`(`GET/POST /profile`) + `MatchController`(`GET /match`) + `WelfareMatchService`(JPA Specification 기반 로컬 캐시 매칭) + Thymeleaf 템플릿(`profile-form.html`, `match-result.html`)
6. **실제 서비스키로 end-to-end 검증 완료** (로컬 MySQL이 없어 임시 H2로 검증, pom.xml은 검증 후 원복):
   - 배치 실행 결과: `{"fetchedCount":4818,"totalCount":4818,"pageCount":25,"truncatedByMaxPages":false}` (page-size=200 적용 후 기준)
   - 프로필(청년+저소득) 제출 → 매칭 131건, 카드 UI 정상 렌더링 확인
   - 인증키(디코딩 키) 정상 동작, XML 응답 구조(`wantedList`/`servList`) 실제로 맞음을 확인

**알려진 한계/TODO**
- 나이(age) 필터는 화면 입력은 받지만 매칭 로직에는 미반영 (서비스별 연령 상한/하한이 로컬에 캐싱되어 있지 않음 — 상세조회 연동 시 재검토)
- 시군구는 드롭다운 계층 없이 자유 텍스트 입력 (MVP 단순화)
- `resultCode` 실패 케이스 처리 로직 없음 (정상 케이스만 검증됨)
- `/admin/batch/welfare-list`는 인증 없이 노출됨 (MVP 방침) — 배포 시 quota 낭비 리스크 있음, 발표 자료에 언급 필요
- 배포 파이프라인 미착수 (로드맵상 가장 큰 리스크로 지목된 항목)

## 다음에 할 일

1. 상세조회(`LcgvWelfaredetailed`) 연동: `WelfareApiService`에 상세조회 호출 추가, `WelfareBenefitDetail` 저장 배치/온디맨드 로직 설계
2. LLM 요약 프롬프트 설계 (`sprtTrgtCn`/`slctCritCn`/`alwServCn` 원문 → 쉬운 말 변환, 결과를 `WelfareBenefitDetail.llmSummary`에 캐싱)
3. "최근 업데이트" 뱃지 (`lastModYmd` 기준), 즐겨찾기(localStorage) — P1
4. 배포 파이프라인 확인 (Railway/Render/AWS 프리티어 중 택1) — 리스크 큰 항목이라 우선순위 앞당기는 것도 고려

## 발표 시 강조 포인트

- "검색이 아니라 매칭"이라는 문제 정의 차별점
- LLM의 구체적 기여: 챗봇이 아니라 "행정 문구 → 쉬운 요약" 변환
- 실사용자 테스트(가족/지인) 피드백을 정량 결과로 제시
- "마감임박 알림"을 드롭한 이유를 데이터 근거(enfcEndYmd 대부분 무기한)로 설명할 수 있어야 함
- 캐싱 전략의 실효성을 수치로 제시 가능: 전체 4,818건을 25회 API 호출로 전량 수집(일 1,000회 quota의 2.5%)
