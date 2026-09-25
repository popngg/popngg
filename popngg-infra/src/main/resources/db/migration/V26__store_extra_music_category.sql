ALTER TABLE songs
    ADD COLUMN extra_type VARCHAR(16) NOT NULL DEFAULT 'NONE' AFTER jacket_url;

UPDATE songs
SET extra_type = 'EXTRA'
WHERE song_name IN (
    'Asian Trinity',
    'JADAMGA',
    'BOMBER!BOMBER!BABY!',
    'OVERDUE DEADEND',
    '朔望',
    'その闇を薙いで',
    '真夜中のun thé noir'
);

UPDATE songs
SET extra_type = 'SUPER_EXTRA'
WHERE song_name IN (
    '精霊都市リトラ・ミュネ',
    'Red Mountain',
    '魍魎乱舞',
    'Oort'
);
