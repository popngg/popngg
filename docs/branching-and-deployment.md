# 브랜치와 배포 정책

## 브랜치 역할

- `feature/**`: 기능 개발. `develop`으로 PR을 보냅니다.
- `hotfix/**`: 수정 작업. `develop`으로 PR을 보냅니다.
- `develop`: 다음 운영 후보 버전입니다. CI 성공 후 서버에 자동 배포하여 검증합니다.
- `main`: 검증이 완료된 안정 버전이자 롤백 기준입니다.

## 일반 출시 흐름

1. `feature/**` 또는 `hotfix/**`에서 작업합니다.
2. `develop` 대상 PR의 CI와 리뷰를 통과시켜 병합합니다.
3. 출시할 변경을 모은 뒤 `develop`에서 `main`으로 PR을 만듭니다.
4. `develop` CI가 성공하면 해당 커밋을 서버에 자동 배포합니다.
5. 배포 후 컨테이너 헬스체크와 API smoke test를 통과하는지 확인합니다.
6. 정상이라면 `develop`에서 `main`으로 PR을 만들어 병합하고 안정 버전으로 확정합니다.

## 긴급 수정

`develop` 후보 배포가 실패하면 워크플로가 자동으로 `origin/main`을 다시 빌드하고 배포합니다. 배포는 성공했지만 실제 기능에 문제가 발견된 경우에는 GitHub Actions의 `Deploy candidate` 워크플로에서 `Run workflow`를 눌러 `rollback-main`을 실행합니다.

긴급 수정도 `hotfix/**`에서 `develop`으로 PR을 보내고 동일한 후보 배포 검증을 거친 뒤 `develop`에서 `main`으로 확정합니다.

Flyway 마이그레이션은 자동으로 되돌아가지 않으므로, 모든 스키마 변경은 `main`의 이전 애플리케이션도 동작할 수 있는 하위 호환 방식으로 작성해야 합니다.

## 필수 검사

- PR 브랜치 흐름 검사
- Gradle 빌드 및 테스트
- MySQL 통합 테스트
- 변경 코드 커버리지 80% 이상
- 배포 manifest 검증
- CodeQL 검사

`develop` CI 성공은 서버에 후보 버전이 배포됨을 의미합니다. `main` 병합은 이미 검증한 후보를 안정 버전으로 확정하며, 서버에 별도 재배포를 발생시키지 않습니다.
