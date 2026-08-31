-- Normalize the official title. The corrected v3 jacket object already exists
-- at the target hash, so the catalog can move atomically without a broken URL.
UPDATE songs
   SET song_name = '人妖絵巻其の二「鬼」〜 夜叉の祭は終夜 〜',
       song_hash = 'b5f01cae26ae8be4d89ab49cc89cc740625a4d23ffd6f66e552772ac82e273b6',
       jacket_url = 'https://static.popn.gg/b5f01cae26ae8be4d89ab49cc89cc740625a4d23ffd6f66e552772ac82e273b6.png',
       updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1191
   AND song_name = '人妖絵巻其の二「鬼」～夜叉の祭は終夜～';
