# Legacy migration draft

POPNGG-20의 대량 데이터 변환 초안입니다. Flyway는 대상 MVP 스키마를
생성하는 데만 사용하고, 이 디렉터리의 job은 이미 Flyway migration이 끝난
빈 대상 DB에 데이터를 적재합니다.

원본 dump와 복원 DB는 읽기 전용 입력입니다. dump를 저장소 안으로 복사하거나
Git/Jira/PR에 첨부하지 않습니다.

## 전체 마이그레이션 과정

스키마 생성과 legacy dump 데이터 변환은 서로 다른 단계입니다. Adminer에서
Flyway가 만든 빈 테이블이 보이는 것만으로는 dump 데이터 마이그레이션이 완료된
것이 아닙니다.

```text
외부 legacy SQL dump
        |
        v
격리된 legacy DB에 restore (원본 보존)
        |
        +-----------------------------+
                                      |
빈 target DB -- Flyway V1~V4 --------+--> MVP 스키마 생성
                                      |
                                      v
                              bulk transform 실행
                                      |
                                      v
                          mapping/failure 기록 생성
                                      |
                                      v
                         건수 및 무결성 검증 실행
                                      |
                          실패 1건 이상이면 중단
                                      |
                                      v
                              검증된 target DB
```

1. 외부 dump를 격리된 legacy DB에 복원합니다. 원본 dump와 복원 DB는 변환
   과정에서 수정하지 않습니다.
2. 비어 있는 target DB에 Flyway `V1`~`V4`를 적용해 계정, 곡/채보,
   플레이데이터/이력, 로그/버전 전환 스키마를 만듭니다.
3. bulk transform이 legacy 데이터를 신규 구조로 적재합니다.
   - legacy `user`는 `users`와 `user_profiles`로 분리합니다.
   - legacy `chart`는 같은 song hash를 묶어 `songs`와 `charts`로 분리합니다.
   - `playdata`는 28버전 current/all-time 상태로 변환합니다.
   - `history`는 `event_type=MIGRATION`인 `playdata_history`로 옮깁니다.
4. old/new ID 관계는 `migration_*_map`에, 적재할 수 없는 행은 원본 값 없이
   `migration_failures`에 숫자 ID와 사유 코드만 기록합니다.
5. 별도 verification job이 원본/대상 건수, orphan, unique, mapping, popclass,
   credit 초기화를 확인합니다. `failure_count`가 하나라도 0이 아니면 cutover를
   중단합니다.

예를 들어 동일한 `(user_id, chart_id)`에 점수 90000과 95000이 있으면 95000
한 건만 `playdata`에 적재하고 다른 행은 `DUPLICATE_USER_CHART`로 기록합니다.
레거시 popclass는 `user_profiles.legacy_popclass`에 보존하며, 신규 credit 4종은
0으로 초기화합니다.

Adminer로 target DB를 확인하는 방법은 [`deploy/README.md`](../deploy/README.md)의
`Inspect the database locally` 절을 따릅니다. 다음 상태를 구분해서 확인해야 합니다.

- `flyway_schema_history`에 V1~V4만 있고 업무 테이블 row가 0이면 스키마 생성만 완료
- `migration_sessions`와 `migration_*_map`이 있고 업무 테이블에 row가 있으면 변환 실행
- `migration_verification_results.failure_count`가 모두 0이면 검증까지 통과

## 외부 dump end-to-end rehearsal

POPNGG-24의 긴급 로컬 리허설은 외부 dump restore, 격리 대상 스키마 생성, 데이터
transform과 검증을 한 명령으로 실행할 수 있습니다.

```bash
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USER=root \
./migration/bin/run-migration.sh \
  --dump /absolute/path/to/legacy-dump.sql \
  --legacy-db popngg_legacy_rehearsal \
  --target-db popngg_mvp_rehearsal \
  --reset
```

