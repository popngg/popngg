# 데이터 갱신 API 계약

프론트엔드 핸드오프 문서를 백엔드에 적용한 계약입니다. API prefix는 `/api/v1`입니다.

## 인증

- 기존 `Authorization: Bearer <token>` 인증을 계속 지원합니다.
- 로그인과 가입 성공 시 같은 JWT를 `access_token` HttpOnly 쿠키로도 발급합니다.
- 쿠키 속성은 `Secure; HttpOnly; SameSite=None; Path=/`입니다.
- 쿠키로 인증한 요청에는 새 JWT 쿠키를 발급해 만료시간을 슬라이딩 갱신합니다.
- 브라우저 호출은 CORS credentials를 사용해야 합니다.

## 가입 확인과 가입

- `GET /api/v1/auth/registrations/{poptomoId}`: 가입 계정이면 200, 없으면 404입니다.
- 이 조회는 프로필 공개 여부와 플레이데이터 존재 여부를 보지 않습니다.
- `POST /api/v1/auth/register`: `{ "poptomoId": string, "password": string, "hidden": boolean }`
- 중복 ID는 `409 ALREADY_REGISTERED`입니다.
- `password`는 기존 북마클릿 호환을 위해 SHA-256 소문자 hex 64자 값을 받으며 DB에는 BCrypt로 저장합니다.

## 갱신

- `POST /api/v1/renewals`
- 인증 사용자의 ID와 `profile.gameId`가 다르면 `403 GAME_ID_MISMATCH`입니다.
- 지원 게임은 `popn29`, collector version은 `1`이며 환경변수로 변경할 수 있습니다.
- 채보 마스터는 서버의 `songs`와 `charts`를 기준으로 별도 관리합니다.
- 채보 레벨은 서버의 채보 마스터를 사용하므로 갱신 요청에서 받지 않습니다.
- 숫자형 `chartId`가 있으면 우선 사용하고, 없거나 비숫자이면 곡명·장르·난이도로 매칭합니다.
- 난이도 원본은 `l/light/easy`, `n/normal`, `h/hyper`, `ex`를 받습니다.
- 랭크 원본 `e,d,c,b,a1,a2,a3,s`를 서버 내부 코드로 변환하고, `none`은 랭크 없음 코드 13으로 저장합니다.
- 메달 원본 `a`~`k`는 내부 코드 1~11로 변환하고, `none`은 메달 없음 코드 0으로 저장합니다.
- `versionBestScore`의 숫자/null/필드 누락을 구분합니다. 숫자는 버전 베스트, null은 이번 버전 미플레이, 누락은 기존 호환 방식으로 처리합니다.
- 최대 선언 payload 크기는 4 MiB이며 `stats.payloadBytes`로 검증합니다.

## 오류 형식

오류 코드는 응답 최상위 `code`에 둡니다.

```json
{ "code": "INVALID_PAYLOAD", "message": "The request payload is invalid." }
```

현재 갱신 오류 코드는 `UNAUTHENTICATED`, `GAME_ID_MISMATCH`, `INVALID_PAYLOAD`,
`EMPTY_PAYLOAD`, `UNSUPPORTED_GAME`, `UNSUPPORTED_COLLECTOR_VERSION`,
`UNKNOWN_DIFFICULTY`, `UNKNOWN_MEDAL_CODE`, `UNKNOWN_RANK_CODE`, `PAYLOAD_TOO_LARGE`입니다.

재시도 제한과 구 경로 종료일, ID 선점 신고 절차는 제품 운영 정책이므로 코드 배포와 별도로 결정합니다.
