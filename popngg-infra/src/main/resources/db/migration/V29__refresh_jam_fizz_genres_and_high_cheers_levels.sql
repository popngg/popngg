-- Official catalog correction verified on 2026-10-13.

-- Preserve IDs, hashes, jackets, scores and history. Guard reviewed identities and old values.

UPDATE songs s SET s.genre_name = 'エヴォリューションディスコ', s.artist_name = 'sei☆shin feat.ken', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1891 AND s.version = 28 AND BINARY s.song_name = BINARY 'Bomb be love' AND BINARY COALESCE(s.artist_name, '') = BINARY 'sei☆shin feat.ken'
   AND BINARY s.genre_name = BINARY 'Bomb be love';

UPDATE songs s SET s.genre_name = 'ビッグバンドミュージカル', s.artist_name = 'T-HEY & TANEKO', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1892 AND s.version = 28 AND BINARY s.song_name = BINARY 'BROADWAY QUEENS' AND BINARY COALESCE(s.artist_name, '') = BINARY 'T-HEY & TANEKO'
   AND BINARY s.genre_name = BINARY 'BROADWAY QUEENS';

UPDATE songs s SET s.genre_name = 'シャイニーゴスペル', s.artist_name = 'アマギセーラ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1893 AND s.version = 28 AND BINARY s.song_name = BINARY 'Heavenly Good Morning!' AND BINARY COALESCE(s.artist_name, '') = BINARY 'アマギセーラ'
   AND BINARY s.genre_name = BINARY 'Heavenly Good Morning!';

UPDATE songs s SET s.genre_name = 'ハピネスモータウン', s.artist_name = '青野りえ+mizuno', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1894 AND s.version = 28 AND BINARY s.song_name = BINARY 'How Many Eyes On You?' AND BINARY COALESCE(s.artist_name, '') = BINARY '青野りえ+mizuno'
   AND BINARY s.genre_name = BINARY 'How Many Eyes On You?';

UPDATE songs s SET s.genre_name = 'コスモテクノ', s.artist_name = 'Serph', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1895 AND s.version = 28 AND BINARY s.song_name = BINARY 'Hypernova' AND BINARY COALESCE(s.artist_name, '') = BINARY 'Serph'
   AND BINARY s.genre_name = BINARY 'Hypernova';

UPDATE songs s SET s.genre_name = 'フローティングフュージョン', s.artist_name = '北川翔也', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1897 AND s.version = 28 AND BINARY s.song_name = BINARY 'Levitation Minneapolis' AND BINARY COALESCE(s.artist_name, '') = BINARY '北川翔也'
   AND BINARY s.genre_name = BINARY 'Levitation Minneapolis';

UPDATE songs s SET s.genre_name = 'モダンカントリー', s.artist_name = '大谷舞 feat.つむぎしゃち（from キミのね）', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1898 AND s.version = 28 AND BINARY s.song_name = BINARY 'Que Sera Sera' AND BINARY COALESCE(s.artist_name, '') = BINARY '大谷舞 feat.つむぎしゃち（from キミのね）'
   AND BINARY s.genre_name = BINARY 'Que Sera Sera';

UPDATE songs s SET s.genre_name = 'エテリアル', s.artist_name = 'BEMANI Sound Team "HuΣeR" × Cororo feat. 大瀬良あい', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1899 AND s.version = 28 AND BINARY s.song_name = BINARY 'storia' AND BINARY COALESCE(s.artist_name, '') = BINARY 'BEMANI Sound Team "HuΣeR" × Cororo feat. 大瀬良あい'
   AND BINARY s.genre_name = BINARY 'storia';

UPDATE songs s SET s.genre_name = 'ガールズスピードコア', s.artist_name = 'T4K feat.ういにゃす', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1901 AND s.version = 28 AND BINARY s.song_name = BINARY 'The Die is cast.' AND BINARY COALESCE(s.artist_name, '') = BINARY 'T4K feat.ういにゃす'
   AND BINARY s.genre_name = BINARY 'The Die is cast.';

