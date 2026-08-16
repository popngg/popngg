# LLM 컨텍스트

이 파일은 LLM이 프로젝트를 빠르게 이해하기 위한 압축 컨텍스트입니다. 코드 수정 전에는 최신 코드와 이 문서를 함께 확인해야 합니다.

## 프로젝트 한 줄 설명

popn.gg는 아케이드 리듬게임 `pop'n music`의 곡, 채보, 플레이데이터, 랭킹 정보를 제공하는 서비스입니다.

## 현재 리팩토링 기준

- 기준 게임 버전은 `pop'n music High☆Cheers!!`입니다.
- High☆Cheers에서 장르명 표기가 부활했습니다.
- High☆Cheers에서 랭크 체계가 변경되어 `S+`, `AA+`, `A+`, `B+`를 지원해야 합니다.
- High☆Cheers에서 `EASY` 난이도명이 `LIGHT`로 바뀐 것으로 관찰됩니다. 내부 code와 표시 label을 분리해야 합니다.
- 랭크는 서버 내부에서 점수로 계산하지 않습니다. clear 여부에 따라 달라질 수 있으므로 크롤링/갱신 시 원천 데이터의 랭크를 함께 저장합니다.

## 기술 스택

- Java 17
- Spring Boot 3.2.4
- Gradle 멀티모듈
- MySQL 8.0
- Spring Security + JWT
- SpringDoc OpenAPI
- JPA, Querydsl

## 모듈 경계

| 모듈 | 역할 |
| --- | --- |
| `popngg-api` | HTTP Controller, request/response DTO, 웹 설정 |
| `popngg-application` | UseCase, service, port, application DTO |
| `popngg-domain` | 도메인 모델, 값 객체, 도메인 예외 |
| `popngg-infra` | DB Entity, JPA Repository, mapper, security adapter |

## 현재 관찰된 구조

- `ChartEntity`는 `chart` 테이블에 곡 메타데이터와 채보 데이터를 함께 저장합니다.
- `PlaydataEntity`는 `UserEntity`, `ChartEntity`를 `ManyToOne`으로 참조합니다.
- `UserEntity`는 `"user"` 테이블을 사용하며 `poptomo_id`, `password`, `role` 등을 가집니다.
- `Rank`, `Medal`은 현재 정수 래퍼 값 객체이고 구체적인 enum 제한은 없습니다.
- `AuthenticateUserService`에는 해싱 검증 로그인과 평문/기존값 비교용 `loginWithoutHash`가 함께 있습니다.

## 리팩토링에서 바뀌어야 하는 것

- 게임 버전 기준: High☆Cheers
- 랭크 추가: `S+`, `AA+`, `A+`, `B+`
- 랭크 저장 정책: score 기반 계산 금지, 크롤링된 rank 저장
- 난이도 표시 변경: `EASY` 대신 `LIGHT` 지원
- 메달 추가: 어시이지
- 자켓/곡 표시 변경: 장르명 추가, 어퍼딱지 표시 삭제
- `chart`에서 `song` 메타데이터 분리
- songhash 재생성: 장르명이 새로 생기는 곡이 많아 기존 hash 정책 변경 필요
- 유저 비밀번호: 기존 해시값에 플레이어별 salt를 넣어 일괄 재해싱
- 비밀번호 복구: 이메일 기반 복구 기능 포함
- FK 제거: DB 차원의 cascading 비용이 리턴보다 큼
- API 응답: 프론트에서 데이터 가공하지 않도록 백엔드에서 계산/그룹핑/표시 데이터 제공

## 열린 질문

- songhash 유니크 키는 어떤 조합으로 확정할 것인가?
- 짠판정/짠게이지를 Song metadata로 둘 것인가, Chart metadata로 둘 것인가?
- 이메일 인증을 MVP 1차에 포함할 것인가, 이메일 등록 후 복구만 먼저 열 것인가?
- 갱신 코드는 KONAMI가 JSON을 제공하는지에 따라 구현 방식이 달라지는가?

## 코드 수정 시 주의

- 기존 멀티모듈 경계를 유지합니다.
- HTTP DTO와 도메인 모델을 직접 섞지 않습니다.
- rank, medal, difficulty는 정수 코드와 표시 label을 분리합니다.
- rank는 score에서 계산하지 말고 갱신 원천 데이터에서 받은 값을 저장합니다.
- High☆Cheers 전환처럼 버전별 정책이 바뀔 수 있는 값은 하드코딩보다 정책 객체 또는 상수 테이블을 우선 검토합니다.
- FK 제거 결정이 확정되면 JPA 연관관계도 엔티티 id 기반 참조로 바꿀지 검토해야 합니다.
- 마이그레이션 문서와 실제 스키마 변경을 함께 업데이트합니다.
