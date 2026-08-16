# 데이터 모델링

## 가장 크게 바뀌는 영역

- High☆Cheers 기준 게임 시스템 반영
- 팝클 계산식
- 플레이데이터 관리 방식
- 유저 비밀번호 저장 및 이메일 기반 복구 방식
- `chart`와 `song` 메타데이터 분리
- songhash 재설계
- 랭크, 메달, 난이도 상수의 버전별 정책화

## 현재 모델의 문제

현재 `chart` 테이블은 곡 메타데이터와 채보 메타데이터를 함께 가지고 있습니다.

| 컬럼 | 성격 |
| --- | --- |
| `song_hash` | 곡 식별자 |
| `genre_name` | 곡 메타데이터 |
| `song_name` | 곡 메타데이터 |
| `version` | 곡 메타데이터 또는 수록 버전 |
| `difficulty` | 채보 메타데이터 |
| `level` | 채보 메타데이터 |
| `jacket` | 표시 메타데이터 |
| `is_upper` | 채보/표시 메타데이터 |

High☆Cheers에서 장르명 표기가 부활했고 랭크 체계가 변경되었으므로, 표시 정책을 기존 정수 코드에 묶어두면 다음 버전 변경 때 다시 크게 흔들릴 수 있습니다.

## 제안 스키마 초안

### song

곡 단위로 변하지 않는 정보를 둡니다.

| 컬럼 | 설명 |
| --- | --- |
| `song_id` | 내부 PK |
| `song_hash` | 외부/API 식별자 |
| `genre_name` | 장르명 |
| `song_name` | 곡명 |
| `artist_name` | 작곡가/아티스트 |
| `version` | 수록 버전 |
| `jacket_url` | 자켓 경로 또는 URL |
| `created_at` | 생성일 |
| `updated_at` | 수정일 |

### chart

난이도별 채보 정보를 둡니다.

| 컬럼 | 설명 |
| --- | --- |
| `chart_id` | 내부 PK |
| `song_id` | 애플리케이션 참조용 song id |
| `difficulty_code` | 내부 난이도 코드 |
| `level` | 레벨 |
| `is_upper` | 어퍼 여부. 표시 딱지는 제거하되 데이터 보존 여부 검토 |
| `strict_judgement` | 짠판정 여부 후보 |
| `strict_gauge` | 짠게이지 여부 후보 |
| `is_deleted` | 삭제 여부 |
| `created_at` | 생성일 |
| `updated_at` | 수정일 |

### playdata

유저별 chart 플레이 상태를 둡니다.

| 컬럼 | 설명 |
| --- | --- |
| `playdata_id` | 내부 PK |
| `user_id` | 유저 id |
| `chart_id` | 채보 id |
| `rank_code` | 크롤링된 랭크 코드 |
| `medal_code` | 크롤링된 메달 코드 |
| `score` | 점수 |
| `popclass` | 당시/계산 팝클 |
| `updated_at` | 최종 갱신일 |

`rank_code`는 `score`에서 계산하지 않습니다. clear 여부에 따라 랭크가 달라질 수 있으므로, 갱신 원천 데이터에서 랭크를 함께 수집해서 저장합니다.

## 버전별 상수 정책

High☆Cheers에서 랭크와 난이도 표시가 변경되었습니다.

### rank_policy

이 테이블은 랭크 계산용이 아니라 표시/정렬/검증용입니다.

| 컬럼 | 설명 |
| --- | --- |
| `rank_code` | 내부 코드 |
| `version_from` | 적용 시작 버전 |
| `version_to` | 적용 종료 버전. 현재 버전이면 null |
| `label` | 표시명. 예: `S+`, `AA+` |
| `sort_order` | 정렬 순서 |
| `reference_min_score` | 참고용 최소 점수 |
| `reference_max_score` | 참고용 최대 점수 |
| `requires_clear_for_reference` | 참고 점수 구간이 클리어 조건을 전제로 하는지 |

High☆Cheers 참고 구간:

| 랭크 | 참고 점수 구간 | 비고 |
| --- | --- | --- |
| `S+` | 99,000 이상 | 클리어 |
| `S` | 98,000 - 98,999 | 클리어 |
| `AAA` | 95,000 - 97,999 | 클리어 |
| `AA+` | 93,000 - 94,999 | 클리어 |
| `AA` | 90,000 - 92,999 | 클리어 |
| `A+` | 86,000 - 89,999 | 클리어 |
| `A` | 82,000 이상 | clear 여부 영향 있음 |
| `B+` | 77,000 - 81,999 | clear 여부 영향 있음 |
| `B` | 72,000 - 76,999 | clear 여부 영향 있음 |
| `C` | 62,000 - 71,999 | clear 여부 영향 있음 |
| `D` | 50,000 - 61,999 | clear 여부 영향 있음 |
| `E` | 49,999 이하 | clear 여부 영향 있음 |

