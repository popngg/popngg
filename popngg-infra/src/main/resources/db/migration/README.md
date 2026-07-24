# Flyway baseline

These versioned migrations create the empty MVP schema only. Bulk legacy data
transforms are separate migration jobs and must not be added to a versioned
Flyway migration.

Versioned files are immutable after they have been applied to any shared
environment. Schema changes must be introduced by adding a new `V*.sql` file;
never edit an applied file.

The schema intentionally has no foreign-key constraints. Application services
must enforce references, while the declared unique keys and indexes protect the
critical current-state invariants such as one `playdata` row per
`user_id + chart_id`.
