# CPI / SPI 원천 데이터 조사 및 기초 분석 계획

작성일: 2026-09-23

상태: 데이터 추출·오프라인 통계·비동기 작업 큐·S3 저장·Discord 접수 및 완료 알림 구현. 실제 운영 DB 추출 및 운영 S3/Discord 전송은 아직 검증하지 않았다. 실행 방법과 구현상의 한계는 `analysis/cpi_spi/README.md`를 참고한다.

## 1. 목표와 범위

- CPI는 유저·채보·공식 레벨·일반 클리어 여부만 사용한다. 일반 클리어, 풀콤보, 퍼펙트는 모두 동일한 성공이다.
- SPI는 유저·채보·공식 레벨·보유 최고 점수만 사용한다. 메달과 클리어 여부로 점수 표본을 걸러내지 않는다.
- 공통 원본에서 CPI와 SPI 유효성 플래그를 독립적으로 파생한다.
- 이번 단계는 데이터 조사, 추출, 품질 검증, 분포와 관계 탐색까지다. 추가 요구에 따라 매일 06:00(KST) 및 Discord 수동 접수, 비공개 S3 저장, admin bot JSON 완료 알림을 포함한다. 레이팅 계산·공개, 자동 파티션, 투표는 후속 단계다.
- 짠게이지, 노트 수, BPM, 채보 속성, BAD 구간 가중치, 공식 레벨별 기본 레이팅을 만들지 않는다. 구 팝클도 본 분석의 입력 실력값으로 사용하지 않는다.
- 전체 레벨의 현황을 조사하고 40~50, 특히 47/48/49/50을 비교한다. 분석 전에 특정 레벨 범위를 최종 대상으로 선정하지 않는다.
- 50명 미만 채보도 분포 조사에는 포함한다. 향후 산출 대상 기준인 50명 이상 충족 여부를 CPI·SPI별로 따로 표시한다.

## 2. 예비 조사에서 확인한 코드와 구조

경로는 저장소 루트 기준이다. 운영 DB 적용 상태는 별도 검증한다.

| 항목 | 확인 내용 | 근거 |
| --- | --- | --- |
| 계정 | `users.user_id`가 식별자이며 role을 저장한다 | `popngg-infra/src/main/resources/db/migration/V1__baseline_account_and_security.sql` |
| 프로필 | `user_profiles.user_id`가 PK이며 공개 여부는 `is_hidden`이다 | V1 |
| 곡/채보 | `songs` 1개에 `charts` 여러 개. 채보 식별은 song_id, difficulty_code, is_upper 조합으로 UNIQUE | `V2__baseline_music_catalog.sql` |
| 공식 레벨 | `charts.level`; 난이도 종류는 별도 `difficulty_code` | V2 |
| 현재 기록 | `playdata`는 `(user_id, chart_id)` UNIQUE | `V3__baseline_playdata_and_history.sql` |
| 최고 점수 | `all_time_score`, 버전 점수는 `version_score` | V3, `PlaydataUpsertPolicy.java` |
| 버전 점수 가용성 | `version_score_known`을 별도로 저장한다 | `V8__track_version_best_score_availability.sql` |
| 메달 | 현재 코드는 1~7 일반 클리어 이상, 8~10 실패, 11 EASY, 12 LONGOFF, 13 NONE | `popngg-domain/src/main/java/gg/popn/domain/game/policy/MedalPolicy.java` |
| 기록 갱신 | 점수는 통상 최대값을 유지하나 버전 RESET 시 원천 값으로 재설정한다. 메달은 관측한 값으로 갱신한다 | `popngg-application/src/main/java/gg/popn/application/playdata/service/PlaydataUpsertPolicy.java` |
| 기록 이력 | 등록·점수 향상·메달 변경 등 이벤트를 저장한다. 매 플레이 로그가 아니다 | 같은 디렉터리의 `PlaydataHistoryPolicy.java` |
| 기록 시각 | `updated_at`은 DB 변경 시각. JDBC 갱신 SQL이 플레이 시각을 입증하지 않는다 | `popngg-infra/src/main/java/gg/popn/infra/db/adapter/PlaydataImportJdbcAdapter.java` |
| 이관 주의 | V12가 과거 기록의 current_version을 올리고 version_score를 0/unknown으로 설정했다 | `V12__promote_legacy_playdata_to_current_version.sql` |
| 메달 이관 | 구형 메달 코드가 별도 보정되었다 | `V7__align_medal_codes_with_high_cheers.sql`, `V16__correct_legacy_medal_codes.sql` |
| 레거시 Entity 불일치 | `ChartEntity`는 `chart`, `PlaydataEntity`는 구형 score/medal 컬럼을 참조한다. 최신 스키마와 다르다 | `popngg-infra/src/main/java/gg/popn/infra/db/entity/` |

