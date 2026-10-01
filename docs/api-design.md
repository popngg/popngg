# API 설계

## 원칙

- 프론트가 데이터 가공을 하지 않도록 서버에서 표시 가능한 형태로 내려줍니다.
- 내부 id와 외부 식별자를 분리합니다.
- rank, medal, difficulty 같은 코드는 label과 sort order를 함께 제공합니다.
- 기존 `/api/v2`와 신규 API의 호환/전환 정책을 문서화합니다.

## 후보 API

사용자 메모:

```text
/api/v1/constant/{songhash}/{difficulty}/{medal}
```

응답 초안:

```json
{
  "data": []
}
```

## 상수 API 초안

랭크, 메달, 난이도, 버전, 장르 표시 규칙을 내려주는 API를 권장합니다.

```http
GET /api/v1/constants
```

```json
{
  "ranks": [
    { "code": 100, "label": "S+", "sortOrder": 100 },
    { "code": 90, "label": "AA+", "sortOrder": 90 }
  ],
  "medals": [
    { "code": 10, "label": "어시이지", "sortOrder": 10 }
  ],
  "difficulties": [
    { "code": 1, "label": "Easy", "sortOrder": 1 }
  ]
}
```

## 곡 목록 API 초안

```http
GET /api/v1/songs
```

Query:

| 이름 | 설명 |
| --- | --- |
| `keyword` | 곡명, 장르명, 작곡가 검색 |
| `version` | 버전 필터 |
| `level` | 레벨 필터 |
| `difficulty` | 난이도 필터 |
| `tag` | 검색 태그 |

Response:

```json
{
  "songs": [
    {
      "songHash": "abc123",
      "genreName": "Genre",
      "songName": "Song",
      "artistName": "Artist",
      "version": 31,
      "jacketUrl": "https://example.com/jacket.png",
      "charts": [
        {
          "difficulty": 4,
          "level": 49,
          "rankSummary": {},
          "medalSummary": {}
        }
      ]
    }
  ]
}
```

## 플레이데이터 API 초안

```http
GET /api/v1/users/{poptomoId}/playdata
```

Response:

```json
{
  "user": {
    "poptomoId": "123456789",
    "name": "player",
    "popclass": 9000
  },
  "playdata": [
    {
      "songHash": "abc123",
      "difficulty": 4,
      "score": 98000,
      "rank": { "code": 100, "label": "S+" },
      "medal": { "code": 10, "label": "어시이지" },
      "updatedAt": "2026-05-09T00:00:00+09:00"
    }
  ]
}
```

## 검색 태그 기여

한국어 지원을 고려합니다.

- 태그 원문
- 정규화된 태그
- 언어 코드
- 기여자
- 승인 상태

검색은 초기에 MySQL 인덱스와 정규화 컬럼으로 시작하고, 검색 품질이 부족해지면 별도 검색 엔진을 검토합니다.

## 에러 응답

기존 `SuccessResponse` 스타일과 맞춰 공통 응답을 유지하되, 에러는 코드와 필드 단위 detail을 제공합니다.

```json
{
  "code": "INVALID_ARGUMENT",
  "message": "Invalid request.",
  "details": [
    { "field": "difficulty", "reason": "Unknown difficulty code." }
  ]
}
```

## 최근 추가된 곡

`GET /api/v1/songs/recent`는 생성일(`songs.created_at`) 내림차순으로 곡을 반환합니다.
생성일이 같으면 `song_id` 내림차순으로 정렬합니다. 삭제되지 않은 채보가 있는 곡만
조회하며, 각 곡에는 삭제되지 않은 채보 전체가 포함됩니다.

- `limit`: 생략하면 10, 허용 범위는 1~10. 예: `/api/v1/songs/recent?limit=5`.
- 범위를 벗어나면 HTTP 400 (`INVALID_SONG_QUERY`), 숫자가 아니어도 HTTP 400입니다.
- 응답은 기존 `GET /api/v1/songs`와 같은 `data.items`, `totalItems`, `totalPages`,
  `hasPrev`, `hasNext` 형식을 사용합니다. 메타데이터는 전체 조회 가능한 곡 수 기준입니다.
- 기존 곡 목록과 최근 곡 목록의 각 항목에 `createdAt`을 추가했습니다.
  값은 UTC ISO 8601 시각입니다(예: `2026-10-01T00:00:00Z`).
  게임 출시일이 아니라 DB에 저장된 생성일이며, 별도 생성일로 등록한 곡은 해당 값을 사용합니다.