UPDATE songs s SET s.genre_name = 'ファンキーブレイクス', s.artist_name = 'Aikapin Prod. by uno & NiActivity', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1902 AND s.version = 28 AND BINARY s.song_name = BINARY 'Tripping Jumping' AND BINARY COALESCE(s.artist_name, '') = BINARY 'Aikapin Prod. by uno & NiActivity'
   AND BINARY s.genre_name = BINARY 'Tripping Jumping';

UPDATE songs s SET s.genre_name = 'バスターメタル', s.artist_name = 'My Complex of Academy', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1903 AND s.version = 28 AND BINARY s.song_name = BINARY '装填昇天エクソシスト' AND BINARY COALESCE(s.artist_name, '') = BINARY 'My Complex of Academy'
   AND BINARY s.genre_name = BINARY '装填昇天エクソシスト';

UPDATE songs s SET s.genre_name = 'J-ハウス', s.artist_name = '阿部靖広 feat.ゆめめ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1906 AND s.version = 28 AND BINARY s.song_name = BINARY '時が戻れば...' AND BINARY COALESCE(s.artist_name, '') = BINARY '阿部靖広 feat.ゆめめ'
   AND BINARY s.genre_name = BINARY '時が戻れば...';

UPDATE songs s SET s.genre_name = 'アンティークワルツ', s.artist_name = 'm@sumi feat. q''yul', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1907 AND s.version = 28 AND BINARY s.song_name = BINARY '囚われの魔女人形' AND BINARY COALESCE(s.artist_name, '') = BINARY 'm@sumi feat. q''yul'
   AND BINARY s.genre_name = BINARY '囚われの魔女人形';

UPDATE songs s SET s.genre_name = 'カーホップベース', s.artist_name = 'BEMANI Sound Team "PON" feat.NU-KO', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1908 AND s.version = 28 AND BINARY s.song_name = BINARY 'Yum Yum OYACHU' AND BINARY COALESCE(s.artist_name, '') = BINARY 'BEMANI Sound Team "PON" feat.NU-KO'
   AND BINARY s.genre_name = BINARY 'Yum Yum OYACHU';

UPDATE songs s SET s.genre_name = 'ホリデーラウンジ', s.artist_name = 'BEMANI Sound Team "劇団レコード" feat.秋成', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1912 AND s.version = 28 AND BINARY s.song_name = BINARY 'Saturday''s Reward' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'Saturday''s Reward';

UPDATE songs s SET s.genre_name = 'カレッジポップ', s.artist_name = '常盤ゆう', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1913 AND s.version = 28 AND BINARY s.song_name = BINARY 'ペパーミントは私の敵' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'カレッジポップ';

UPDATE songs s SET s.genre_name = 'エレクトロサーフロック', s.artist_name = '情次２号＆ジン', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1914 AND s.version = 28 AND BINARY s.song_name = BINARY 'Surfing Area51' AND BINARY COALESCE(s.artist_name, '') = BINARY '情次２号＆ジン'
   AND BINARY s.genre_name = BINARY 'Surfing Area51';

UPDATE songs s SET s.genre_name = 'アゲモノハードバップ', s.artist_name = 'red glasses feat.ゆきまめ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1916 AND s.version = 28 AND BINARY s.song_name = BINARY 'ハートのフリット' AND BINARY COALESCE(s.artist_name, '') = BINARY 'red glasses feat.ゆきまめ'
   AND BINARY s.genre_name = BINARY 'ハートのフリット';

UPDATE songs s SET s.genre_name = '彗星ブギ', s.artist_name = '豚乙女', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1918 AND s.version = 28 AND BINARY s.song_name = BINARY 'DOKIDOKI☆シューティングスター' AND BINARY COALESCE(s.artist_name, '') = BINARY '豚乙女'
   AND BINARY s.genre_name = BINARY 'DOKIDOKI☆シューティングスター';

