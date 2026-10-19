# 목표 달성 레벨 상수: 오프라인 실험

상태: Proposed / EXPERIMENTAL. 기존 운영 모델, DB, S3 latest를 변경하지 않는다.

메달은 CLEAR를, 랭크는 잠정적으로 AAA를 레벨 환산 기준으로 삼는다.
두 축은 서로 비교하지 않는다. 같은 축에서는 채보별 목표 난이도를 공유 사용자
능력으로 추정한다. 목표 간 간격은 채보마다 다르며, 같은 채보의 누적 목표 순서만
softplus 누적합으로 보장한다. 곡 간 순위는 목표마다 뒤집힐 수 있다.

## 실행

Python 3.11 이상, requirements.txt 설치 후:

```sh
python analysis/achievement/experiment.py --snapshot /private/snapshot --output build/achievement/medal --axis medal --bootstrap 30
python analysis/achievement/experiment.py --snapshot /private/snapshot --output build/achievement/rank --axis rank --bootstrap 30
python -m unittest discover -s analysis/achievement -p 'test_*.py'
```

DB 스냅샷을 직접 사용할 수 없는 검토 환경에서는 공개 API에서 익명화된 연구 입력을
수집할 수 있다. 팝토모 ID와 닉네임은 파일에 기록하지 않는다. API 수집은 트랜잭션
스냅샷이 아니므로 최종 검증보다 탐색 실험에만 사용한다.

```sh
python analysis/achievement/collect_api.py --output build/achievement/api-input
```

원천 catalog.json과 records.jsonl을 읽는다. user/profile/chart/song 존재 여부,
bot/hidden/deleted/duplicate를 재검사한다. 누락된 플래그는 안전하게 제외한다.
메달 1~10만 분석하며 EASY/LONGOFF/NONE은 실패로 바꾸지 않는다.
랭크는 allTimeRankCode 1~12만 사용한다. 점수에서 랭크를 생성하지 않는다.
기존 원천에는 랭크가 없으므로 랭크 실험은 NO_VALID_RECORDS로 중단될 수 있다.

## 모델과 평가

- P(목표 이상 보유)=sigmoid(userSkill-chartTargetDifficulty).
- 누적 이진 composite likelihood: 목표들은 독립된 시도로 취급하지 않는다.
- user와 목표별 chart 편차에 L2 규제. 채보마다 목표 간격을 학습한다.
- 유저·채보 단위 70/15/15 train/validation/test 분할. 동일 기록의 모든 목표는
  같은 분할에 속한다. 학습에 없는 user/chart는 평가에서 제외하고 건수를 기록한다.
- 규제 후보 .5/2/8은 validation log loss로 선택. test는 선택 이후 평가한다.
- baseline A는 학습 기록의 공식 레벨별 목표 보유율, B는 user+level 목표 모델이다.
- B와 후보 모델은 각각 validation에서 규제를 선택한다.
- 운영 CPI/SPI baseline, user holdout, calibration curves는 후속 검증이며 미구현이다.
- 50명, 성공/미달성 각 5명은 잠정 연구 적격 기준이다. 공개 기준으로 확정하지 않는다.
- 그래프가 분리되거나 최적화가 수렴하지 않으면 상수를 보류한다.

## 레벨 환산과 불확실성

Lv48/49/50마다 적격 채보 3개 이상일 때 기준 목표 계수의 중앙값을 48/49/50에
대응시킨다. 기준점이 증가하지 않으면 보류한다. 48~50 사이는 선형 보간하고,
그 밖의 목표는 가장 가까운 두 레벨 기준점의 기울기로 외삽한다. 따라서 Lv48 FC가
Lv50 CLEAR보다 어렵게 추정되면 50을 넘는 상수가 나올 수 있다. 이는 관리자 검토용
실험 환산이며 공식 레벨을 바꾸는 값이 아니다.

사용자 단위 bootstrap마다 모델과 기준점을 다시 적합한다. 같은 사용자의 모든
기록에 동일 가중치를 부여한다. 30회 이상, 전체 반복의 90% 이상에서 환산 가능한
경우에만 percentile 95% 구간을 출력한다. 30회는 smoke 수준이며 공개 검증은
반복 수를 늘려 안정성을 확인해야 한다. 표본 반복 횟수를 고유 사용자 수로 세지 않는다.

## 산출물과 후속 저장

- achievement-ratings.json/csv: 목표별 계수, 환산 상수, 구간, 표본, 보류 사유
- achievement-import.json: 관리자 API로 DB에 원자적으로 등록하는 실험 스냅샷 묶음
- report.json: 소스 SHA256, seed, 선택 규제, test 성능, 수렴, 기준점, 제약

운영 DB에는 기존 채보 테이블을 수정하지 않고 `achievement_constant_snapshots`와
`achievement_constants`에 저장한다. 관리자 권한으로 아래 API에
`achievement-import.json`을 전송하면 같은 축의 최신 스냅샷 포인터가 한 트랜잭션에서
교체된다. 등록 원본 JSON은 DB 변경 전에 비공개 분석 S3 버킷에 AES-256으로
보관한다. 보관 실패 시 DB 활성 스냅샷은 바뀌지 않는다. 일반 사용자 API에는
노출하지 않는다.

```text
POST /api/v1/admin/analysis/achievement-constants/snapshots
```

등록 뒤 Discord 관리자 명령 `/상수표`에서 레벨 48~50과 메달/랭크 축을 선택하면
곡별 다섯 목표 상수를 CPI 서열표 계열 디자인의 PNG로 확인할 수 있다.

원천/사용자 데이터는 커밋하지 않는다. 합성 결과는 운영 근거로 사용하지 않는다.
generate_fixture.py로 가상 사용자 300명, 채보 18개, 기록 5,400개를 생성할 수 있다.
합성 데이터 결과는 파이프라인 확인용일 뿐 운영 근거로 사용하지 않는다. 외삽된
상수는 특히 기준점 추정 오차에 민감하므로 bootstrap 구간과 함께 검토한다.

합성 데이터 test log loss (작을수록 좋음):

| 목표 | Level only | User+Level | User+Chart targets |
|---|---:|---:|---:|
| CLEAR | 0.6347 | 0.5190 | 0.5206 |
| 동다 | 0.6088 | 0.4987 | 0.4883 |
| 동별 | 0.5884 | 0.4844 | 0.4792 |
| FC | 0.5228 | 0.4347 | 0.4288 |
| PERFECT | 0.4275 | 0.3334 | 0.3325 |

이 데이터는 후보 모델과 유사한 구조에서 생성했으므로 실제 성능의 증거가 아니다.
공통 능력이 성립하지 않는 데이터와 실제 스냅샷으로 추가 검증해야 한다.

공개 검증 이후 별도 snapshot/result DB 테이블과 S3 immutable JSON에 동일 결과를
저장하고, 양쪽 저장 검증 후 공개 포인터를 전환한다. 현재 단계에서 이 배포 경로를
연결하지 않아 실험 숫자가 운영 레이팅을 덮어쓰지 않는다.