주의: 최고 점수와 메달이 같은 플레이의 결과라는 보장은 없다. `all_time_score_version`도 실제 달성 시점을 완전하게 보장하는 값으로 취급하지 않는다. 위 baseline DDL에는 관계 컬럼은 있지만 FK 선언이 없어 고아 레코드를 실제로 검사해야 한다.

추가 추적: Domain → Repository/JDBC adapter → import/export 경로, 계정 탈퇴·삭제 처리, 프로필 누락, BOT 제외 정책, 점수 0과 NONE의 원천 의미, `last_played_at`의 전체 쓰기 경로, 기록 시각의 이관 영향을 조사한다.

## 3. 단계 A: 실제 DB와 추출 범위 확인

1. 설정에서 실제 분석 대상 DB와 접속 방법을 확인한다. 비밀번호·이메일·토큰 등은 출력하거나 추출하지 않는다.
2. `information_schema` 및 Flyway 적용 이력으로 테이블·컬럼·인덱스·UNIQUE 제약과 코드의 일치 여부를 확인한다. 구형 `chart` Entity로 추출하지 않는다.
3. 전체 사용자·프로필·채보·기록 수와 공개 유저·BOT·삭제 채보·누락 관계별 수를 센다.
4. 코드와 운영 DB가 다르면 차이를 먼저 보고한다. 운영 스키마를 분석을 위해 변경하지 않는다.
5. 일관된 읽기 전용 스냅샷으로 추출한다. 가능한 경우 읽기 복제본 또는 동일 스냅샷의 로컬 덤프를 이용하며 DB 시간대·기준 시각을 기록한다.
6. 초기 주 분석은 공개 유저, BOT 제외, 현재 삭제되지 않은 채보를 대상으로 한다. 제외 집단은 품질 요약에서 건수로 보고한다.

실제 DB에 접근할 수 없다면 도구·테스트 결과와 실제 통계를 명확히 구분한다. 상위 랭킹 API 일부나 테스트 fixture를 전체 사용자 데이터로 대체하지 않는다. 사용자 약 700명이라는 대화상의 수치는 실측 결과로 기재하지 않는다.

## 4. 단계 B: 공통 원천 데이터셋

단위는 유저 × 채보이며 반복 플레이 횟수가 아니다.

| 필드 | 원천 / 의미 |
| --- | --- |
| userId | users.user_id; 외부 공유본은 별도 익명 식별자 사용 |
| chartId / songId | charts.chart_id / charts.song_id |
| level | 스냅샷 시점 charts.level |
| cleared | true / false / null. CPI 적격 결과만 이진값 |
| medal | 원천 medal_code 보존 |
| score | all_time_score. SPI 기본 점수 |
| cpiEligible / spiEligible | 각 분석에 독립적인 사용 가능 여부 |
| exclusionReasons | 모델별 제외 사유. 다중 사유와 단계별 탈락 수 구분 |
| difficultyCode / isUpper | 채보 식별 검증용 |
| currentVersion / allTimeScoreVersion | 버전 편향 점검용 |
| versionScore / versionScoreKnown | 버전 최고점 비교·미갱신 조사용 |
| lastPlayedAt / recordUpdatedAt / lastRenewLogId | 날짜 품질 조사용; 플레이 시각으로 임의 해석 금지 |
| snapshotAt | 추출 기준 시각 |