UPDATE songs s SET s.genre_name = 'ベースボールヒロインREMIX', s.artist_name = 'RoughSketch Vs. BEMANI Sound Team "L.E.D.-G"', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1920 AND s.version = 28 AND BINARY s.song_name = BINARY '轟け！恋のビーンボール！！ (STARDOM Remix)' AND BINARY COALESCE(s.artist_name, '') = BINARY 'RoughSketch Vs. BEMANI Sound Team "L.E.D.-G"'
   AND BINARY s.genre_name = BINARY '轟け！恋のビーンボール！！ (STARDOM Remix)';

UPDATE songs s SET s.genre_name = 'エモーショナルJ-ポップ', s.artist_name = 'BEMANI Sound Team "HuΣeR × Yvya" feat.紫村 花澄', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1921 AND s.version = 28 AND BINARY s.song_name = BINARY 'ANEMONE' AND BINARY COALESCE(s.artist_name, '') = BINARY 'BEMANI Sound Team "HuΣeR × Yvya" feat.紫村 花澄'
   AND BINARY s.genre_name = BINARY 'ANEMONE';

UPDATE songs s SET s.genre_name = 'サイケデリックトランス', s.artist_name = 'AJURIKA feat.矢吹真央', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1922 AND s.version = 28 AND BINARY s.song_name = BINARY 'Moving on' AND BINARY COALESCE(s.artist_name, '') = BINARY 'AJURIKA feat.矢吹真央'
   AND BINARY s.genre_name = BINARY 'Moving on';

UPDATE songs s SET s.genre_name = 'ブレイクコア', s.artist_name = 'L.E.D. vs TOMOSUKE fw.crimm', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1923 AND s.version = 28 AND BINARY s.song_name = BINARY 'Cookie Bouquets' AND BINARY COALESCE(s.artist_name, '') = BINARY 'L.E.D. vs TOMOSUKE fw.crimm'
   AND BINARY s.genre_name = BINARY 'Cookie Bouquets';

UPDATE songs s SET s.genre_name = 'クリティカルケルト', s.artist_name = '夜影ネコチ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1924 AND s.version = 28 AND BINARY s.song_name = BINARY '竜を狩る者' AND BINARY COALESCE(s.artist_name, '') = BINARY '夜影ネコチ'
   AND BINARY s.genre_name = BINARY '竜を狩る者';

UPDATE songs s SET s.genre_name = 'バルカニックロック', s.artist_name = 'BEMANI Sound Team "Akhuta Works" feat. mami', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1925 AND s.version = 28 AND BINARY s.song_name = BINARY '乱膳の舞姫' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY '乱膳の舞姫';

UPDATE songs s SET s.genre_name = 'ポルカ2020', s.artist_name = 'かめりあ feat. ななひら', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1926 AND s.version = 28 AND BINARY s.song_name = BINARY 'POLꞰAMAИIA' AND BINARY COALESCE(s.artist_name, '') = BINARY 'かめりあ feat. ななひら'
   AND BINARY s.genre_name = BINARY 'POLꞰAMAИIA';

UPDATE songs s SET s.genre_name = 'トランス', s.artist_name = 'BEMANI Sound Team "dj TAKA" feat.Sana', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1927 AND s.version = 28 AND BINARY s.song_name = BINARY 'No Border' AND BINARY COALESCE(s.artist_name, '') = BINARY 'BEMANI Sound Team "dj TAKA" feat.Sana'
   AND BINARY s.genre_name = BINARY 'No Border';

UPDATE songs s SET s.genre_name = 'ピコピコ電波ソング', s.artist_name = 'ヒゲドライバー', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1928 AND s.version = 28 AND BINARY s.song_name = BINARY 'あるビー！ feat.ころねぽち' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ヒゲドライバー'
   AND BINARY s.genre_name = BINARY 'あるビー！ feat.ころねぽち';

