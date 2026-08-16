# 기존 DB 구조 추정

이 문서는 이전 백엔드 저장소의 `domain`, `repository`, `service`, `dto` 코드를 읽고 유추한 기존 DB 구조입니다.

참고한 주요 파일:

- `src/main/java/gg/popn/popngg/domain/Chart.java`
- `src/main/java/gg/popn/popngg/domain/User.java`
- `src/main/java/gg/popn/popngg/domain/Playdata.java`
- `src/main/java/gg/popn/popngg/domain/History.java`
- `src/main/java/gg/popn/popngg/domain/RenewLog.java`
- `src/main/java/gg/popn/popngg/domain/LoginLog.java`
- `src/main/java/gg/popn/popngg/repository/*Repository.java`
- `src/main/java/gg/popn/popngg/service/RenewService.java`
- `src/main/java/gg/popn/popngg/service/PlaydataService.java`

## 전체 테이블

기존 백엔드 기준 핵심 테이블은 다음 6개로 보입니다.

| 테이블 | 역할 |
| --- | --- |
| `chart` | 곡 메타데이터와 난이도별 채보 정보를 함께 저장 |
| `"user"` | 유저 프로필, 팝토모 ID, 비밀번호, 팝클, 공개 여부 저장 |
| `playdata` | 유저별 채보 최신 플레이 상태 저장 |
| `history` | 플레이데이터 갱신 이력 스냅샷 저장 |
| `renew_log` | 갱신/등록 시도 로그 |
| `login_log` | 로그인 시도 로그 |

## chart

`Chart` 엔티티는 `chart` 테이블에 매핑됩니다.

| 컬럼 | 타입 추정 | nullable 추정 | 설명 |
| --- | --- | --- | --- |
| `chart_id` | BIGINT | NOT NULL | PK, auto increment |
| `song_hash` | VARCHAR | NOT NULL | 곡 식별자. 기존 API 외부 식별자 |
| `genre_name` | VARCHAR | NOT NULL | 장르명 |
| `song_name` | VARCHAR | NOT NULL | 곡명 |
| `version` | INT | NOT NULL | 수록 버전 |
| `difficulty` | INT | NOT NULL | 난이도 코드. 기존 1~4 |
| `level` | INT | NOT NULL | 레벨 |
| `jacket` | VARCHAR | NULL | 자켓 경로 또는 파일명 |
| `is_deleted` | INT | NOT NULL | 삭제 여부. 0이면 활성 |
| `is_upper` | INT | NOT NULL | Upper 여부 |
| `created_at` | DATETIME | NULL | 생성일 |

### 사실상 제약

- 활성 채보 조회는 `song_hash + difficulty + is_deleted = 0`으로 합니다.
- 곡 단위 조회는 `song_hash + is_deleted = 0`으로 합니다.
- 신규 chart 등록은 `song_hash + difficulty + is_deleted = 0` 중복을 막으려는 의도가 있습니다.
- 같은 곡의 난이도 4개가 모두 `chart` row로 들어갑니다.

### 기존 songhash 생성

`RenewService.makeHash` 기준:

```text
MD5(genreName + songName + version + optionalUpperMarker)
```

특정 곡만 `isUpper = 1`일 때 `"2"`를 추가합니다.

특수 처리 곡:

- 깨진 문자열로 보이는 `virkato...`
- `Popperz Chronicle`
- `ma plume`
- `Megalara Garuda`

## "user"

`User` 엔티티는 `"user"` 테이블에 매핑됩니다. SQL 예약어 회피를 위해 quote를 사용합니다.

