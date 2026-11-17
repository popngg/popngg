# Lv50 곡명 검색 태그 시범안

상태: **운영 미적용, 별칭 검토용**. 기존 `song_search_tags` 테이블과
`SongCatalogJdbcAdapter`의 활성 태그 검색 경로를 사용한다. 신규 API나
Flyway 마이그레이션은 필요하지 않다. 이 파일의 한국어 표기와 로마자 표기는
공식 번역이라고 주장하지 않는 검색 별칭 후보이며, 운영 등록 전에 검토한다.

| Lv50 곡명 | 한국어 검색 별칭 | 로마자/영어 검색 별칭 |
| --- | --- | --- |
| シュレーディンガーの猫 | 슈뢰딩거의고양이 | SchrodingersCat |
| 音楽 | 음악 | Ongaku |
| ピアノ協奏曲第１番”蠍火” | 피아노협주곡제1번사소리비, 사소리비, 피아노협주곡제1번헐화, 헐화 | Sasoribi |
| L-an!ma | 라니마 | 원래 곡명으로 검색 가능 |
| Chaos:Q | 카오스큐 | 원래 곡명으로 검색 가능 |
| BabeL ～MODEL DD101～ | 바벨모델DD101 | 원래 곡명으로 검색 가능 |
| Blue River | 블루리버 | 원래 곡명으로 검색 가능 |
| ΔΟΓΜΑ | 도그마 | Dogma |
| Mecha Kawa Breaker!! | 메카카와브레이커 | 원래 곡명으로 검색 가능 |
| Megalara Garuda | 메갈라라가루다 | 원래 곡명으로 검색 가능 |

총 10곡·17개 태그다. 태그에는 공백을 넣지 않는다. `蠍火`는 한국어 한자음
`헐화`와 일본어 독음 `사소리비`를 모두 등록한다. 다른 한자 곡명도 한국어
한자음이 실제 검색어로 유용한 경우 별칭을 추가한다. `apply.sql`은 song ID·hash·원래 곡명과
**삭제되지 않은 Lv50 채보**를 모두 확인한다. 하나라도 다르면 아무 태그도
넣지 않는다. 동일 시범 태그를 다시 적용하거나 `rollback.sql`로 비활성화한
뒤 재활성화할 수 있다. 다른 출처의 동일 태그와 충돌하면 중단한다.

## 시범 실행 순서

1. 표기의 정확성과 검색어의 유용성을 검토한다.
2. 운영 DB를 백업하고, 별도 복제 DB에서 `apply.sql`을 실행한다.
3. 활성 태그 17개와 10곡의 검색 결과를 확인한다. 두 조회 API는 같은
   `SongCatalogJdbcAdapter`를 사용한다.
4. 운영 시범 시작이 승인되면 `__TARGET_DB__`를 실제 DB명으로 바꾼
   `apply.sql`을 UTF-8 연결의 MySQL에서 수동 실행한다. 서버 재배포는
   필요하지 않다.
5. 아래 API로 한국어·영어 검색 결과를 확인하고 예상 밖 매칭을 기록한다.
6. 중단할 때 `rollback.sql`로 이 시범 태그만 비활성화한다.

```text
GET /api/v1/songs?keyword=슈뢰딩거&level=50
GET /api/v1/songs?keyword=schrodingers&level=50
GET /api/v1/songs?keyword=사소리비&level=50
GET /api/v1/songs?keyword=헐화&level=50
GET /api/v1/charts?q=도그마&levelMin=50&levelMax=50
GET /api/v1/charts?q=dogma&levelMin=50&levelMax=50
```

API URL에서는 검색어를 URL 인코딩한다. `/api/v1/songs`의 `page`는
0부터, `/api/v1/charts`의 `page`는 1부터 시작한다. 현재 검색은
태그 검색은 대소문자와 입력의 띄어쓰기를 무시한다. 원래 곡명·장르명 등의
검색은 기존 동작을 유지한다. `사소리비`와 `헐화`처럼 독음 자체가 다른
경우에는 각각 별칭을 등록한다.

2026-09-27에 운영 DB의 검색 태그는 0건이었다. 17건 버전은 복제 DB에서
적용·재실행·`헐화`/`사소리비`/`Sasoribi` 조회·비활성화·재활성화를
시험했다. 입력 공백 정규화는 `SongCatalogJdbcAdapterTest`로 확인했다.
병합 커밋 `0040449` 배포 후 운영 DB에서 임시 테이블의 collation 불일치로
첫 실행이 태그 등록 전에 중단됐다. `utf8mb4_unicode_ci`를 임시 테이블에
명시해 재실행했고 운영 DB에 시범 태그 17개가 등록됐다. 공개 API에서
`헐화`·`사소리비`·`헐 화` 조회를 확인했다.
