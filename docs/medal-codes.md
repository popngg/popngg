# 메달 코드

갱신 API와 저장소에서 사용하는 메달 코드는 다음과 같습니다.

| 코드 | 이름 | 조건 |
| ---: | --- | --- |
| 1 | `GOLD_STAR` | GOOD 0, BAD 0으로 클리어 |
| 2 | `SILVER_STAR` | BAD 0, GOOD 1~5로 클리어 |
| 3 | `SILVER_DIAMOND` | BAD 0, GOOD 6~20으로 클리어 |
| 4 | `SILVER_CIRCLE` | BAD 0, GOOD 21 이상으로 클리어 |
| 5 | `BRONZE_STAR` | BAD 1~5로 클리어 |
| 6 | `BRONZE_DIAMOND` | BAD 6~20으로 클리어 |
| 7 | `BRONZE_CIRCLE` | BAD 21 이상으로 클리어 |
| 8 | `BLACK_STAR` | 그루브 게이지 15~16칸으로 종료 |
| 9 | `BLACK_DIAMOND` | 그루브 게이지 12~14칸으로 종료 |
| 10 | `BLACK_CIRCLE` | 그루브 게이지 11칸 이하로 종료 |
| 11 | `EASY_CLEAR` | EASY 게이지로 클리어 |
| 12 | `LONGOFF_CLEAR` | LONG POP을 OFF하고 롱 팝 채보를 클리어 |
| 13 | `NO_MEDAL` | 원천 데이터의 메달 없음 (`none`) |

코드 1~12는 High Cheers 원천 코드 `a`~`l` 순서와 일치하며, 메달 없음은 코드 13입니다.