UPDATE songs s SET s.genre_name = 'ボンバーラップ', s.artist_name = 'FAKE TYPE.', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1929 AND s.version = 28 AND BINARY s.song_name = BINARY 'CRAZY ARTS' AND BINARY COALESCE(s.artist_name, '') = BINARY 'FAKE TYPE.'
   AND BINARY s.genre_name = BINARY 'CRAZY ARTS';

UPDATE songs s SET s.genre_name = 'ボンバーテクノ', s.artist_name = 'TESSEI TOJO', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1930 AND s.version = 28 AND BINARY s.song_name = BINARY 'Amazing Bomberman' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'Amazing Bomberman';

UPDATE songs s SET s.genre_name = 'ポップコーンパーティー2', s.artist_name = 'Tomoyuki Uchida', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1931 AND s.version = 28 AND BINARY s.song_name = BINARY 'ミラクル☆喫茶' AND BINARY COALESCE(s.artist_name, '') = BINARY 'Tomoyuki Uchida'
   AND BINARY s.genre_name = BINARY 'ミラクル☆喫茶';

UPDATE songs s SET s.genre_name = 'シンセウェーブポップ', s.artist_name = 'ここなつ2.0', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1932 AND s.version = 28 AND BINARY s.song_name = BINARY 'ロマンシングエスケープ' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ここなつ2.0'
   AND BINARY s.genre_name = BINARY 'ロマンシングエスケープ';

UPDATE songs s SET s.genre_name = 'コスモボヤージュ', s.artist_name = 'ARForest feat.nayuta', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1934 AND s.version = 28 AND BINARY s.song_name = BINARY 'astral_dismantle' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ARForest feat.nayuta'
   AND BINARY s.genre_name = BINARY 'astral_dismantle';

UPDATE songs s SET s.genre_name = 'ヨフカシプロジェクト1', s.artist_name = 'ヨフカシプロジェクト', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1935 AND s.version = 28 AND BINARY s.song_name = BINARY 'まっぷたツートンソウル' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ヨフカシプロジェクト'
   AND BINARY s.genre_name = BINARY 'まっぷたツートンソウル';

UPDATE songs s SET s.genre_name = 'ヨフカシプロジェクト2', s.artist_name = 'ヨフカシプロジェクト', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1936 AND s.version = 28 AND BINARY s.song_name = BINARY 'そして惨劇は灰色に踊る' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'そして惨劇は灰色に踊る';

UPDATE songs s SET s.genre_name = 'ジャックポット', s.artist_name = '山本真央樹', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1937 AND s.version = 28 AND BINARY s.song_name = BINARY 'Hold the Pickles!!' AND BINARY COALESCE(s.artist_name, '') = BINARY '山本真央樹'
   AND BINARY s.genre_name = BINARY 'Hold the Pickles!!';

UPDATE songs s SET s.genre_name = '激辛コア', s.artist_name = 'Hommarju', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1938 AND s.version = 28 AND BINARY s.song_name = BINARY 'MAX KARAI' AND BINARY COALESCE(s.artist_name, '') = BINARY 'Hommarju'
   AND BINARY s.genre_name = BINARY 'MAX KARAI';

UPDATE songs s SET s.genre_name = 'カートゥーンクッキング', s.artist_name = 'OSTER project feat. Hinatanso', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1939 AND s.version = 28 AND BINARY s.song_name = BINARY 'すすめ！キッチンカー' AND BINARY COALESCE(s.artist_name, '') = BINARY 'OSTER project feat. Hinatanso'
   AND BINARY s.genre_name = BINARY 'すすめ！キッチンカー';

UPDATE songs s SET s.genre_name = 'フードファイト', s.artist_name = 'cosMo＠暴走P', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1941 AND s.version = 28 AND BINARY s.song_name = BINARY 'ホットドッグドカぐいレース!!!!' AND BINARY COALESCE(s.artist_name, '') = BINARY 'cosMo＠暴走P'
   AND BINARY s.genre_name = BINARY 'ホットドッグドカぐいレース!!!!';

