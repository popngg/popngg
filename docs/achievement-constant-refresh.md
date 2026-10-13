# 관리자용 상수 최신화

Discord 관리자 역할이 있는 사용자는 `/상수최신화`로 Lv48–50 메달·랭크 상수를 다시 계산할 수 있다. 명령은 작업 ID를 즉시 반환한다. 계산이 끝날 때까지 Discord 상호작용을 로딩 상태로 두지 않는다. 최종 성공·실패 결과는 admin bot이 `achievement-result.json` 첨부로 알린다.

작업은 기존 `/실력분석최신화`와 별도 활성 슬롯에 접수되며 API 워커가 순서대로 실행한다. 한 번 추출한 DB 스냅샷으로 메달과 랭크를 각각 학습한다. 두 축의 결과가 모두 검증되고 S3에 업로드된 다음 한 DB 트랜잭션에서 활성 스냅샷을 바꾼다. 실패하면 이전 상수가 유지된다. CPI/SPI `latest.json`은 이 작업에서 수정하지 않는다.

완료 JSON의 `status`가 `SUCCEEDED`일 때 `stored`에 DB 스냅샷 ID, `artifacts`에 S3 JSON 및 보고서 경로, `summary`에 축별 계산·보류 건수가 담긴다. 이후 `/상수표`에서 레벨과 메달 또는 랭크 기준을 골라 관리자용 이미지를 확인한다. `FAILED`라면 `errorCode`를 확인한다. 결과는 여전히 `EXPERIMENTAL`이며 사용자 공개용 검증 모델은 아니다.

운영 API 이미지에는 격리된 Python 환경과 NumPy/SciPy가 포함된다. `ACHIEVEMENT_BOOTSTRAP` 기본값은 30, `ACHIEVEMENT_TIMEOUT_SECONDS` 기본값은 축당 7200이다. 분석 버킷의 퍼블릭 액세스 차단 및 admin webhook이 구성되어 있어야 한다. 이미지 빌드에는 Python 패키지 다운로드가 필요하다.