기본 메달 정책: 1~7 → cleared=true, 8~10 → false, 11/12/13 및 알 수 없는 값 → null. EASY/LONGOFF는 일반 CLEAR 성공·실패 어느 쪽으로도 강제하지 않고 별도 집계한다. 원천에 존재하지 않는 유저×채보 조합은 생성하지 않는다.

SPI는 유효한 최고 점수와 플레이 증거가 있으면 실패·EASY·LONGOFF 기록도 포함한다. 메달 누락만으로 유효한 양수 점수를 제외하지 않는다. 점수 범위는 0~100000을 검증하되, 0은 미플레이 기본값인지 실제 기록인지 구분하고 판별 불가하면 별도 집계한다. 점수가 잘못돼도 독립적으로 유효한 클리어 결과는 CPI에서 사용할 수 있다.

사용자/채보/곡 연결 누락, 중복 키, 잘못된 레벨은 사유와 건수를 보존한다. 중복 발견 시 임의로 최대 점수 행을 골라 메달까지 합치지 않는다. 원천 의미를 확인하거나 해당 키를 격리한다.

원본 품질 확인은 LEFT JOIN과 사전 건수 집계로 수행하여 INNER JOIN이 고아 레코드를 조용히 버리지 않게 한다. 플레이 0명 채보도 카탈로그 기준 통계에 남긴다.

## 5. 단계 C: 기초 통계와 파일

공통 정의:

- `playerCount`: 플레이 증거가 있는 고유 유저 수. `cpiPlayerCount`, `spiPlayerCount`, `ambiguousRecordCount`를 함께 제공한다.
- `clearRate = clearCount / cpiPlayerCount`. 표본 0이면 null이다.
- 점수 평균·분위수는 spiEligible 기록으로 계산하며 기록 수를 함께 표시한다.
- 분위수는 정렬 값의 위치 `(n-1)*q`에 대한 선형 보간으로 고정한다. median=scoreP50이다.
- 레벨 전체 clearRate는 해당 레벨의 유효 유저×채보 기록을 분모로 계산한다. 유저 수로 나누지 않는다.
- 레벨 전체 점수는 기록 가중 분포임을 표시하고, 채보별 평균을 동일 가중한 값도 별도로 제공하여 인기곡 영향을 비교한다.

### 채보 통계: chart_stats.csv/json

chartId, songId, level, playerCount, cpiPlayerCount, spiPlayerCount, clearCount, clearRate, averageScore, medianScore, scoreP10/P25/P50/P75/P90/P95/P99, 50명 충족 여부, 원천 메달별 count/rate.

메달 분포는 서로 배타적인 failed(8~10), bad21Plus(7), bad6To20(6), bad1To5(5), fullCombo(2~4), perfect(1), easy(11), longOff(12), none/unknown으로 제공한다. 분모는 `medalObservationCount`로 명시한다.

요청한 bad20OrLessRate는 5~6의 일반 CLEAR 메달, bad5OrLessRate는 5의 일반 CLEAR 메달로 정의한다. FC/PERFECT는 별도 범주다. 이 누적 지표는 서로 겹치므로 배타적 분포와 합산하지 않는다. 정확한 BAD 수 추정은 하지 않는다.

### 레벨 통계: level_stats.csv/json

level, 고유 playerCount, 전체 chartCount, 플레이된 chartCount, 각 분석 유효 recordCount, clearCount/clearRate, 점수 평균·중앙값·전체 분위수, CPI/SPI 각각 표본 50명 이상 채보 수와 비율.

채보별 플레이어 수 구간: 0 / 1~9 / 10~29 / 30~49 / 50~99 / 100+. 성공·실패 한쪽만 존재하는 채보도 집계한다. 47/48/49/50 비교표를 보고서에 포함한다.

### 유저 통계: user_stats.csv, user_level_stats.csv 및 JSON