UPDATE songs s SET s.genre_name = 'キャラバン', s.artist_name = '藤野マナミ & うさおりーぬ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1946 AND s.version = 28 AND BINARY s.song_name = BINARY 'エトランジェ' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'エトランジェ';

UPDATE songs s SET s.genre_name = 'ベジグレッシブ', s.artist_name = 'red glasses feat.池尻家', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1947 AND s.version = 28 AND BINARY s.song_name = BINARY 'Primitive Whimsical Chopped Salad' AND BINARY COALESCE(s.artist_name, '') = BINARY 'red glasses feat.池尻家'
   AND BINARY s.genre_name = BINARY 'Primitive Whimsical Chopped Salad';

UPDATE songs s SET s.genre_name = 'チッピンステップ', s.artist_name = 'かめりあ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1948 AND s.version = 28 AND BINARY s.song_name = BINARY 'Rushiest Rush' AND BINARY COALESCE(s.artist_name, '') = BINARY 'かめりあ'
   AND BINARY s.genre_name = BINARY 'Rushiest Rush';

UPDATE songs s SET s.genre_name = 'フォレストファンク', s.artist_name = '削除', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1949 AND s.version = 28 AND BINARY s.song_name = BINARY '嵐の唄' AND BINARY COALESCE(s.artist_name, '') = BINARY '削除'
   AND BINARY s.genre_name = BINARY '嵐の唄';

UPDATE songs s SET s.genre_name = 'デジタルサイバーロック', s.artist_name = 'SOUND HOLIC Vs. ZYTOKINE feat. Nana Takahashi', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1951 AND s.version = 28 AND BINARY s.song_name = BINARY 'ONYX' AND BINARY COALESCE(s.artist_name, '') = BINARY 'SOUND HOLIC Vs. ZYTOKINE feat. Nana Takahashi'
   AND BINARY s.genre_name = BINARY 'ONYX';

UPDATE songs s SET s.genre_name = 'トランスコア BAND', s.artist_name = 'B.E.L', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1958 AND s.version = 28 AND BINARY s.song_name = BINARY 'Flower-Band ver.-' AND BINARY COALESCE(s.artist_name, '') = BINARY 'B.E.L'
   AND BINARY s.genre_name = BINARY 'Flower-Band ver.-';

UPDATE songs s SET s.genre_name = '昭和レトロ', s.artist_name = '猫叉Master feat.霜月はるか', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1959 AND s.version = 28 AND BINARY s.song_name = BINARY '黒紅掬い' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY '黒紅掬い';

UPDATE songs s SET s.genre_name = 'リリカルベース', s.artist_name = 'onoken', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1960 AND s.version = 28 AND BINARY s.song_name = BINARY 'Viden' AND BINARY COALESCE(s.artist_name, '') = BINARY 'onoken'
   AND BINARY s.genre_name = BINARY 'Viden';

UPDATE songs s SET s.genre_name = 'ミュージカルファンタジー', s.artist_name = 'ARM×狐夢想 feat. anporin', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1961 AND s.version = 28 AND BINARY s.song_name = BINARY '円環のヴァルキュリア -lopulla tragedia-' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ARM×狐夢想 feat. anporin'
   AND BINARY s.genre_name = BINARY '円環のヴァルキュリア -lopulla tragedia-';

UPDATE songs s SET s.genre_name = 'ヨーロピアンブレイクコア2', s.artist_name = 'm@sumi', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1962 AND s.version = 28 AND BINARY s.song_name = BINARY '真夜中のun thé noir' AND BINARY COALESCE(s.artist_name, '') = BINARY 'm@sumi'
   AND BINARY s.genre_name = BINARY '真夜中のun thé noir';