| 컬럼 | 타입 추정 | nullable 추정 | 설명 |
| --- | --- | --- | --- |
| `user_id` | BIGINT | NOT NULL | PK, auto increment |
| `user_name` | VARCHAR | NOT NULL | 유저명 |
| `poptomo_id` | VARCHAR | NOT NULL | 팝토모 ID. 로그인/조회 키 |
| `popclass` | INT | NOT NULL | 유저 팝클 |
| `"character"` | VARCHAR | NOT NULL | 캐릭터 |
| `comment` | VARCHAR | NOT NULL | 코멘트 |
| `is_hidden` | INT | NOT NULL | 비공개 여부 |
| `password` | VARCHAR | NOT NULL | 기존 비밀번호. 평문 또는 외부 입력 secret 비교 |
| `normal_credit` | INT | NOT NULL | 노멀 크레딧 |
| `battle_credit` | INT | NOT NULL | 배틀 크레딧 |
| `local_credit` | INT | NOT NULL | 로컬 크레딧 |
| `created_at` | DATETIME | NOT NULL | 생성일 |
| `updated_at` | DATETIME | NOT NULL | 수정일 |
| `role` | VARCHAR | NOT NULL | `USER`, `BOT`, `ADMIN` 등 |

### 사실상 제약

- `poptomo_id`가 유저의 외부 식별자입니다.
- `findUserByPoptomoId`가 로그인, 갱신, 프로필 조회의 기본 조회입니다.
- `user_name`으로 BOT 유저를 찾는 로직이 있습니다.
- 랭킹에서는 `role = BOT` 유저를 제외합니다.
- 숨김 유저는 본인 또는 ADMIN이 아니면 프로필/플레이데이터 노출을 제한합니다.

## playdata

`Playdata` 엔티티는 `playdata` 테이블에 매핑됩니다.

| 컬럼 | 타입 추정 | nullable 추정 | 설명 |
| --- | --- | --- | --- |
| `playdata_id` | BIGINT | NOT NULL | PK, auto increment |
| `user_id` | BIGINT | NOT NULL | 유저 참조 |
| `chart_id` | BIGINT | NOT NULL | 채보 참조 |
| `"rank"` | INT | NOT NULL | 랭크 코드 |
| `medal` | INT | NOT NULL | 메달 코드 |
| `score` | INT | NOT NULL | 점수 |
| `popclass` | INT | NOT NULL | 해당 채보 기준 팝클 |

### 사실상 제약

- `user_id + chart_id`가 최신 플레이데이터의 사실상 unique key입니다.
- 기존 코드는 `findPlaydataByUserAndChart`로 upsert합니다.
- `playdata`는 최신 상태 테이블이고, 변경 이력은 `history`에 남깁니다.
- 곡별 랭킹은 `chart_id`로 모은 뒤 `score desc` 또는 `medal asc, score desc`로 정렬합니다.

### 팝클 계산식

기존 `RenewService.calculatePopclass` 기준:

```text
medalBonus =
  medal 1~4면 2000
  medal 1~8이면 추가 3000

popclass =
  floor((level * 10000 + score - 50000 + medalBonus) / 54.4)
  단, 최소 0
```

유저 팝클은 갱신된 플레이데이터의 `popclass`를 내림차순 정렬하고 상위 50개의 평균으로 계산합니다. 데이터가 50개 미만이어도 분모는 50입니다.

```text
user.popclass = floor(sum(top50PlaydataPopclass) / 50)
```

## history

`History` 엔티티는 `history` 테이블에 매핑됩니다.

| 컬럼 | 타입 추정 | nullable 추정 | 설명 |
| --- | --- | --- | --- |
| `history_id` | BIGINT | NOT NULL | PK, auto increment |
| `user_id` | BIGINT | NOT NULL | 유저 참조 |
| `chart_id` | BIGINT | NOT NULL | 채보 참조 |
| `"rank"` | INT | NOT NULL | 당시 랭크 |
| `medal` | INT | NOT NULL | 당시 메달 |
| `score` | INT | NOT NULL | 당시 점수 |
| `popclass` | INT | NOT NULL | 당시 팝클 |
| `created_at` | DATETIME | NULL | 기록일 |

### 생성 조건

등록 시에는 신규 플레이데이터마다 history를 생성합니다.

갱신 시에는 다음 조건 중 하나면 history를 생성합니다.

- 새 점수가 이전 점수보다 높음
- 새 rank 코드가 이전 rank 코드보다 작음

기존 랭크 코드는 낮을수록 좋은 랭크였던 것으로 보입니다.

## renew_log

`RenewLog` 엔티티는 `renew_log` 테이블에 매핑됩니다.

