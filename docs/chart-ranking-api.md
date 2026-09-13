# Chart ranking API

`GET /api/v1/charts/{songHash}/{difficulty}/rankings?axis=score&page=1&size=20`

Public endpoint, no authentication. Difficulty: 1=LIGHT, 2=NORMAL, 3=HYPER, 4=EX.
Axis: score (default) or medal. Page: starts at 1 (default). Size: 1-100, default 20;
frontend uses 20, 50, 100.

All-time best results participate regardless of current game version. Hidden profiles
are excluded before counting and ranking. Score sorts descending; medal codes sort
ascending. Equal scores/medals share competition positions (1, 1, 3), including across
pages. Medal ties sort by score descending then poptomo ID ascending; score ties use ID.

Success: {code: "SUCCESS", message: "The request is successful.", data: page}.
Page fields: items, totalItems, totalPages, hasPrev, hasNext.
Each item contains exactly:

- position: server-calculated competition position
- id: poptomo ID (not internal PK)
- name: nickname
- avatarUrl: URL or null
- userPopnClass: current display class in hundredths (19000 displays as 190.00)
- popnClass: all-time play result class in hundredths (17000 displays as 170.00)
- score: all-time best score, maximum 100000
- rank: score rank code
- medal: medal code

Rank codes 1-13: S+, S, AAA, AA+, AA, A+, A, B+, B, C, D, E, none.
Medal codes 1-13: gold star, silver star, silver diamond, silver circle, bronze star,
bronze diamond, bronze circle, black star, black diamond, black circle, EASY, LONG OFF, none.
Missing rank/medal is 13, never null. No current, scope, scoreVersion or chart metadata.

Missing songs, missing difficulties and deleted charts return HTTP 404:
{"code":"NOT_FOUND","message":"Chart not found.","data":null}.
Invalid parameters return HTTP 400. Existing charts with no records return an empty page;
pages beyond the end also return empty items.

Legacy `/api/v1/charts/{chartId}/rankings?limit=100` retains its previous response.
