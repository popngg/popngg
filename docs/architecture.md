# 아키텍처

## 현재 모듈 구조

```text
popngg/
  popngg-api/          HTTP 계층
  popngg-application/  UseCase, port, service
  popngg-domain/       도메인 모델과 값 객체
  popngg-infra/        DB, Security, 외부 어댑터
```

## 의존 방향

권장 의존 방향은 다음과 같습니다.

```text
api -> application -> domain
infra -> application/domain
```

`application`은 port를 정의하고, `infra`가 adapter로 구현합니다. `api`는 request를 command로 변환하고 use case를 호출한 뒤 response를 반환합니다.

## 현재 주요 흐름

### 차트 조회

```text
ChartController
  -> Find...UseCase
  -> application service
  -> ChartQueryPort
  -> ChartQueryJpaAdapter
  -> ChartJpaRepository / Querydsl
```

### 로그인

```text
AuthController
  -> AuthenticateUserUseCase
  -> LoadUserPort
  -> PasswordHasherPort
  -> TokenPort
```

## 리팩토링 방향

- 프론트 표시용 그룹핑, 정렬, 계산은 application service에서 처리합니다.
- DB Entity는 스키마에 맞춘 영속성 모델로 제한하고, 도메인 규칙은 domain/application에 둡니다.
- songhash와 chart id는 역할을 분리합니다.
- FK 제거 후에도 application 계층에서 참조 무결성을 검증합니다.

## 계층별 문서화 기준

| 계층 | 문서화할 것 |
| --- | --- |
| API | endpoint, request, response, error code, 프론트 표시 정책 |
| Application | use case, 계산식, 트랜잭션 경계 |
| Domain | 값 범위, 랭크/메달 정책, 불변 규칙 |
| Infra | 테이블, 인덱스, 마이그레이션, 외부 연동 |