### difficulty_policy

| 컬럼 | 설명 |
| --- | --- |
| `difficulty_code` | 내부 코드 |
| `version_from` | 적용 시작 버전 |
| `version_to` | 적용 종료 버전 |
| `label` | 표시명. 예: `LIGHT`, `NORMAL`, `HYPER`, `EX` |
| `short_label` | 짧은 표시명. 예: `L`, `N`, `H`, `EX` |
| `sort_order` | 정렬 순서 |

기존 `EASY`는 High☆Cheers 기준 표시에서 `LIGHT`로 보일 수 있도록 정책으로 관리합니다.

### medal_policy

| 컬럼 | 설명 |
| --- | --- |
| `medal_code` | 내부 코드 |
| `version_from` | 적용 시작 버전 |
| `version_to` | 적용 종료 버전 |
| `label` | 표시명 |
| `short_label` | 짧은 표시명 |
| `sort_order` | 정렬 순서 |

신규 메달 `어시이지`를 추가합니다.

## FK 제거 정책

DB FK는 제거하되, 애플리케이션에서 다음을 보장합니다.

- `playdata.user_id`가 존재하는 유저를 가리키는지 저장 전에 검증
- `playdata.chart_id`가 존재하는 채보를 가리키는지 저장 전에 검증
- 삭제는 hard delete보다 `is_deleted`를 우선 사용
- 정합성 점검 배치 또는 관리용 쿼리를 별도 운영

## songhash 후보

장르명이 새로 생기는 곡이 많아 기존 songhash는 재생성이 필요합니다.

| 옵션 | 구성 | 장점 | 리스크 |
| --- | --- | --- | --- |
| Option 1 | 곡명 + 작곡가 + Upper | 장르명 변경에 강함 | 동명/동작곡가 곡 충돌 가능 |
| Option 2 | 장르명 + 곡명 + 작곡가 + Upper | 현재 표시 정보 반영 | 장르명 추가/변경 시 hash 변경 |
| Option 3 | 장르명 + 곡명 + 작곡가 + Upper + 버전 | 충돌 가능성 낮음 | 버전 정정 시 hash 변경 |

## 권장 검증 쿼리

마이그레이션 전 실제 데이터로 후보별 중복을 확인합니다.

```sql
-- Option 1 중복 확인
SELECT song_name, artist_name, is_upper, COUNT(*) AS cnt
FROM source_chart
GROUP BY song_name, artist_name, is_upper
HAVING COUNT(*) > 1;

-- Option 2 중복 확인
SELECT genre_name, song_name, artist_name, is_upper, COUNT(*) AS cnt
FROM source_chart
GROUP BY genre_name, song_name, artist_name, is_upper
HAVING COUNT(*) > 1;

-- Option 3 중복 확인
SELECT genre_name, song_name, artist_name, is_upper, version, COUNT(*) AS cnt
FROM source_chart
GROUP BY genre_name, song_name, artist_name, is_upper, version
HAVING COUNT(*) > 1;
```

## 인덱스 초안

| 테이블 | 인덱스 | 목적 |
| --- | --- | --- |
| `song` | unique `song_hash` | API 식별자 조회 |
| `song` | `genre_name`, `song_name` | 검색 |
| `song` | `version` | 버전 필터 |
| `chart` | unique `song_id`, `difficulty_code` | 곡별 난이도 단일화 |
| `chart` | `difficulty_code`, `level` | 레벨/난이도 검색 |
| `playdata` | unique `user_id`, `chart_id` | 유저별 최신 플레이데이터 |
| `playdata` | `chart_id`, `score` | 곡별 랭킹 |
| `playdata` | `user_id`, `popclass` | 유저 통계 |

## 비밀번호 해싱

현재 요구사항은 기존 해시값에 플레이어별 salt를 추가해 DB를 일괄 갱신하는 것입니다.

권장 초안:

- salt 후보: `poptomo_id`
- 입력값: `legacy_password_hash + ":" + poptomo_id`
- 저장값: Spring Security `PasswordEncoder` 결과
- 마이그레이션 후 `loginWithoutHash` 제거

주의: bcrypt/argon2 계열은 자체 salt를 포함합니다. 여기서 말하는 salt는 legacy hash를 한 번 더 감싸기 위한 pepper-like migration input에 가깝습니다. 실제 정책명은 구현 시 명확히 정해야 합니다.

## 비밀번호 찾기

비밀번호 찾기는 이메일 기반 복구로 진행합니다.

필요 컬럼/테이블:

- `users.email`
- `users.email_verified_at`
- `password_reset_tokens`

토큰 정책:

- 원문 토큰은 DB에 저장하지 않습니다.
- `sha256(token)`만 저장합니다.
- 토큰은 짧은 TTL과 1회 사용을 전제로 합니다.
- 이메일 존재 여부는 API 응답에서 노출하지 않습니다.
