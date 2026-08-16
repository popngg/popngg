# 운영과 배포

## 현재 개발 실행

```bash
./gradlew :popngg-api:bootRun --args='--spring.profiles.active=local'
```

Swagger:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

## CI/CD

우선순위는 낮지만, 서버 재구현 후 다음을 권장합니다.

- PR마다 `./gradlew test`
- API 모듈 bootJar 생성 확인
- main 브랜치 merge 후 배포
- 배포 전 smoke test

## 모니터링

CloudWatch는 우선순위가 낮습니다. 도입 시 최소 지표:

- API latency
- 4xx/5xx rate
- 로그인 실패율
- 갱신 작업 성공/실패
- DB connection pool
- 마이그레이션 이후 정합성 점검 결과

## 로그

- 인증 실패는 민감정보 없이 남깁니다.
- 갱신 코드는 외부 응답 원문 저장 여부를 별도 정책으로 정합니다.
- 데이터 마이그레이션은 row count, 실패 row, old/new id 매핑을 파일 또는 테이블로 남깁니다.

## 전환 당일 체크리스트

- [ ] 신규 스키마 생성 완료
- [ ] 데이터 싱크 리허설 완료
- [ ] 검증 SQL 통과
- [ ] 신규 API smoke test 준비
- [ ] 롤백 경로 확인
- [ ] 서버 중단 공지
- [ ] 최종 싱크
- [ ] 서버 재오픈
- [ ] 주요 화면 확인