playedChartCount, cpiEligibleChartCount, spiEligibleChartCount, clearCount/clearRate, averageScore/medianScore와 동일 항목의 레벨별 분해. 활성 카탈로그 대상 통계임을 명시한다. 계정은 있지만 기록이 없는 사용자도 별도로 센다.

### 점수 → 백분위: score_percentiles.csv/json

채보별 관측 점수마다 nBelow/nEqual/nAbove와 ECDF=`count(score<=S)/n`, 상위 비율=`count(score>=S)/n`을 제공한다. 동점이 있으면 두 비율은 단순 보수 관계가 아니다. 90000/93000/95000/98000/99000의 비교표도 출력한다.

표본 50명에서 P99는 최상위 몇 개 기록에 매우 민감하므로 순위와 표본 수를 함께 제공한다. 경험적 점수 분포를 SPI 값으로 명명하지 않는다.

## 6. 단계 D: 모델링 가능성 탐색

### CPI 관계 탐색 — CPI 값은 계산하지 않음

- 대상 채보를 제외한 다른 채보들의 레벨별 clearCount/playedCount로 관측 프로필을 만든다.
- 공통 플레이 채보가 충분한 유저끼리 비교하고 공통 채보 수를 보고한다. 서로 다른 곡을 플레이한 유저의 단순 clearRate를 곧바로 실력 서열로 해석하지 않는다.
- 채보를 기준 집합과 검증 집합으로 나누어 기준 집합의 관측 성과 구간별로 검증 채보 clearRate를 출력한다. 기준 집합·분할 seed를 저장하고 해당 채보 결과가 그 유저의 구간 결정에 포함되지 않게 한다.
- 레벨별 비교, 공통 채보 기반 비교를 병행한다. 경향이 증가하지 않아도 단조 보정하지 않고 원래 표본 수·성공 수·구간별 불확실성을 보여준다.
- 전원 성공/전원 실패, 유저 실력 범위 부족, 비교 그래프의 연결성·공통 채보 수를 점검한다. 50명 충족만으로 모델 식별 가능하다고 결론 내리지 않는다.
- 비교 가능한 데이터가 부족한 경우 그 사실과 건수를 보고한다. 새로운 최소 성공/실패 수를 확정 필터로 추가하지 않는다.

### SPI 관계 탐색

- 같은 점수의 채보별 경험 백분위 차이를 표로 보여준다.
- 바닥·천장 효과, 동점 밀도, 표본 규모, 인기곡 편향, 전체 최고점과 알려진 버전 최고점의 차이를 조사한다.
- CPI 적격성으로 SPI 표본을 제한하지 않는다. 실제 기록의 FAILED 고득점 사례도 SPI에 포함되는지 검증한다.

## 7. 단계 E: 품질과 편향 보고

- NO PLAY와 FAILED, 메달 없음+양수 점수, 점수 0+메달의 교차표.
- 이관/미갱신/최근 갱신 집단별 기록 수와 레벨 분포. current_version만으로 최근 플레이로 간주하지 않는다.
- last_played_at 채움률 및 출처, updated_at의 마이그레이션 일괄 변경 여부. 관측된 갱신 시각과 실제 플레이 시각의 차이를 명시한다.
- 과거 저레벨 점수가 현재 능력을 과소평가하는지는 날짜·이력 가용 범위에서만 탐색한다. 최고 기록 스냅샷만 있으면 인과적으로 확인할 수 없다고 적는다.
- 플레이 횟수·옵션·과거 모든 실패는 없을 수 있다. 따라서 보유 기록 분포이며 1회 시도 성공률이나 현재 실력의 확정 측정치가 아니다.
- 소수 표본·고레벨 선택 편향·신규/해금곡 참여 편향을 설명하되 임의 보정은 하지 않는다.
- 전체 → 정책상 대상 → 관계 유효 → CPI 적격 / SPI 적격으로 건수를 대조한다. 제외 건수 합산 시 중복 사유를 별도 처리한다.

