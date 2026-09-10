# Chart ranking API

The chart ranking page `/chart/{songHash}/{difficulty}/ranking` can call:

```http
GET /api/v1/charts/{songHash}/{difficulty}/rankings?limit=100
```

Difficulty accepts `easy` (`e`), `normal` (`n`), `hyper` (`h`), and `ex`, case-insensitively.
`limit` defaults to 100 and must be between 1 and 100. Missing songs or active charts
return 404; invalid difficulty or limit returns 400.

The response remains `{code, message, data: {chartId, currentVersion, allTime}}`.
Both ranking lists retain their existing score order and entry fields.
The numeric `/api/v1/charts/{chartId}/rankings` endpoint remains available for existing clients.
