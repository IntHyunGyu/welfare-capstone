# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

이 문서는 새 Claude Code 세션이 이 캡스톤 프로젝트를 이어서 진행할 수 있도록 지금까지의 논의/결정 사항과 진행 상황을 정리한 기획 문서입니다.
**2026-09-23 기준 진행 상황**: 프로젝트 스캐폴딩부터 프로필 입력→매칭 화면까지 구현 완료, 실제 서비스키로 배치 실행까지 검증 완료. 자세한 내용은 하단 [진행 상황](#진행-상황-2026-09-23-기준) 섹션 참고.

**2026-11-27 배포 완료**: https://welfare-capstone-production.up.railway.app (Railway + MySQL). 실데이터 배치 적재 성공, 매칭(청년+저소득) 131건, 상세조회 파싱(`wantedDtl` 추측 맞음)과 LLM 요약 실제 동작 확인. 요약이 너무 길어서(한 줄에 여러 문장) 프롬프트를 3~4줄·300자 이내·핵심만으로 줄임 — 프롬프트를 바꾸면 관리자 페이지의 "요약 다시 만들기"(특정 혜택만: 서비스 ID 입력)로 저장된 요약을 지워야 새 프롬프트가 적용된다. 첫 상세 조회가 약 13초 걸리던 문제는, 상세 화면을 LLM 없이 먼저 렌더링하고 `static/js/summary.js`가 `POST /benefits/{servId}/summary`로 요약을 따로 받아 채우는 방식으로 해결 (기다리는 동안 "AI가 쉬운 말로 요약하고 있어요…" 표시, 실패/키 없음이면 박스 숨김). 요약 요청을 POST로 둔 건 브라우저 미리 불러오기나 크롤러가 LLM 비용을 쓰지 않게 하려는 것.

**2026-11-27 추가 결정**: ① 프로필 입력/매칭 결과 화면 UI를 토스 스타일(칩 토글, 카드+그림자)로 리디자인. ② 로컬 개발 머신에 JDK가 8뿐이라 Temurin JDK 17 설치. 다른 프로젝트가 1.8을 쓰므로 기본 `JAVA_HOME`은 1.8로 두고, 이 프로젝트는 `mvn17.cmd`로만 17을 쓴다. ③ **로컬 DB를 H2로 확정** — MySQL 구축/연동은 배포 시점으로 미룸 (위 [DB](#개발-명령어) 항목 참고).

## 개발 명령어

Maven Wrapper(`mvnw`)는 없음. **이 PC의 기본 JDK는 1.8**(다른 프로젝트용)이고 이 프로젝트만 JDK 17이 필요하므로, `mvn` 대신 저장소 루트의 `mvn17.cmd`를 쓴다 — 그 실행에만 Temurin 17(`%ProgramFiles%\Eclipse Adoptium\jdk-17*`, 또는 `JDK17_HOME`)을 `JAVA_HOME`으로 잡고 `mvn`에 인자를 그대로 넘긴다. 그냥 `mvn`으로 돌리면 Java 8로 record 문법 컴파일이 실패한다.

```bash
.\mvn17.cmd clean package                                   # 빌드 (+테스트)
.\mvn17.cmd -DskipTests package                             # 테스트 생략 빌드
.\mvn17.cmd spring-boot:run "-Dspring-boot.run.profiles=local"  # 실키가 든 local 프로필로 실행 (http://localhost:8080 → /profile 리다이렉트)
.\mvn17.cmd test                                            # 전체 테스트
.\mvn17.cmd test -Dtest=TodayWelfareApplicationTests        # 단일 테스트 클래스 (메서드: -Dtest=Class#method)
```

- **데이터 수집/관리**: 관리자 페이지 `/manage`(ADMIN만)에서 한다 — "지금 데이터 수집하기"(목록조회 배치, API 약 25회), "요약 다시 만들기"(저장된 LLM 요약 지우기, 서비스 ID 지정 가능), "코드표 라벨 대조"(DB 라벨별 건수). 로컬에서는 `ADMIN_USERNAMES=<가입한 아이디>`로 실행하면 그 아이디가 관리자. (예전 curl + `X-Admin-Token` 방식의 `/admin/**` 엔드포인트는 2026-11-27에 삭제)

- **로컬 설정**: `src/main/resources/application-local.yml`(gitignore 대상, 저장소에 없음)에 `welfare.api.service-key`(반드시 **디코딩 키** — `RestClient`가 쿼리를 다시 인코딩하므로 인코딩 키는 이중 인코딩되어 인증 실패)를 둔다. 또는 환경변수 `WELFARE_API_SERVICE_KEY`. 같은 파일에 `llm.api-key`(LLM 요약)와 `welfare.admin.usernames: admin`(로컬 관리자 아이디 — 로컬에서 `admin`으로 가입하면 `/manage` 접근)도 둔다. 즉 `local` 프로필만 켜면 환경변수 없이 전부 동작한다.
- **DB (2026-11-27 결정)**: 로컬 화면 개발은 **H2 파일 DB**(`./data/welfare`, `pom.xml`에 `h2` 런타임 의존성 포함)를 기본 프로필로 사용 — `mvn spring-boot:run`만으로 로컬 MySQL 없이 기동 가능, `ddl-auto: update`로 스키마 자동 생성. MySQL은 **배포 시점에만** 다시 꺼내 쓸 예정 (`application.yml` 주석에 mysql 설정 예시 남겨둠, `mysql-connector-j` 의존성은 유지 중). `DB_USERNAME`/`DB_PASSWORD`는 그때 가서 설정.
- **H2 콘솔**: 기동 중 `http://localhost:8080/h2-console`에서 JDBC URL `jdbc:h2:file:./data/welfare`, 유저 `sa`(비밀번호 없음)로 접속해 로컬 데이터 직접 확인 가능.
- **테스트** (57개, H2라 별도 DB 없이 돈다): `SinceLastVisitTest`(지난 방문 이후 새 혜택 — 첫 방문 없음/조건 맞는 새 혜택만/새로고침 유지/다음 방문 기준 이동), `WelfareBenefitBatchServiceTest`(NEW 기록 — 첫 채우기는 NEW 아님/새 혜택만 기록/재수집해도 유지), `FavoriteFlowTest`(즐겨찾기 API 로그인·CSRF/저장·해제 반복 가능/저장 화면 최신순/비회원 즐겨찾기 일괄 이전/탈퇴 시 삭제), `MyPageFlowTest`(마이페이지 — 접근 권한/비밀번호 변경 검증·토큰 삭제/탈퇴 시 전부 삭제·로그아웃), `MemberProfileFlowTest`(회원 조건 저장 — 가입 안내/가입 전 조건 이어받기/재로그인 시 바로 결과/비회원은 세션만), `ProfileFlowTest`(조건 다시 입력 시 이전 선택 유지, 조건 없이 제출 차단, 결과 화면의 찾은 조건 표시), `AuthFlowTest`(`@SpringBootTest` — 가입/로그인/로그인 유지(DB 토큰 저장·쿠키만으로 로그인·로그아웃 시 삭제)/CSRF 규칙), `ManageAccessTest`(관리자 접근 권한/수집 버튼/요약 지우기/라벨 대조/페이지 조회 집계), `MetricServiceTest`, `AdminDashboardServiceTest`(일별 집계·추정 비용), `CodeLabelTest`(코드표 라벨 = 실데이터 표기), `WelfareMatchServiceTest`(`@DataJpaTest` — 같은 분류 OR/분류 간 AND/시도 equal/최신순), `RegionTest`(지역 목록, 시군구 선택 시 시도 전체 사업 포함), `WelfareBenefitTest`(최근 업데이트 30일 판정), `contextLoads`.

## 아키텍처 핵심 (여러 파일을 봐야 보이는 흐름)

패키지 루트: `com.welfare.today` (`controller`/`service`/`repository`/`entity`/`dto` + `code` 코드표 enum, `config` 프로퍼티 바인딩)

1. **수집 경로 (배치, 수동 트리거)**: `BatchAdminController` → `WelfareBenefitBatchService.syncAll()` → `WelfareApiService.fetchList(pageNo)` (XML → `WelfareListResponse` 레코드, `jackson-dataformat-xml`) → `WelfareBenefit` 엔티티로 `saveAll` (servId PK라 재실행 시 upsert). `welfare.api.page-size`/`max-pages`(`WelfareApiProperties`)가 호출 횟수를 제어 — quota 보호용 안전장치이므로 스케줄러 추가 금지 방침.
2. **조회 경로 (사용자)**: `GET /profile` 폼 → `POST /profile`이 `WelfareProfileForm`(체크박스 바인딩용 가변 POJO)을 불변 `WelfareProfile` 레코드로 바꿔 `HttpSession`에 저장 → `GET /match`가 세션에서 꺼내 `WelfareMatchService.match()` 호출. 세션에 프로필 없으면 `/profile`로 리다이렉트.
3. **매칭의 코드↔라벨 변환**: 폼/세션은 API **코드값**(`001`, `010`…)을 들고 있지만, DB에는 API 응답의 **한글 이름** 콤마문자열(`lifeNmArray` 등)이 저장되어 있다. 그래서 `LifeStage`/`HouseholdSituation`/`InterestTheme`의 `labelOfCode()`로 라벨 변환 후 `LIKE %라벨%`. 같은 카테고리 내 선택은 OR, 카테고리 간은 AND, 시도는 equal, 시군구는 equal 또는 시군구가 빈 행(시도 전체 사업). 코드표 enum 라벨이 API 응답 표기와 한 글자라도 다르면 매칭이 조용히 0건이 되므로 라벨 수정 시 주의.

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
- **LLM 연동**: Claude API, 공식 Anthropic Java SDK(`anthropic-java`) 사용 — ✅ 구현됨 (실키 검증 전)
- **인증**: 원래는 없음(비회원은 `HttpSession`에 프로필, 즐겨찾기는 `localStorage`). 2026-11-27에 회원 기능 추가로 변경 — 비회원 방식은 유지하고 회원은 DB에 조건/즐겨찾기 저장 (하단 "다음에 할 일" 로드맵)
- **배포**: Railway / Render / AWS 프리티어 중 택1 — **3~4주차에 미리 배포 파이프라인 확인 필수** (막판 배포 이슈가 캡스톤 최대 리스크) — ⚠️ 아직 미착수, 이 프로젝트에서 가장 리스크 큰 항목

## MVP 기능 범위

**P0 (필수)**
1. ✅ 프로필 입력 폼 (생애주기/가구상황/관심주제 체크박스, 나이, 시도/시군구)
2. ✅ 복지혜택 매칭 — 프로필을 로컬 캐시 조회 조건으로 변환, 결과 리스트 표시
3. ✅ 혜택 카드 UI (제목, 요약, 지원주기, 신청방법, 상세링크)
4. ✅ LLM 요약 — 상세조회의 `sprtTrgtCn`(지원대상)/`slctCritCn`(선정기준)/`alwServCn`(지원내용) 같은 딱딱한 행정 문구를 쉬운 말로 변환 (실키로 품질 확인 필요)

**P1 (여유 있으면)**
5. ✅ "최근 업데이트" 뱃지 (`lastModYmd`가 30일 이내 — `WelfareBenefit.isRecentlyUpdated()`)
6. ✅ 즐겨찾기 (localStorage 키 `favoriteServIds`, `static/js/favorites.js` + `GET /favorites`)

**P2 (발표 시 "향후 계획"으로만 언급, 구현 안 함)**
- 실시간 푸시 알림, 전국 완전 자동 연동 (회원가입/로그인은 2026-11-27에 구현하기로 변경 — 하단 "다음에 할 일" 참고)

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
001 영유아, 002 아동, 003 청소년, 004 청년, 005 중장년, 006 노년, 007 임신 · 출산 (**실데이터 표기는 가운뎃점 양옆에 공백** — 관심주제의 임신·출산과 다름)

**가구상황 (trgterIndvdlArray)** — ✅ `HouseholdSituation` enum으로 구현
010 다문화·탈북민, 020 다자녀, 030 보훈대상자 (예전에 "보호대상자"로 잘못 적었다가 실데이터로 정정), 040 장애인, 050 저소득, 060 한부모·조손

**관심주제 (intrsThemaArray)** — ✅ `InterestTheme` enum으로 구현
010 신체건강, 020 정신건강, 030 생활지원, 040 주거, 050 일자리, 060 문화·여가, 070 안전·위기, 080 임신·출산, 090 보육, 100 교육, 110 입양·위탁, 120 보호·돌봄, 130 서민금융, 140 법률 (2026-11 실데이터에는 정신건강/문화·여가/법률 혜택이 0건 — 단독 선택 시 결과 없음이 정상)

> 실데이터 라벨과 건수는 관리자 페이지 `/manage`의 "코드표 라벨 대조"에서 확인한다. 코드표 enum 라벨은 `CodeLabelTest`가 실데이터 표기로 고정하고 있으니, 라벨을 바꾸면 이 테스트도 같이 실데이터로 확인해서 고칠 것.

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

## 16주 로드맵

| 주차 | 내용 | 상태 |
|---|---|---|
| 1~2주 | 주제 확정, 공공데이터 API 활용신청, ERD 확정 | ✅ |
| 3~4주 | 프로젝트 셋업(Spring Initializr), 엔티티/Repository, **배포 파이프라인 미리 확인** | 프로젝트 셋업/엔티티는 ✅, 배포 파이프라인 확인은 ⬜ (리스크) |
| 5~7주 | 배치 수집 로직 + 매칭 서비스(Controller/Thymeleaf) | ✅ (목록조회 배치·매칭·화면 전부 실데이터로 검증됨) |
| 8주 | 중간발표 (프로필 입력 → 혜택 목록 데모) | 데모 가능한 상태 |
| 9~11주 | LLM 요약 기능 연동 | ✅ 구현 (실키 검증 남음) |
| 12~13주 | 최근 업데이트 뱃지, 즐겨찾기(localStorage) | ✅ |
| 14주 | 배포 + 통합 테스트 | ✅ Railway 배포, 테스트 19개 (실데이터 대조로 라벨 버그 2건 발견·수정) |
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
- 지역 선택 (2026-11-27): 시도/시군구 선택지를 하드코딩하지 않고 DB에 저장된 이름으로 만든다(`RegionService`, `static/js/region.js`가 시도에 따라 시군구 목록을 바꿈). 하드코딩 목록일 때 실데이터에서 광주광역시·전라남도가 0건이었다(표기가 달라서). 시군구를 고르면 시군구가 비어 있는 시도 전체 대상 사업도 함께 매칭된다
- `resultCode` 실패 케이스 처리 로직 없음 (정상 케이스만 검증됨)
- 관리 기능은 모두 `/manage`(ADMIN 세션 로그인)에 있다. 예전 `/admin/**` + `X-Admin-Token`(`ADMIN_TOKEN`) 방식은 환경변수를 줄이려고 2026-11-27에 삭제
- 배포 파이프라인 미착수 (로드맵상 가장 큰 리스크로 지목된 항목)

## 다음에 할 일 — 회원 기능 로드맵 (2026-11-27 확정, 한 단계씩 진행)

P2로 미뤄 두었던 회원가입/로그인을 이번에 구현한다. 비회원 조회(지금 방식)는 그대로 유지한다. 단계마다 구현 → 테스트 → 로컬 확인 → 커밋/배포.

| 단계 | 내용 | 상태 |
|---|---|---|
| 1 | **회원가입/로그인**: Spring Security(세션 + BCrypt), 로그인 ID는 **아이디**(이메일 수집 안 함), 로그인 상태 유지(remember-me), 헤더에 로그인 상태. 비회원은 모든 화면 그대로 접근. CSRF: 폼은 Thymeleaf 자동 토큰, 요약 `fetch`는 헤더로 토큰 전달 | ✅ `SecurityConfig`, `Member`(`members` 테이블), `MemberService`, `AuthController`(가입 직후 자동 로그인). 로그인 상태 유지는 처음엔 서명 키(`REMEMBER_ME_KEY`) 방식이었다가, 환경변수를 줄이려고 DB 토큰 방식(`persistent_logins`, `JdbcTokenRepositoryImpl`)으로 변경 — 키 불필요, 로그아웃 시 토큰 삭제 |
| 2 | **관리자 계정·관리자 페이지**: ADMIN 권한, 데이터 수집(목록조회 배치) 버튼, LLM 토큰 사용량(호출별 기록 → 합계/추정 비용), 트래픽 모니터링(공공데이터 API 일별 호출 수 vs 1,000회 한도, 사이트 요청 수) | ✅ `/manage`(`ManageController`, `AdminDashboardService`). 관리자는 환경변수 `ADMIN_USERNAMES`(콤마 구분)에 적힌 아이디 — 로그인 시 ADMIN 권한 부여(`MemberService`), 비밀번호를 환경변수에 두지 않음. 기록: `llm_usage`(호출별 토큰), `daily_metric`(api.list/api.detail/page.view, 한국 날짜, `MetricService`가 REQUIRES_NEW로 커밋), 페이지 조회는 `PageViewFilter`(GET, 확장자 없는 경로, /admin·/manage 제외). 추정 비용은 `llm.input/output-price-per-million` 단가로 계산(모델별 단가 구분 없음). JVM 시간대는 Dockerfile에서 Asia/Seoul |
| 3 | **내 조건 저장**: 회원 계정에 조건(생애주기/가구상황/관심주제/나이/지역) 저장, 로그인하면 조건 입력 없이 바로 맞춤 혜택. 가입 직후 조건 입력으로 안내 | ✅ `MemberProfile`(`member_profiles`, 키=members.id, 코드는 콤마 문자열), `MemberProfileService`. 로그인 회원은 `/`→`/match`가 계정 조건 사용, `/profile`은 계정 조건으로 채우고 제출 시 계정에 저장. 비회원일 때 고른 조건(세션)은 가입/로그인 후 `/match`에서 계정에 이어서 저장. 조건 없이 가입하면 `/profile?welcome` 안내. 비회원은 기존대로 세션만 |
| 4 | **마이페이지**: 조건 변경, 비밀번호 변경(현재 비밀번호 확인), 회원 탈퇴(조건·즐겨찾기 함께 삭제) | ✅ `/mypage`(`MyPageController`, 로그인 필요). 조건 변경은 기존 `/profile`(계정 조건으로 채워지고 저장) 재사용. 비밀번호 변경 시 그 회원의 로그인 유지 토큰 전부 삭제. 탈퇴는 비밀번호+동의 체크 → 조건/토큰/계정 삭제 후 로그아웃(`MemberService.withdraw`). 헤더의 "아이디님"이 마이페이지 링크(모바일은 사람 아이콘만). 탈퇴 시 즐겨찾기 삭제는 5단계에서 추가함 |
| 5 | **즐겨찾기 DB**: 로그인 시 저장 버튼/즐겨찾기 화면이 DB 기준, 비회원은 localStorage 그대로 | ✅ `Favorite`(`favorites`, 회원+servId 유니크), `FavoriteService`, `FavoriteApiController`(`GET/POST/DELETE /api/favorites[/{servId}]`, 로그인 필요 — 비로그인은 401, 화면 경로는 로그인 페이지로 가도록 `SecurityConfig`에서 entry point를 명시). head의 `fav-api` 메타가 있으면(로그인) `favorites.js`가 API를, 없으면 localStorage를 쓴다. `/favorites`는 로그인 회원이면 DB에서 바로 렌더링. 탈퇴 시 즐겨찾기도 삭제 |
| 6 | **비회원 즐겨찾기 일괄 이전**: 로그인 직후 localStorage에 저장분이 있으면 "N개를 내 계정에 옮길까요?" → 일괄 저장 후 localStorage 비움 | ✅ `favorites.js`가 로그인 상태에서 localStorage 중 계정에 없는 혜택이 있으면 화면 상단에 안내(옮기기/나중에) → `POST /api/favorites/import`(JSON 배열, 최대 100개, 이미 있거나 없어진 혜택은 건너뜀, 받은 순서대로 저장해 최신순 유지). "나중에"는 이 탭(sessionStorage)에서만 다시 안 물음. 조건 입력 화면에도 `favorites.js`를 넣어 로그인 직후 어디로 가든 안내가 뜸 |
| 7 | **NEW 표시**: 배치가 혜택을 처음 받은 시각(`first_seen_at`)을 기록, 처음 들어온 날부터 **3일간** NEW 아이콘. 기존 4,818건은 날짜를 모르므로 NEW 아님. **매일 자동 배치는 하지 않기로 함** → 관리자가 수집 버튼을 누른 날 들어온 혜택만 NEW | ✅ `WelfareBenefit.firstSeenAt`/`isNew()`(들어온 날 포함 3일, `NEW_DAYS`). 배치(`carryOverFirstSeen`)는 기존 혜택의 값을 이어받고 처음 보는 혜택에만 수집 시각 기록, 빈 DB를 처음 채우는 수집은 기록 안 함. 카드/상세 제목 앞 NEW 배지, 수집 결과 메시지와 관리자 현황에 "새 혜택" 수 |
| 8 | **지난 방문 이후 새 혜택**: 로그인 회원에게 마지막 방문 이후 내 조건에 새로 맞는 혜택 수/목록 (7단계의 `first_seen_at` 사용) | ✅ `members.last_visited_at`(로그인 회원이 결과 화면을 본 시각). 세션의 첫 `/match`에서 직전 방문 시각을 세션(`MatchController.VisitSince`, 아이디 포함)에 고정하고 방문 시각 갱신 → 같은 세션에선 새로고침해도 안내 유지. 매칭 결과 중 `first_seen_at`이 직전 방문 이후인 혜택을 결과 화면 상단 패널에 표시(최대 10개 + "외 N건"), 없으면 "없어요", 첫 방문·비회원은 패널 없음 |

비밀번호 찾기는 메일 발송이 필요해 범위 밖 (발표에서 향후 계획으로 언급).

## 완료한 작업 기록

1. ✅ 상세조회 연동 (2026-11-27): `GET /benefits/{servId}` → `WelfareBenefitDetailService.getOrFetch()`가 DB 캐시 우선, 없으면 `WelfareApiService.fetchDetail()` 온디맨드 호출 후 저장. 화면은 `benefit-detail.html`, 공통 스타일은 `static/css/app.css`. ⚠️ 응답 루트 태그 `wantedDtl`은 **추정값** — 실키로 아직 호출 안 해봄, 처음 실키로 열어볼 때 파싱 결과(필드가 비어있지 않은지) 확인 필요
2. ✅ LLM 요약 (2026-11-27): `LlmSummaryService`가 **공식 Anthropic Java SDK**(`anthropic-java`, 계획했던 RestClient 직접 호출 대신)로 `claude-haiku-5-5`(effort low, 2026-11-27에 opus-5-5에서 비용 때문에 변경 — Haiku 5.5는 거절 시 서버측 fallback이 없어 fallback 설정 제거) 호출 → `llmSummary`에 캐싱, 상세 화면 "쉽게 말하면" 박스에 표시. 키는 환경변수 `ANTHROPIC_API_KEY` (`llm.api-key`) — 비어 있으면 요약을 건너뛰고 원문만 보여줌, 호출 실패해도 화면은 원문으로 뜨고 다음 조회 때 재시도. ⚠️ 실제 키로 요약 품질 확인은 아직 안 함 (프롬프트는 `LlmSummaryService.SYSTEM_PROMPT`). 첫 조회 시 요약 생성 때문에 몇 초 걸릴 수 있음. 요약 1건마다 `LLM 요약 servId=... model=... stop=... input_tokens=... output_tokens=...` INFO 로그를 남김 (비용 = 입력 토큰×$0.10 + 출력 토큰×$0.50 / 100만, haiku-5-5 기준 — 프롬프트 10만 토큰 이하일 때)
3. ✅ "최근 업데이트" 뱃지, 즐겨찾기 (2026-11-27). 즐겨찾기 화면은 `/favorites` 첫 요청 때 스크립트가 localStorage의 servId를 읽어 `/favorites?ids=...`로 다시 요청하는 방식 (서버는 최대 100개, 저장한 순서의 역순=최신순으로 렌더링). 결과 카드는 `templates/fragments/benefit-card.html` 프래그먼트로 매칭 결과/즐겨찾기 화면이 공유. ⚠️ 뱃지 기준 30일은 실데이터 분포를 안 보고 정한 값 — 실데이터로 대부분 카드에 붙거나 거의 안 붙으면 조정
4. 배포 — 플랫폼 공통 준비는 완료 (2026-11-27): `Dockerfile`(멀티스테이지, `SPRING_PROFILES_ACTIVE=prod`), `.dockerignore`(로컬 실키 파일 제외), `application-prod.yml`(환경변수 `PORT`, `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`(MySQL), `ADMIN_TOKEN`, `WELFARE_API_SERVICE_KEY`, `ANTHROPIC_API_KEY`). prod 기동·토큰 검사는 H2로 대신 띄워 확인했고, **Docker 이미지 빌드와 실제 MySQL 연결은 아직 미검증**(이 PC에 Docker 없음). **플랫폼은 Railway로 결정** (GitHub `IntHyunGyu/welfare-capstone` 연결 배포, 루트 `Dockerfile` 자동 인식은 공식 문서로 확인). Railway 앱 서비스 변수: `DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`, `DB_USERNAME=${{MySQL.MYSQLUSER}}`, `DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}` (MySQL 서비스 이름이 `MySQL`일 때), `PORT=8080`(Railway가 PORT를 주입하는지 문서로 확인 못 해서 명시), `WELFARE_API_SERVICE_KEY`, `ANTHROPIC_API_KEY`, `ADMIN_USERNAMES`(관리자 아이디) — 총 7개 (`ADMIN_TOKEN`, `REMEMBER_ME_KEY`는 2026-11-27에 불필요해져 삭제). 공개 주소는 Settings → Networking → Generate Domain (포트 8080). 남은 것: 계정/MySQL 생성 → 환경변수 등록 → 배포 후 `curl -X POST <주소>/admin/batch/welfare-list -H "X-Admin-Token: <값>"`으로 데이터 1회 적재

## 발표 시 강조 포인트

- "검색이 아니라 매칭"이라는 문제 정의 차별점
- LLM의 구체적 기여: 챗봇이 아니라 "행정 문구 → 쉬운 요약" 변환
- 실사용자 테스트(가족/지인) 피드백을 정량 결과로 제시
- "마감임박 알림"을 드롭한 이유를 데이터 근거(enfcEndYmd 대부분 무기한)로 설명할 수 있어야 함
- 캐싱 전략의 실효성을 수치로 제시 가능: 전체 4,818건을 25회 API 호출로 전량 수집(일 1,000회 quota의 2.5%)
