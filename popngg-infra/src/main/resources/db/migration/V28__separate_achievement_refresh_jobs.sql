ALTER TABLE analysis_jobs
    ADD COLUMN job_type VARCHAR(32) NOT NULL DEFAULT 'CPI_SPI' AFTER trigger_type;

-- Each job type may retain one active request. The shared worker lock still
-- serializes execution; slot 1 is CPI/SPI and slot 2 is achievement constants.
