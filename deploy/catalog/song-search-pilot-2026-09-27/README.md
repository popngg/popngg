# Lv50 곡명 검색 태그 시범안

상태: **운영 미적용, 별칭 검토용**. 기존 `song_search_tags` 테이블과
`SongCatalogJdbcAdapter`의 활성 태그 검색 경로를 사용한다. 신규 API나
Flyway 마이그레이션은 필요하지 않다. 이 파일의 한국어 표기와 로마자 표기는
공식 번역이라고 주장하지 않는 검색 별칭 후보이며, 운영 등록 전에 검토한다.

| Lv50 곡명 | 한국어 검색 별칭 | 로마자/영어 검색 별칭 |
| --- | --- | --- |
| シュレーディンガーの猫 | 슈뢰딩거의 고양이 | Schrodingers Cat |
| 音楽 | 음악 | Ongaku |
| ピアノ協奏曲第１番”蠍火” | 피아노 협주곡 제1번 사소리비 | Sasoribi |
| L-an!ma | 라니마 | 원래 곡명으로 검색 가능 |
| Chaos:Q | 카오스 큐 | 원래 곡명으로 검색 가능 |
| BabeL ～MODEL DD101～ | 바벨 모델 DD101 | 원래 곡명으로 검색 가능 |
| Blue River | 블루 리버 | 원래 곡명으로 검색 가능 |
| ΔΟΓΜΑ | 도그마 | Dogma |
| Mecha Kawa Breaker!! | 메카 카와 브레이커 | 원래 곡명으로 검색 가능 |
| Megalara Garuda | 메갈라라 가루다 | 원래 곡명으로 검색 가능 |

총 10곡·14개 태그다. `apply.sql`은 song ID·hash·원래 곡명과
**삭제되지 않은 Lv50 채보**를 모두 확인한다. 하나라도 다르면 아무 태그도
넣지 않는다. 동일 시범 태그를 다시 적용하거나 `rollback.sql`로 비활성화한
뒤 재활성화할 수 있다. 다른 출처의 동일 태그와 충돌하면 중단한다.

## 시범 실행 순서

1. 표기의 정확성과 검색어의 유용성을 검토한다.
2. 운영 DB를 백업하고, 별도 복제 DB에서 `apply.sql`을 실행한다.
3. 활성 태그 14개와 10곡의 검색 결과를 확인한다. 두 조회 API는 같은
   `SongCatalogJdbcAdapter`를 사용한다.
4. 운영 시범 시작이 승인되면 `__TARGET_DB__`를 실제 DB명으로 바꾼
   `apply.sql`을 UTF-8 연결의 MySQL에서 수동 실행한다. 서버 재배포는
   필요하지 않다.
5. 아래 API로 한국어·영어 검색 결과를 확인하고 예상 밖 매칭을 기록한다.
6. 중단할 때 `rollback.sql`로 이 시범 태그만 비활성화한다.

```text
GET /api/v1/songs?keyword=슈뢰딩거&level=50
GET /api/v1/songs?keyword=schrodingers&level=50
GET /api/v1/charts?q=도그마&levelMin=50&levelMax=50
GET /api/v1/charts?q=dogma&levelMin=50&levelMax=50
```

API URL에서는 검색어를 URL 인코딩한다. `/api/v1/songs`의 `page`는
0부터, `/api/v1/charts`의 `page`는 1부터 시작한다. 현재 검색은
대소문자만 무시하며, 띄어쓰기·표기 변형은 자동 통합하지 않는다.
이 시범에서는 정확한 별칭부터 검증하고, 실제 검색어 데이터를 본 뒤
변형 태그와 정규화 규칙을 결정한다.

2026-09-27에 운영 DB의 검색 태그는 0건이었다. 복제 DB에서 14건 적용,
같은 SQL 재실행, 한국어/영어 별칭 조회, 14건 비활성화와 재활성화를
시험했다. 운영 DB에는 이 시범 태그를 아직 넣지 않았다.
