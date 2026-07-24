# Legacy migration draft

POPNGG-20의 대량 데이터 변환 초안입니다. Flyway는 대상 MVP 스키마를
생성하는 데만 사용하고, 이 디렉터리의 job은 이미 Flyway migration이 끝난
빈 대상 DB에 데이터를 적재합니다.

원본 dump와 복원 DB는 읽기 전용 입력입니다. dump를 저장소 안으로 복사하거나
Git/Jira/PR에 첨부하지 않습니다.

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