runner는 dump가 Git worktree 밖의 절대 경로인지 확인합니다. `--reset`은 이름이
검증된 두 rehearsal DB만 삭제하고 다시 만들며, production cutover에서는 사용하지
않습니다. 출력은 session 상태, table row count, 실패 사유별 count와 검증 실패
건수로 제한합니다.

이 end-to-end runner의 `01_mvp_schema.sql`은 실제 dump와 transform을 격리
검증하기 위한 schema snapshot입니다. 운영 schema lifecycle은
`popngg-infra/src/main/resources/db/migration/V*.sql`의 Flyway baseline만
사용하고, 운영 대량 transform은 아래 `migrate-legacy.sh`처럼 별도 job으로
실행합니다. schema snapshot이 Flyway baseline과 달라지면 리허설을 중단하고
동시에 갱신해야 합니다.

end-to-end 산출물은 target DB에만 남습니다.

- `migration_user_map`
- `migration_song_map`
- `migration_chart_map`
- `migration_playdata_map`
- `migration_failures`
- `migration_verification_results`

## 실행

MySQL 8의 legacy/target DB를 준비한 뒤 실행합니다.

```bash
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USER=root \
./migration/bin/migrate-legacy.sh \
  --legacy-db popngg_legacy \
  --target-db popngg_mvp \
  --session-id rehearsal-20260724
```

비밀번호는 MySQL option file 또는 로컬 `MYSQL_PWD`로만 전달합니다. runner는
비밀번호, 원본 row, 개인정보를 출력하지 않습니다.

## 산출물

대상 DB에 다음 테이블이 생성됩니다.

- `migration_sessions`: 시작/종료 시각과 상태
- `migration_user_map`
- `migration_song_map`: old chart/songHash와 new song ID/hash
- `migration_chart_map`
- `migration_playdata_map`
- `migration_failures`: source table, 숫자 ID, 사유 코드만 저장

재실행은 새로운 `--session-id`와 비어 있는 target DB를 사용합니다. 같은 session
ID는 거부되므로 부분 성공 데이터를 덮어쓰지 않습니다.

변환 정책:

- `users`와 `user_profiles` 분리
- 레거시 chart를 `songs`/`charts`로 분리
- playdata는 28버전 current state로 변환
- score를 `version_score`와 `all_time_score`에 보존
- rank/medal은 원천 값을 그대로 보존
- `legacy_popclass` 보존, 신규 credit 4종은 0
- 중복 `(user_id, chart_id)`은 score가 높은 한 건만 적재하고 나머지는 실패 보고

## 검증과 cutover 중단 기준

변환 후 별도 검증 job을 실행하면 민감한 원본 값 없이 check 이름과 건수만 담긴
TSV dry-run 리포트를 만들 수 있습니다.

```bash
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USER=root \
./migration/bin/verify-migration.sh \
  --legacy-db popngg_legacy \
  --target-db popngg_mvp \
  --session-id rehearsal-20260724 \
  --report build/reports/migration-verification.tsv
```

다음 검증 중 하나라도 실패하면 runner가 종료 코드 2를 반환하며 cutover를
중단해야 합니다.

- users, user_profiles, songs, charts, playdata row count
- users/user_profiles, charts/songs, playdata/users/charts orphan
- `(user_id, chart_id)` unique와 old/new mapping 완전성
- legacy popclass 보존 및 all-time score 기반 potential popclass 재계산 결과
- display/potential popclass 범위
- 신규 High☆Cheers credit 4종의 0 초기화
- songHash 중복과 하나의 old hash가 여러 new song으로 갈라지는 alias
- jacket 원본 참조가 신규 song 참조로 보존되었는지 여부

검증 SQL은 계정 식별자, 비밀번호, 이메일, 토큰, 원본 row를 출력하지 않습니다.
Docker MySQL 8에서 실패 감지와 정상 통과를 함께 확인하려면 다음을 실행합니다.

```bash
./migration/test/verify-migration.sh
```