## 8. 구현 및 실행 순서

1. 이 계획 작성과 실제 스키마/접근 경로 조사.
2. `analysis/cpi_spi/`에 읽기 전용 SQL, 추출 실행기, 오프라인 분석기, README, 테스트 작성. 실제 DB 종류/버전 확인 후 쿼리 확정.
3. 합성 fixture로 미플레이·실패·메달별 이진화·잘못된 점수·중복·고아·0명 채보·동점 분위수·0분모·독립된 CPI/SPI 표본을 검증.
4. 실제 DB 스냅샷 추출. 익명/내부 ID만 사용하고 불필요한 프로필 정보는 포함하지 않음.
5. 원천 건수와 출력 건수 대조, 모든 통계 산출, 47~50 비교 및 분포 확인.
6. 실제 결과 보고서에 모델 후보와 다음 실험을 제안. 최종 공식은 선정·구현하지 않음.

분석 출력 위치: `build/analysis/cpi-spi/<snapshot-id>/` (현재 gitignore의 build 대상). 원천 개인별 데이터는 Git에 커밋하지 않는다. 계획·코드·합성 fixture는 추적 가능하다.

출력: records.csv/jsonl, chart_stats.csv/json, level_stats.csv/json, user_stats.csv/json, user_level_stats.csv/json, score_percentiles.csv/json, cpi_readiness.csv/json, data_quality.json, manifest.json, report.md.

manifest에는 추출 기준 시각/시간대, 스키마 버전, 코드·쿼리 해시, 필터 정책, 메달 매핑 버전, 분위수 정의, 입력 파일 체크섬, seed, 행 수를 저장한다.

## 9. 완료 기준과 후속 모델 검토

- 실제 Entity/Domain/Repository/JDBC/DDL과 운영 스키마의 차이가 설명되어 있다.
- 실제 사용 가능한 유저·채보·기록 수가 실측되어 있고 CPI/SPI 분모가 구분된다.
- 입력 스냅샷이 같으면 동일한 결과를 재현할 수 있다.
- Chart/Level/User 통계, 점수→백분위 관계, 데이터 품질 보고가 생성된다.
- 접근 불가·누락 정보가 있으면 미확인 상태를 명시하며 완료로 포장하지 않는다.
- 결과에 따라 CPI의 규제 로지스틱/IRT 계열, SPI의 경험분포/목표 점수 달성 모델/연속 점수 모델을 비교 실험 후보로 제안한다. 단일 모델이 적합하다고 미리 확정하지 않는다.
- 기존 서열표 기능은 후속 단계다. S3·일일 배치·Discord 수동 실행은 이번 분석 작업의 실행/보관 경로로 구현한다.

## 10. 구현 범위와 검증 경계

- Java application 모듈의 AnalysisStatistics를 런타임과 오프라인 CLI가 공통으로 사용한다. infra의 JDBC 추출기는 repeatable-read 읽기 전용 스냅샷을 사용한다.
- 매일 06:00 및 `/실력분석최신화`는 동일한 DB 작업 큐에 접수한다. Discord는 type 4로 즉시 응답하며 로딩을 유지하지 않는다.
- S3의 모든 파일 업로드가 성공한 뒤 latest를 변경한다. 원천/유저 통계를 포함하므로 별도의 비공개 버킷을 사용한다.
- 완료/실패 JSON은 admin webhook에 첨부 파일로 전달한다. 알림 실패는 DB에 보존해 재시도하고, jobId로 중복 전달을 식별한다.
- 초기 CPI 관계 탐색은 같은 레벨의 고정 기준/검증 채보 분할을 사용한다. 기준 채보별 교집합을 완전히 맞춘 실험이나 유저-채보 그래프 연결성 분석은 아직 구현하지 않았다. readiness 결과만으로 최종 모델 적합성을 확정하지 않는다.
- 실제 운영 계정/기록 수는 접속 경로 확인 후 산출한다. 테스트 fixture의 통계는 운영 결과가 아니다.