UPDATE songs s SET s.genre_name = 'トランス BAND', s.artist_name = 'ピンクターボ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1963 AND s.version = 28 AND BINARY s.song_name = BINARY 'pandora-追憶の彼方へ-' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ピンクターボ'
   AND BINARY s.genre_name = BINARY 'pandora-追憶の彼方へ-';

UPDATE songs s SET s.genre_name = 'ケルティックトランスステップ', s.artist_name = 'Nhato', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1964 AND s.version = 28 AND BINARY s.song_name = BINARY 'Dawn Saga' AND BINARY COALESCE(s.artist_name, '') = BINARY 'Nhato'
   AND BINARY s.genre_name = BINARY 'Dawn Saga';

UPDATE songs s SET s.genre_name = 'ミッドナイトスウィング', s.artist_name = 'かめりあ', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1965 AND s.version = 28 AND BINARY s.song_name = BINARY 'Yuni''s Nocturnal Days' AND BINARY COALESCE(s.artist_name, '') = BINARY 'かめりあ'
   AND BINARY s.genre_name = BINARY 'Yuni''s Nocturnal Days';

UPDATE songs s SET s.genre_name = 'ブレスドワルツ', s.artist_name = 'みーに feat. しろさきあや', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1966 AND s.version = 28 AND BINARY s.song_name = BINARY 'アルファ・スカイ' AND BINARY COALESCE(s.artist_name, '') = BINARY 'みーに feat. しろさきあや'
   AND BINARY s.genre_name = BINARY 'アルファ・スカイ';

UPDATE songs s SET s.genre_name = 'エデン', s.artist_name = 'BEMANI Sound Team "Power Of Nature"', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1967 AND s.version = 28 AND BINARY s.song_name = BINARY 'AVEL' AND BINARY COALESCE(s.artist_name, '') = BINARY 'BEMANI Sound Team "Power Of Nature"'
   AND BINARY s.genre_name = BINARY 'AVEL';

UPDATE songs s SET s.genre_name = 'ラブリーブチアゲポップ BAND', s.artist_name = 'Mar.na', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1968 AND s.version = 28 AND BINARY s.song_name = BINARY 'MAX LOVE - 鐘の音が止む前に -' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'MAX LOVE - 鐘の音が止む前に -';

UPDATE songs s SET s.genre_name = 'トゥインクルポップ BAND', s.artist_name = 'good-cool', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1969 AND s.version = 28 AND BINARY s.song_name = BINARY 'Twinkle Wonderland -Sparkle Wonderland-' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'Twinkle Wonderland -Sparkle Wonderland-';

UPDATE songs s SET s.genre_name = 'ミラクルダンスポップ', s.artist_name = 'Another Infinity feat. Mayumi Morinaga', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1970 AND s.version = 28 AND BINARY s.song_name = BINARY 'It''s my Miracle' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'It''s my Miracle';

UPDATE songs s SET s.genre_name = 'ファンタジア', s.artist_name = 'arcane feat.rionos', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1971 AND s.version = 28 AND BINARY s.song_name = BINARY '昏き甲鉄のヴェルガ' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY '昏き甲鉄のヴェルガ';

UPDATE songs s SET s.genre_name = 'ダンスポップ', s.artist_name = 'ReGLOSS', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1972 AND s.version = 28 AND BINARY s.song_name = BINARY 'Shooting Star' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ReGLOSS'
   AND BINARY s.genre_name = BINARY 'Shooting Star';

UPDATE songs s SET s.genre_name = '電波', s.artist_name = 'covered by 一条莉々華(ReGLOSS)', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1974 AND s.version = 28 AND BINARY s.song_name = BINARY 'カジノファイヤーことみちゃん' AND BINARY COALESCE(s.artist_name, '') = BINARY 'covered by 一条莉々華(ReGLOSS)'
   AND BINARY s.genre_name = BINARY 'カジノファイヤーことみちゃん';