| 컬럼 | 타입 추정 | nullable 추정 | 설명 |
| --- | --- | --- | --- |
| `renew_id` | BIGINT | NOT NULL | PK, auto increment |
| `created_at` | DATETIME | NULL | 생성일 |
| `poptomo_id` | VARCHAR | NULL | 대상 팝토모 ID |
| `is_succeeded` | INT | NULL | 성공 여부 |
| `is_register` | INT | NULL | 최초 등록 여부 |
| `failure_reason` | VARCHAR | NULL | 실패 사유 |
| `chart_count` | INT | NULL | 입력 플레이데이터 수 |
| `ip` | VARCHAR | NULL | IP. TODO 상태 |

## login_log

`LoginLog` 엔티티는 `login_log` 테이블에 매핑됩니다.

| 컬럼 | 타입 추정 | nullable 추정 | 설명 |
| --- | --- | --- | --- |
| `login_id` | BIGINT | NOT NULL | PK, auto increment |
| `created_at` | DATETIME | NULL | 생성일 |
| `poptomo_id` | VARCHAR | NULL | 로그인 팝토모 ID |
| `"password"` | VARCHAR | NULL | 시도한 비밀번호 |
| `is_succeeded` | INT | NULL | 성공 여부 |
| `failure_reason` | VARCHAR | NULL | 실패 사유 |
| `ip` | VARCHAR | NULL | IP. TODO 상태 |

주의: 기존 코드가 로그인 실패/성공 로그에 입력 password를 저장합니다. 신규 설계에서는 저장하지 않아야 합니다.

## 주요 조회 패턴

| 기능 | 주요 조건 |
| --- | --- |
| 유저 조회 | `poptomo_id` |
| 유저 랭킹 | `user.popclass desc`, `role != BOT`, 숨김 처리 |
| 전체 chart 조회 | `is_deleted = 0`, `version asc`, `song_name asc` |
| 레벨별 chart 조회 | `level`, `is_deleted = 0` |
| 단일 chart 조회 | `song_hash`, `difficulty`, `is_deleted = 0` |
| 곡별 chart 조회 | `song_hash`, `is_deleted = 0` |
| 유저 전체 플레이데이터 | `user_id`, chart `is_deleted = 0` |
| 유저 팝클 테이블 | `user_id`, chart `is_deleted = 0`, `popclass desc`, `score desc`, 상위 50 |
| 곡별 랭킹 | `chart_id`, `score desc` 또는 `medal asc, score desc` |
| 히스토리 조회 | `poptomo_id`, `song_hash` |
| 레벨별 rank 집계 | `user_id`, chart `level`, `rank` |
| 레벨별 medal 집계 | `user_id`, chart `level`, `medal` |

## 기존 구조의 문제점

- `chart`가 song metadata와 chart metadata를 함께 들고 있습니다.
- 기존 songhash가 `genreName + songName + version` 기반이라 High☆Cheers 장르명 부활/변경에 취약합니다.
- `rank`가 점수 기반 계산과 외부 수집값 사이에서 의미가 섞여 있습니다.
- 로그인 로그가 password를 저장합니다.
- FK/JPA 연관관계에 강하게 묶여 있고, 새 정책인 FK 제거와 맞지 않습니다.
- `is_deleted`, `is_hidden`, `is_upper`, `is_succeeded` 등이 정수 플래그입니다.
- 랭크/메달/난이도 코드가 상수 테이블 없이 암묵적으로 사용됩니다.
- BOT 데이터 생성 로직이 `user_name`과 `role` 문자열에 의존합니다.

## 신규 DB 설계에 가져갈 것

- `playdata`는 최신 상태, `history`는 이력이라는 역할 분리는 유지할 가치가 있습니다.
- `user_id + chart_id` unique 개념은 유지하는 편이 좋습니다.
- `chart_id` 단위 곡별 랭킹 조회 패턴은 중요합니다.
- 레벨별 rank/medal/score 집계는 MVP에서도 API 요구 가능성이 높습니다.
- 갱신 로그는 유지하되 password와 민감정보는 제거해야 합니다.
- rank는 점수로 계산하지 않고 원천 데이터에서 수집한다는 새 정책을 우선합니다.
