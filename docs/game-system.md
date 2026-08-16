# 게임 시스템 변경

## 기준 버전

리팩토링의 기준 게임 버전은 `pop'n music High☆Cheers!!`입니다.

- 일본 가동 시작: 2025-12-18
- 본편 29번째 작품
- 장르명 표기 부활
- `EASY` 난이도명이 `LIGHT`로 변경된 것으로 관찰됨
- 점수 랭크에 `+` 등급 추가

참고:

- KONAMI 제품 페이지: <https://www.konami.com/arcadegames/products/am_popn_29/>
- KONAMI 가동 공지: <https://www.konami.com/arcadegames/corporate/ja/topics/2025/1218/>
- Pop'n Music Wiki High☆Cheers 정리: <https://popnmusic.fandom.com/wiki/Pop%27n_Music_High%E2%98%86Cheers%21%21>

## 랭크

High☆Cheers에서 점수 랭크 체계가 변경되었습니다. 기존 랭크에 `S+`, `AA+`, `A+`, `B+`가 추가됩니다.

중요 정책:

- 서버 내부에서 점수만 보고 랭크를 판단하지 않습니다.
- clear 여부에 따라 같은 점수라도 랭크가 달라질 수 있습니다.
- 갱신/크롤링 시점에 게임 화면 또는 원천 데이터의 랭크를 함께 수집하고, 그 값을 저장합니다.
- 아래 점수 구간은 참고/검증용입니다. 핵심 판정 로직으로 사용하지 않습니다.

| 랭크 | 점수 구간 | 조건 |
| --- | --- | --- |
| `S+` | 99,000 이상 | 클리어 |
| `S` | 98,000 - 98,999 | 클리어 |
| `AAA` | 95,000 - 97,999 | 클리어 |
| `AA+` | 93,000 - 94,999 | 클리어 |
| `AA` | 90,000 - 92,999 | 클리어 |
| `A+` | 86,000 - 89,999 | 클리어 |
| `A` | 82,000 이상 | clear 여부 영향 있음 |
| `B+` | 77,000 - 81,999 | clear 여부 영향 있음 |
| `B` | 72,000 - 76,999 | clear 여부 영향 있음 |
| `C` | 62,000 - 71,999 | clear 여부 영향 있음 |
| `D` | 50,000 - 61,999 | clear 여부 영향 있음 |
| `E` | 49,999 이하 | clear 여부 영향 있음 |

### 구현 메모

- 현재 `Rank`는 정수 값 객체입니다.
- High☆Cheers 이후에는 랭크 코드와 표시명을 분리해야 합니다.
- `rank_code`는 크롤링된 랭크를 저장하는 필드입니다.
- `score`는 점수 자체를 저장하는 필드이고, `rank_code`를 재계산하기 위한 source of truth가 아닙니다.
- 점수 구간은 데이터 검증, 관리자 리포트, 비정상 크롤링 감지에만 사용합니다.
- API 응답은 `code`, `label`, `sortOrder`, `scoreRange`, `requiresClearForReference`를 함께 내려줄 수 있습니다.

## 난이도

High☆Cheers 기준으로 난이도 표기는 `L / N / H / EX`를 우선 지원합니다.

| 표시 | 의미 | 비고 |
| --- | --- | --- |
| `L` | LIGHT | 기존 EASY 명칭 변경으로 추정 |
| `N` | NORMAL | 유지 |
| `H` | HYPER | 유지 |
| `EX` | EX | 유지 |

### 구현 메모

- 내부 difficulty code와 외부 표시명을 분리합니다.
- 기존 데이터의 `EASY`는 표시 단계에서 `LIGHT`로 변환할 수 있게 합니다.
- 버전별 표시명이 필요하면 `difficulty_label_policy` 또는 상수 API에서 관리합니다.

## 메달

신규 메달 `어시이지`를 추가합니다.

### 구현 메모

- 현재 `Medal`은 정수 값 객체입니다.
- 메달은 클리어 상태, 이지/노멀/하드 계열, 실패/미플레이 여부를 구분할 수 있어야 합니다.
- API는 프론트가 badge 표시를 바로 할 수 있도록 `code`, `label`, `shortLabel`, `sortOrder`를 내려주는 방식을 권장합니다.

## 갱신 코드

갱신 코드는 랭크를 반드시 함께 수집해야 합니다.

| 수집 항목 | 저장 위치 | 비고 |
| --- | --- | --- |
| 점수 | `playdata.score` | 정수 점수 |
| 랭크 | `playdata.rank_code` | 원천 데이터에서 수집한 값 |
| 메달 | `playdata.medal_code` | 원천 데이터에서 수집한 값 |
| 클리어 여부 | `playdata` 또는 medal 파생 | 랭크 검증 참고값 |

| 시나리오 | 서버 구현 |
| --- | --- |
| KONAMI가 JSON 제공 | JSON에서 score/rank/medal을 함께 파싱 |
| HTML/이미지/비정형 데이터 | score/rank/medal 영역을 함께 크롤링/파싱 |
| 수동 업로드 | 관리자 API에서 rank를 필수 입력값으로 받음 |

## 자켓 표시 정책

High☆Cheers에서 장르명 표기가 부활했습니다.

- 장르명을 곡 표시 정보에 포함합니다.
- 어퍼딱지는 표시에서 제거합니다.
- `isUpper` 데이터 자체는 songhash 판별과 채보 구분을 위해 보존 여부를 검토합니다.
- 자켓 파일명/URL 규칙은 song metadata와 chart metadata 중 어디에 둘지 확정해야 합니다.

## 짠판정 / 짠게이지

아직 위치가 확정되지 않았습니다.

| 선택지 | 장점 | 단점 |
| --- | --- | --- |
| Song metadata | 곡 단위 특성으로 보기 쉬움 | 난이도별로 다르면 표현 불가 |
| Chart metadata | 난이도별 차이 표현 가능 | 곡 목록 조회 시 집계 필요 |

권장 초안: 실제 게임에서 난이도별로 달라질 수 있다면 `chart`에 두고, 곡 목록 응답에서 요약값을 계산합니다.
