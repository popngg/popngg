# 마이그레이션 계획

## 목표

- 서비스 중단 시간을 최소화하면서 신규 스키마로 전환합니다.
- 기존 플레이데이터, 유저, 차트 데이터를 손실 없이 이전합니다.
- songhash 변경에 따른 매핑 테이블을 남겨 롤백과 추적이 가능하게 합니다.

## 단계

### 1. 스키마 설계

- `song`, `chart`, `playdata`, `user` 변경안 확정
- FK 제거 범위 확정
- 인덱스와 unique 제약 확정
- songhash 생성 규칙 확정

### 2. 테이블 생성

- 신규 테이블 생성
- 기존 테이블은 즉시 삭제하지 않음
- 신규 테이블명 또는 suffix는 운영 전환 전략에 맞춰 결정

### 3. 데이터 싱크

- 기존 `chart`에서 `song`과 `chart`로 분리
- 기존 `playdata.chart_id`를 신규 `chart_id`로 매핑
- 기존 유저 비밀번호를 salt 정책에 맞춰 재해싱
- songhash old/new 매핑 테이블 생성

### 4. 검증

필수 검증:

- 기존 곡 수와 신규 song 수 비교
- 기존 chart 수와 신규 chart 수 비교
- playdata row count 비교
- 유저 row count 비교
- songhash 후보별 중복 여부 확인
- 신규 unique 제약 위반 여부 확인

### 5. 애플리케이션 라우팅 스위치

- 읽기 API를 신규 스키마로 전환
- 쓰기 API를 신규 스키마로 전환
- 필요하면 feature flag 또는 profile 기반 라우팅 사용

### 6. 최종 전환

1. 서버를 닫습니다.
2. 최종 데이터 싱크를 수행합니다.
3. 검증 쿼리를 다시 실행합니다.
4. 애플리케이션 설정을 신규 DB/테이블로 고정합니다.
5. 서버를 다시 엽니다.
6. 주요 API smoke test를 실행합니다.

## 비밀번호 마이그레이션 초안

```text
for each user:
  salt = user.poptomo_id
  migrated_input = user.password + ":" + salt
  user.password = passwordEncoder.encode(migrated_input)
```

로그인 시에는 사용자가 입력한 원문 비밀번호가 아니라 기존 hash를 어떻게 복원/비교할 수 있는지 확인해야 합니다. 현재 저장값이 이미 원문 비밀번호가 아닌 hash라면, 사용자가 입력한 비밀번호로는 `legacy_hash + salt`를 재현할 수 없습니다.

따라서 다음 중 하나를 확정해야 합니다.

| 방식 | 설명 |
| --- | --- |
| A | 기존 저장값이 사실상 로그인 secret이면 그대로 감싼 뒤, 로그인 입력도 같은 secret을 받음 |
| B | 기존 평문 입력을 legacy hash 함수로 먼저 변환한 뒤 salt 적용 |
| C | 전환 후 최초 로그인 때 새 hash로 업그레이드 |
| D | 비밀번호 재설정 플로우로 강제 전환 |

## 롤백 전략

- 기존 테이블은 전환 후 일정 기간 read-only로 보존합니다.
- old/new songhash 매핑을 보존합니다.
- 신규 API 배포 후 문제가 생기면 기존 `/api/v2` 읽기 경로로 되돌릴 수 있게 합니다.

## 마이그레이션 산출물

- DDL
- 데이터 변환 스크립트
- 검증 SQL
- old/new id 매핑 결과
- 실패 row 리포트
- 전환 체크리스트