UPDATE songs s SET s.genre_name = 'アニマソング', s.artist_name = 'covered by 儒烏風亭らでん(ReGLOSS)', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1976 AND s.version = 28 AND BINARY s.song_name = BINARY '朱と碧のランページ' AND BINARY COALESCE(s.artist_name, '') = BINARY 'covered by 儒烏風亭らでん(ReGLOSS)'
   AND BINARY s.genre_name = BINARY '朱と碧のランページ';

UPDATE songs s SET s.genre_name = 'バトルプログレッシブ', s.artist_name = '桜庭　統', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1977 AND s.version = 28 AND BINARY s.song_name = BINARY 'Step forward' AND BINARY COALESCE(s.artist_name, '') = BINARY '桜庭　統'
   AND BINARY s.genre_name = BINARY 'Step forward';

UPDATE songs s SET s.genre_name = 'J-ダンスポップ BAND', s.artist_name = 'fleufleu', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1978 AND s.version = 28 AND BINARY s.song_name = BINARY 'Follow Tomorrow -fleufleu Edition-' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'Follow Tomorrow -fleufleu Edition-';

UPDATE songs s SET s.genre_name = 'アラビアンハードダンス', s.artist_name = 'Shouya Namai', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1979 AND s.version = 28 AND BINARY s.song_name = BINARY 'High & Low' AND BINARY COALESCE(s.artist_name, '') = BINARY 'Shouya Namai'
   AND BINARY s.genre_name = BINARY 'High & Low';

UPDATE songs s SET s.genre_name = 'ラウドロック', s.artist_name = 'A.T.park', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1980 AND s.version = 28 AND BINARY s.song_name = BINARY 'BLAKE' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'BLAKE';

UPDATE songs s SET s.genre_name = 'フォーエバーフレンズ', s.artist_name = '日向美ビタースイーツ♪', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1982 AND s.version = 28 AND BINARY s.song_name = BINARY '走れメロンパン' AND BINARY COALESCE(s.artist_name, '') = BINARY ''
   AND BINARY s.genre_name = BINARY 'フォーエバーフレンズ';

UPDATE songs s SET s.genre_name = 'ノア', s.artist_name = 'BEMANI Sound Team "Power Of Nature"', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 1985 AND s.version = 28 AND BINARY s.song_name = BINARY 'THX 4' AND BINARY COALESCE(s.artist_name, '') = BINARY 'BEMANI Sound Team "Power Of Nature"'
   AND BINARY s.genre_name = BINARY 'THX 4';

UPDATE songs s SET s.genre_name = 'ハッピーフューチャーベース', s.artist_name = 'ARM (IOSYS) feat. Nicole Curry', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 2119 AND s.version = 29 AND BINARY s.song_name = BINARY 'Neverland' AND BINARY COALESCE(s.artist_name, '') = BINARY 'ARM (IOSYS) feat. Nicole Curry'
   AND BINARY s.genre_name = BINARY 'Neverland';

UPDATE songs s SET s.genre_name = 'キューティーシャッフル', s.artist_name = 'U-ske feat.花柚', s.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 2122 AND s.version = 29 AND BINARY s.song_name = BINARY 'charm♡you' AND BINARY COALESCE(s.artist_name, '') = BINARY 'U-ske feat.花柚'
   AND BINARY s.genre_name = BINARY 'charm♡you';

UPDATE charts c JOIN songs s ON s.song_id = c.song_id
 SET c.level = 46, c.updated_at = CURRENT_TIMESTAMP
 WHERE s.song_id = 2131 AND s.version = 29 AND BINARY s.song_name = BINARY 'TEKNO KEISER' AND BINARY COALESCE(s.artist_name, '') = BINARY 'STEVIE(Trymatic)/Nana Takahashi Prod. by L.E.D.'
   AND c.chart_id = 7548 AND c.difficulty_code = 4 AND c.chart_version = 29 AND c.is_upper = 0 AND c.is_deleted = FALSE AND c.level = 47;
