-- Genre refresh from https://p.eagate.573.jp/game/popn/popn29/music/list.html?version=27&sort=music&sort_type=up

-- Preserve song/chart IDs, existing song hashes, jacket URLs and all playdata.

-- Each update requires the reviewed identity and old genre; newer edits are not overwritten.

UPDATE songs SET genre_name = 'ケミカルクラッシュ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1539 AND version = 27
   AND BINARY song_name = BINARY '001 -どうしんのかいろ-' AND BINARY artist_name = BINARY '90°club'
   AND BINARY genre_name = BINARY '001 -どうしんのかいろ-';

UPDATE songs SET genre_name = 'ワールドオルタナティブ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1540 AND version = 27
   AND BINARY song_name = BINARY 'Airplane' AND BINARY artist_name = BINARY 'Red Planets'
   AND BINARY genre_name = BINARY 'Airplane';

UPDATE songs SET genre_name = '超覚醒ロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1541 AND version = 27
   AND BINARY song_name = BINARY 'Awakening Wings' AND BINARY artist_name = BINARY '伊達朱里紗'
   AND BINARY genre_name = BINARY 'Awakening Wings';

UPDATE songs SET genre_name = 'セツナ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1543 AND version = 27
   AND BINARY song_name = BINARY 'Bye Bye' AND BINARY artist_name = BINARY '削除'
   AND BINARY genre_name = BINARY 'Bye Bye';

UPDATE songs SET genre_name = 'ナイトメアプリマ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1544 AND version = 27
   AND BINARY song_name = BINARY 'Candy Crime Toe Shoes' AND BINARY artist_name = BINARY 'アリスシャッハと魔法の楽団'
   AND BINARY genre_name = BINARY 'Candy Crime Toe Shoes';

UPDATE songs SET genre_name = 'ドーパミンハイテック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1545 AND version = 27
   AND BINARY song_name = BINARY 'Empty Backdoor' AND BINARY artist_name = BINARY '森羅万象'
   AND BINARY genre_name = BINARY 'Empty Backdoor';

UPDATE songs SET genre_name = 'デライトフルデジポップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1546 AND version = 27
   AND BINARY song_name = BINARY 'Engraved on my heart ft. 小林マナ' AND BINARY artist_name = BINARY 'Xceon'
   AND BINARY genre_name = BINARY 'Engraved on my heart ft. 小林マナ';

UPDATE songs SET genre_name = 'ワールドエレクトロニカ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1547 AND version = 27
   AND BINARY song_name = BINARY 'fallen leaves -IIDX edition-' AND BINARY artist_name = BINARY '猫叉Master'
   AND BINARY genre_name = BINARY 'fallen leaves -IIDX edition-';

UPDATE songs SET genre_name = 'ガムランガバ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1548 AND version = 27
   AND BINARY song_name = BINARY 'Gabbalungang' AND BINARY artist_name = BINARY 'Hommarju'
   AND BINARY genre_name = BINARY 'Gabbalungang';

UPDATE songs SET genre_name = 'スピニングビーツ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1549 AND version = 27
   AND BINARY song_name = BINARY 'HAGURUMA' AND BINARY artist_name = BINARY 'School'
   AND BINARY genre_name = BINARY 'HAGURUMA';

UPDATE songs SET genre_name = 'リムピッド', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1550 AND version = 27
   AND BINARY song_name = BINARY 'Hibiki' AND BINARY artist_name = BINARY 'ARForest feat.nayuta'
   AND BINARY genre_name = BINARY 'Hibiki';

UPDATE songs SET genre_name = 'デッドヒートレイヴ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1551 AND version = 27
   AND BINARY song_name = BINARY 'High Speed Junkie!' AND BINARY artist_name = BINARY 'BEMANI Sound Team "KE!JU"'
   AND BINARY genre_name = BINARY 'High Speed Junkie!';

UPDATE songs SET genre_name = 'スムースコンテンポラリー', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1553 AND version = 27
   AND BINARY song_name = BINARY 'Indigo Nocturne' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Power Of Nature"'
   AND BINARY genre_name = BINARY 'Indigo Nocturne';

UPDATE songs SET genre_name = 'J-ロックΦNEXT', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1554 AND version = 27
   AND BINARY song_name = BINARY 'Journey' AND BINARY artist_name = BINARY 'colors'
   AND BINARY genre_name = BINARY 'Journey';

UPDATE songs SET genre_name = 'ハピコアMAX', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1555 AND version = 27
   AND BINARY song_name = BINARY 'LIMIT TOPPA REVOLUTION' AND BINARY artist_name = BINARY 'BEMANI Sound Team "PON" feat.NU-KO'
   AND BINARY genre_name = BINARY 'LIMIT TOPPA REVOLUTION';

UPDATE songs SET genre_name = 'ジャパネスク', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1556 AND version = 27
   AND BINARY song_name = BINARY 'MA・TSU・RI' AND BINARY artist_name = BINARY 'かなたん,アマギセーラ,ぁゅ by BEMANI Sound Team "藤森崇多"'
   AND BINARY genre_name = BINARY 'MA・TSU・RI';

UPDATE songs SET genre_name = 'カワイイブレイクコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1557 AND version = 27
   AND BINARY song_name = BINARY 'Mecha Kawa Breaker!!' AND BINARY artist_name = BINARY 'BEMANI Sound Team "ZAQUVA"'
   AND BINARY genre_name = BINARY 'Mecha Kawa Breaker!!';

UPDATE songs SET genre_name = 'オービタリックエレクトリックミュージック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1559 AND version = 27
   AND BINARY song_name = BINARY 'MOVE! (We Keep It Movin'')' AND BINARY artist_name = BINARY 'Jonny Dynamite!,Lisa - paint with stars -,Rio Hiiragi by BEMANI Sound Team "U1-ASAMi"'
   AND BINARY genre_name = BINARY 'MOVE! (We Keep It Movin'')';

UPDATE songs SET genre_name = 'スカイパティスリー', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1561 AND version = 27
   AND BINARY song_name = BINARY 'pastel@sweets labo(*''v''*)' AND BINARY artist_name = BINARY 'm@sumi & くりむ'
   AND BINARY genre_name = BINARY 'pastel@sweets labo(*''v''*)';

UPDATE songs SET genre_name = 'キャンディレイヴランド', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1562 AND version = 27
   AND BINARY song_name = BINARY 'Pure Rude' AND BINARY artist_name = BINARY 'kors k'
   AND BINARY genre_name = BINARY 'Pure Rude';

UPDATE songs SET genre_name = 'ヘルメタル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1563 AND version = 27
   AND BINARY song_name = BINARY 'Satan' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Devil Summoner"'
   AND BINARY genre_name = BINARY 'Satan';

UPDATE songs SET genre_name = 'スラッシュロア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1564 AND version = 27
   AND BINARY song_name = BINARY 'Shout It Out' AND BINARY artist_name = BINARY 'G-T-R'
   AND BINARY genre_name = BINARY 'Shout It Out';

UPDATE songs SET genre_name = 'ライフストリーム', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1565 AND version = 27
   AND BINARY song_name = BINARY 'SOLID STATE SQUAD -RISEN RELIC REMIX-' AND BINARY artist_name = BINARY 'kors k vs. L.E.D. (Remixed by xi)'
   AND BINARY genre_name = BINARY 'SOLID STATE SQUAD -RISEN RELIC REMIX-';

UPDATE songs SET genre_name = 'ワールドハウス BAND', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1567 AND version = 27
   AND BINARY song_name = BINARY 'THE SAFARI - NEETs ver. -' AND BINARY artist_name = BINARY '東京アクティブNEETs'
   AND BINARY genre_name = BINARY 'THE SAFARI - NEETs ver. -';

UPDATE songs SET genre_name = '幻想音樂2', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1569 AND version = 27
   AND BINARY song_name = BINARY 'Τέλος' AND BINARY artist_name = BINARY 'BEMANI Sound Team "HuΣeR" feat.ゆきまめ'
   AND BINARY genre_name = BINARY 'Τέλος';

UPDATE songs SET genre_name = 'クラシカルアートコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1570 AND version = 27
   AND BINARY song_name = BINARY 'unisonote' AND BINARY artist_name = BINARY 'onoken'
   AND BINARY genre_name = BINARY 'unisonote';

UPDATE songs SET genre_name = 'アラビアンプログレ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1571 AND version = 27
   AND BINARY song_name = BINARY 'Unknown Region' AND BINARY artist_name = BINARY 'REVERSE JOY'
   AND BINARY genre_name = BINARY 'Unknown Region';

UPDATE songs SET genre_name = 'エンピリアルハイドロアートコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1573 AND version = 27
   AND BINARY song_name = BINARY 'VOLAQUAS' AND BINARY artist_name = BINARY 'BEMANI Sound Team "DJ TOTTO VS 兎々"'
   AND BINARY genre_name = BINARY 'VOLAQUAS';

UPDATE songs SET genre_name = 'Xハードコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1574 AND version = 27
   AND BINARY song_name = BINARY 'Xジェネの逆襲' AND BINARY artist_name = BINARY 'BEMANI Sound Team "dj razzle dazzle"'
   AND BINARY genre_name = BINARY 'Xジェネの逆襲';

UPDATE songs SET genre_name = 'パーティーフュージョン', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1576 AND version = 27
   AND BINARY song_name = BINARY 'あまるがむ' AND BINARY artist_name = BINARY '山本真央樹'
   AND BINARY genre_name = BINARY 'あまるがむ';

UPDATE songs SET genre_name = 'ヤマトスピリット', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1577 AND version = 27
   AND BINARY song_name = BINARY '粋 -IKI-' AND BINARY artist_name = BINARY 'BEMANI Sound Team "藤森崇多"'
   AND BINARY genre_name = BINARY '粋 -IKI-';

UPDATE songs SET genre_name = 'クロスブリード', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1585 AND version = 27
   AND BINARY song_name = BINARY '鴉' AND BINARY artist_name = BINARY 'L.E.D.-G'
   AND BINARY genre_name = BINARY '鴉';

UPDATE songs SET genre_name = 'シャーマンダンス', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1586 AND version = 27
   AND BINARY song_name = BINARY 'グランデーロの守り' AND BINARY artist_name = BINARY 'BEMANI Sound Team "劇団レコード" feat.霜月はるか'
   AND BINARY genre_name = BINARY 'グランデーロの守り';

UPDATE songs SET genre_name = 'ラブリーサンバ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1587 AND version = 27
   AND BINARY song_name = BINARY '恋するMonstro' AND BINARY artist_name = BINARY 'sei☆shin feat.あまねこゆ'
   AND BINARY genre_name = BINARY '恋するMonstro';

UPDATE songs SET genre_name = 'メロメロユーロ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1588 AND version = 27
   AND BINARY song_name = BINARY '恋とメロンとキューピット' AND BINARY artist_name = BINARY 'まろん feat. キャサリン'
   AND BINARY genre_name = BINARY '恋とメロンとキューピット';

UPDATE songs SET genre_name = '爆走ユーロビート', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1592 AND version = 27
   AND BINARY song_name = BINARY '情熱タンデムRUNAWAY' AND BINARY artist_name = BINARY 'Nana Takahashi & BEMANI Sound Team "L.E.D."'
   AND BINARY genre_name = BINARY '情熱タンデムRUNAWAY';

UPDATE songs SET genre_name = 'スノーウィーノクターン', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1593 AND version = 27
   AND BINARY song_name = BINARY '蒼氷のフラグメント' AND BINARY artist_name = BINARY 'BEMANI Sound Team "JYUNN"'
   AND BINARY genre_name = BINARY '蒼氷のフラグメント';

UPDATE songs SET genre_name = 'スターライトビート', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1594 AND version = 27
   AND BINARY song_name = BINARY '空駆けるスピードスター' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Sota Fujimori" feat. Kanata.N'
   AND BINARY genre_name = BINARY '空駆けるスピードスター';

UPDATE songs SET genre_name = 'オーバーキャスト', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1595 AND version = 27
   AND BINARY song_name = BINARY 'ダイバージェンス' AND BINARY artist_name = BINARY 'T-HEY & TANEKO'
   AND BINARY genre_name = BINARY 'ダイバージェンス';

UPDATE songs SET genre_name = 'シティウォークトラップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1598 AND version = 27
   AND BINARY song_name = BINARY '東京メモリー' AND BINARY artist_name = BINARY '暁(from My Complex of Academy)'
   AND BINARY genre_name = BINARY '東京メモリー';

UPDATE songs SET genre_name = 'オモイデチップチューン', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1601 AND version = 27
   AND BINARY song_name = BINARY '夏色のセーブデータ' AND BINARY artist_name = BINARY 'BEMANI Sound Team "KE!JU"'
   AND BINARY genre_name = BINARY '夏色のセーブデータ';

UPDATE songs SET genre_name = 'にゃんにゃんマーチ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1602 AND version = 27
   AND BINARY song_name = BINARY 'にゃんのパレードマーチ♪' AND BINARY artist_name = BINARY 'DJ TOTTO feat.にゃん'
   AND BINARY genre_name = BINARY 'にゃんのパレードマーチ♪';

UPDATE songs SET genre_name = 'モーマンタイレイバーズ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1603 AND version = 27
   AND BINARY song_name = BINARY 'ノープラン・デイズ' AND BINARY artist_name = BINARY 'TORIENA'
   AND BINARY genre_name = BINARY 'ノープラン・デイズ';

UPDATE songs SET genre_name = 'アドレナリンパンク', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1604 AND version = 27
   AND BINARY song_name = BINARY '脳ミソ de 向上' AND BINARY artist_name = BINARY '豚乙女'
   AND BINARY genre_name = BINARY '脳ミソ de 向上';

UPDATE songs SET genre_name = 'サディスティックブレイクコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1605 AND version = 27
   AND BINARY song_name = BINARY '灰の羽搏' AND BINARY artist_name = BINARY 'かめりあ feat. かめりあ'
   AND BINARY genre_name = BINARY '灰の羽搏';

UPDATE songs SET genre_name = 'メルティーデジロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1606 AND version = 27
   AND BINARY song_name = BINARY '悪夢♡ショコラティエ' AND BINARY artist_name = BINARY 'My Complex of Academy'
   AND BINARY genre_name = BINARY '悪夢♡ショコラティエ';

UPDATE songs SET genre_name = 'ダークネスアルケミー', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1609 AND version = 27
   AND BINARY song_name = BINARY 'ホムンクルスレシピ' AND BINARY artist_name = BINARY 'アマギセーラ'
   AND BINARY genre_name = BINARY 'ホムンクルスレシピ';

UPDATE songs SET genre_name = 'ハードトランス', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1611 AND version = 27
   AND BINARY song_name = BINARY '無意識のフィロソフィア' AND BINARY artist_name = BINARY 'BEMANI Sound Team "TAG"'
   AND BINARY genre_name = BINARY '無意識のフィロソフィア';

UPDATE songs SET genre_name = 'ガールズヴィジュアル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1612 AND version = 27
   AND BINARY song_name = BINARY '斑咲花' AND BINARY artist_name = BINARY 'mami,駄々子 by BEMANI Sound Team "Akhuta Works"'
   AND BINARY genre_name = BINARY '斑咲花';

UPDATE songs SET genre_name = 'リベリオンメタル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1613 AND version = 27
   AND BINARY song_name = BINARY '明滅の果てに' AND BINARY artist_name = BINARY 'ELFENSJóN'
   AND BINARY genre_name = BINARY '明滅の果てに';

UPDATE songs SET genre_name = 'ブレイブロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1615 AND version = 27
   AND BINARY song_name = BINARY '勇猛無比' AND BINARY artist_name = BINARY 'Upper Cape Project'
   AND BINARY genre_name = BINARY '勇猛無比';

UPDATE songs SET genre_name = 'ボルテナイズドドリームステップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1616 AND version = 27
   AND BINARY song_name = BINARY 'ユメブキ' AND BINARY artist_name = BINARY '紫崎 雪,Risa Yuzuki,709sec. by BEMANI Sound Team "PHQUASE & SYUNN"'
   AND BINARY genre_name = BINARY 'ユメブキ';

UPDATE songs SET genre_name = 'ラブケミストリー', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1617 AND version = 27
   AND BINARY song_name = BINARY 'ラブケミ' AND BINARY artist_name = BINARY 'red glasses Trio feat.Sana'
   AND BINARY genre_name = BINARY 'ラブケミ';

UPDATE songs SET genre_name = 'ガールズマインドポップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1618 AND version = 27
   AND BINARY song_name = BINARY 'リナリア' AND BINARY artist_name = BINARY 'OSTER project feat. hinatanso'
   AND BINARY genre_name = BINARY 'リナリア';

UPDATE songs SET genre_name = 'スピリチュアルコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1619 AND version = 27
   AND BINARY song_name = BINARY '輪廻の鴉' AND BINARY artist_name = BINARY 'BEMANI Sound Team "dj TAKA"'
   AND BINARY genre_name = BINARY '輪廻の鴉';

UPDATE songs SET genre_name = 'アイリッシュミソロジー', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1820 AND version = 27
   AND BINARY song_name = BINARY 'Amulet of Enbarr' AND BINARY artist_name = BINARY 'Cororo'
   AND BINARY genre_name = BINARY 'Amulet of Enbarr';

UPDATE songs SET genre_name = 'スターリーベースハウス', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1821 AND version = 27
   AND BINARY song_name = BINARY 'Caldwell 99' AND BINARY artist_name = BINARY 'BlackY'
   AND BINARY genre_name = BINARY 'Caldwell 99';

UPDATE songs SET genre_name = 'ソーサリーメタル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1822 AND version = 27
   AND BINARY song_name = BINARY 'Hexer' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Yvya"'
   AND BINARY genre_name = BINARY 'Hexer';

UPDATE songs SET genre_name = 'ジャジーグルーヴ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1823 AND version = 27
   AND BINARY song_name = BINARY 'ISERBROOK' AND BINARY artist_name = BINARY 'SOUND HOLIC'
   AND BINARY genre_name = BINARY 'ISERBROOK';

UPDATE songs SET genre_name = 'ハイスピードテックコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1824 AND version = 27
   AND BINARY song_name = BINARY 'Sword of Vengeance' AND BINARY artist_name = BINARY 'sky_delta'
   AND BINARY genre_name = BINARY 'Sword of Vengeance';

UPDATE songs SET genre_name = '重奏戦記', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1825 AND version = 27
   AND BINARY song_name = BINARY '葬送のエウロパ' AND BINARY artist_name = BINARY '工藤吉三（ベイシスケイプ）'
   AND BINARY genre_name = BINARY '葬送のエウロパ';

UPDATE songs SET genre_name = 'レゾン', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1826 AND version = 27
   AND BINARY song_name = BINARY 'ただ、それだけの理由で' AND BINARY artist_name = BINARY 'BEMANI Sound Team "あさき"'
   AND BINARY genre_name = BINARY 'ただ、それだけの理由で';

UPDATE songs SET genre_name = 'ロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1827 AND version = 27
   AND BINARY song_name = BINARY 'パーフェクトイーター' AND BINARY artist_name = BINARY 'BEMANI Sound Team "PON" feat.かなたん'
   AND BINARY genre_name = BINARY 'パーフェクトイーター';

UPDATE songs SET genre_name = 'チャイニーズラッシュ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1828 AND version = 27
   AND BINARY song_name = BINARY '満漢全席火花ノ舞' AND BINARY artist_name = BINARY 'onoken'
   AND BINARY genre_name = BINARY '満漢全席火花ノ舞';

UPDATE songs SET genre_name = 'ハードコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1829 AND version = 27
   AND BINARY song_name = BINARY 'mathematical good-bye' AND BINARY artist_name = BINARY '三代目 ADULTIC TEACHERS feat. BEMANI Sound Team "スコーピオン志村"'
   AND BINARY genre_name = BINARY 'mathematical good-bye';

UPDATE songs SET genre_name = 'エウレカ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1830 AND version = 27
   AND BINARY song_name = BINARY 'F/S' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Power Of Nature"'
   AND BINARY genre_name = BINARY 'F/S';

UPDATE songs SET genre_name = 'A.I.ステップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1831 AND version = 27
   AND BINARY song_name = BINARY 'Tan♪Tan♪Tan♪' AND BINARY artist_name = BINARY 'BEMANI Sound Team "seiya-murai" feat ALT'
   AND BINARY genre_name = BINARY 'Tan♪Tan♪Tan♪';

UPDATE songs SET genre_name = 'バーニングロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1832 AND version = 27
   AND BINARY song_name = BINARY 'Keep the Faith' AND BINARY artist_name = BINARY '吾龍'
   AND BINARY genre_name = BINARY 'Keep the Faith';

UPDATE songs SET genre_name = 'ちゅきちゅきテクノポップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1833 AND version = 27
   AND BINARY song_name = BINARY 'Lovin'' You' AND BINARY artist_name = BINARY 'NU-KO'
   AND BINARY genre_name = BINARY 'Lovin'' You';

UPDATE songs SET genre_name = 'ゴシックヴィジュアル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1834 AND version = 27
   AND BINARY song_name = BINARY 'Redemption Tears' AND BINARY artist_name = BINARY 'BEMANI Sound Team "劇団レコード" feat.T4K'
   AND BINARY genre_name = BINARY 'Redemption Tears';

UPDATE songs SET genre_name = 'バブルディスコファンク', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1835 AND version = 27
   AND BINARY song_name = BINARY 'Dancin'' in シャングリラ' AND BINARY artist_name = BINARY '阿部靖広 feat.Shiori'
   AND BINARY genre_name = BINARY 'Dancin'' in シャングリラ';

UPDATE songs SET genre_name = 'アミューズメントポップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1836 AND version = 27
   AND BINARY song_name = BINARY 'ココロコースター' AND BINARY artist_name = BINARY 'ウッチーズ'
   AND BINARY genre_name = BINARY 'ココロコースター';

UPDATE songs SET genre_name = 'アンジェリーク', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1837 AND version = 27
   AND BINARY song_name = BINARY 'ma plume' AND BINARY artist_name = BINARY 'nonuplet VS BEMANI Sound Team "Power Of Nature"'
   AND BINARY genre_name = BINARY 'ma plume';

UPDATE songs SET genre_name = 'アンジェリーク', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1838 AND version = 27
   AND BINARY song_name = BINARY 'ma plume' AND BINARY artist_name = BINARY 'nonuplet VS BEMANI Sound Team "Power Of Nature"'
   AND BINARY genre_name = BINARY 'ma plume';

UPDATE songs SET genre_name = 'クランブル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1839 AND version = 27
   AND BINARY song_name = BINARY 'いばら姫' AND BINARY artist_name = BINARY '藤野マナミ & うさおりーぬ'
   AND BINARY genre_name = BINARY 'いばら姫';

UPDATE songs SET genre_name = 'ボーイズロックスタイル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1840 AND version = 27
   AND BINARY song_name = BINARY '螺旋' AND BINARY artist_name = BINARY 'Lie☆Rays'
   AND BINARY genre_name = BINARY '螺旋';

UPDATE songs SET genre_name = 'フューチャーベース', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1841 AND version = 27
   AND BINARY song_name = BINARY 'ポラリスノウタ' AND BINARY artist_name = BINARY 'ここなつ2.0'
   AND BINARY genre_name = BINARY 'ポラリスノウタ';

UPDATE songs SET genre_name = 'ハイパーアクティブヘルコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1843 AND version = 27
   AND BINARY song_name = BINARY 'めうめうぺったんたん！！ (ZAQUVA Remix)' AND BINARY artist_name = BINARY 'Remixed by BEMANI Sound Team "ZAQUVA"'
   AND BINARY genre_name = BINARY 'めうめうぺったんたん！！ (ZAQUVA Remix)';

UPDATE songs SET genre_name = 'スイーツプログレメタル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1844 AND version = 27
   AND BINARY song_name = BINARY 'ちくわパフェだよ☆ＣＫＰ (Yvya Remix)' AND BINARY artist_name = BINARY 'Remixed by BEMANI Sound Team "Yvya"'
   AND BINARY genre_name = BINARY 'ちくわパフェだよ☆ＣＫＰ (Yvya Remix)';

UPDATE songs SET genre_name = 'ヒップロック8', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1847 AND version = 27
   AND BINARY song_name = BINARY '狼弦暴威' AND BINARY artist_name = BINARY 'Des-ROW・組スペシアル'
   AND BINARY genre_name = BINARY '狼弦暴威';

UPDATE songs SET genre_name = 'ハードヴィジュアル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1848 AND version = 27
   AND BINARY song_name = BINARY 'The Escape' AND BINARY artist_name = BINARY 'RENO feat.夕霧'
   AND BINARY genre_name = BINARY 'The Escape';

UPDATE songs SET genre_name = 'ハイパーミステリーポップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1849 AND version = 27
   AND BINARY song_name = BINARY '謎情の雫 ft. Kanae Asaba' AND BINARY artist_name = BINARY 'Xceon'
   AND BINARY genre_name = BINARY '謎情の雫 ft. Kanae Asaba';

UPDATE songs SET genre_name = '三味線ステップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1850 AND version = 27
   AND BINARY song_name = BINARY '黒猫と珈琲' AND BINARY artist_name = BINARY 'Yocke'
   AND BINARY genre_name = BINARY '黒猫と珈琲';

UPDATE songs SET genre_name = 'インテリジェントジャズ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1851 AND version = 27
   AND BINARY song_name = BINARY 'Head Scratcher' AND BINARY artist_name = BINARY 'ヨナヲスナヲ×北川翔也'
   AND BINARY genre_name = BINARY 'Head Scratcher';

UPDATE songs SET genre_name = 'パンダフュージョン', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1854 AND version = 27
   AND BINARY song_name = BINARY '遊戯大熊猫' AND BINARY artist_name = BINARY '98'
   AND BINARY genre_name = BINARY '遊戯大熊猫';

UPDATE songs SET genre_name = 'スタイリッシュテクノ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1855 AND version = 27
   AND BINARY song_name = BINARY 'Stylus' AND BINARY artist_name = BINARY 'BEMANI Sound Team "HuΣeR"'
   AND BINARY genre_name = BINARY 'Stylus';

UPDATE songs SET genre_name = 'フューチャーハウス', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1856 AND version = 27
   AND BINARY song_name = BINARY 'Crazy Shuffle' AND BINARY artist_name = BINARY 'Yooh'
   AND BINARY genre_name = BINARY 'Crazy Shuffle';

UPDATE songs SET genre_name = 'フューチャーフュージョン2', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1857 AND version = 27
   AND BINARY song_name = BINARY 'speedstar[02]' AND BINARY artist_name = BINARY 'm@sumi'
   AND BINARY genre_name = BINARY 'speedstar[02]';

UPDATE songs SET genre_name = 'ポストルネッサンス', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1858 AND version = 27
   AND BINARY song_name = BINARY '少年A' AND BINARY artist_name = BINARY 'Remixed by 少年ラジオ'
   AND BINARY genre_name = BINARY '少年A';

UPDATE songs SET genre_name = 'インディーズヒップパンク', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1859 AND version = 27
   AND BINARY song_name = BINARY 'what I wish' AND BINARY artist_name = BINARY 'E.S.P. feat.星野奏子'
   AND BINARY genre_name = BINARY 'what I wish';

UPDATE songs SET genre_name = 'ビヨンドロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1860 AND version = 27
   AND BINARY song_name = BINARY 'TAKE YOU AWAY' AND BINARY artist_name = BINARY 'SHIN feat.MiA & BEMANI Sound Team "あさき"'
   AND BINARY genre_name = BINARY 'TAKE YOU AWAY';

UPDATE songs SET genre_name = 'スレイヤーメタル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1861 AND version = 27
   AND BINARY song_name = BINARY 'Dragon Blade -The Arrange-' AND BINARY artist_name = BINARY 'BEMANI Sound Team "あさき" ＆ RENO'
   AND BINARY genre_name = BINARY 'Dragon Blade -The Arrange-';

UPDATE songs SET genre_name = 'マッスルガバ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1862 AND version = 27
   AND BINARY song_name = BINARY 'pump up dA CORE' AND BINARY artist_name = BINARY 'RoughSketch'
   AND BINARY genre_name = BINARY 'pump up dA CORE';

UPDATE songs SET genre_name = 'ネオデジタルロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1863 AND version = 27
   AND BINARY song_name = BINARY 'TURBO BOOSTER' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Sota F. with Mr.Screamer"'
   AND BINARY genre_name = BINARY 'TURBO BOOSTER';

UPDATE songs SET genre_name = 'サーキュラーシンフォニカ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1864 AND version = 27
   AND BINARY song_name = BINARY '夜虹' AND BINARY artist_name = BINARY 'BEMANI Sound Team "HuΣeR"'
   AND BINARY genre_name = BINARY '夜虹';

UPDATE songs SET genre_name = 'プログレッシブ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1866 AND version = 27
   AND BINARY song_name = BINARY '天泣' AND BINARY artist_name = BINARY 'BEMANI Sound Team "meteorologists"'
   AND BINARY genre_name = BINARY '天泣';

UPDATE songs SET genre_name = 'オッタマトライバル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1867 AND version = 27
   AND BINARY song_name = BINARY 'オッタマゲッター' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Ota Master"'
   AND BINARY genre_name = BINARY 'オッタマゲッター';

UPDATE songs SET genre_name = 'テンペストコア', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1870 AND version = 27
   AND BINARY song_name = BINARY 'TYPHØN' AND BINARY artist_name = BINARY 'BlackY'
   AND BINARY genre_name = BINARY 'TYPHØN';

UPDATE songs SET genre_name = 'フュージョン', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1871 AND version = 27
   AND BINARY song_name = BINARY 'REFLEXES MANIPULATION' AND BINARY artist_name = BINARY '山本真央樹'
   AND BINARY genre_name = BINARY 'REFLEXES MANIPULATION';

UPDATE songs SET genre_name = 'ドラムンレミニス', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1872 AND version = 27
   AND BINARY song_name = BINARY 'オーバー' AND BINARY artist_name = BINARY 'BEMANI Sound Team "DJ KOMACHI"'
   AND BINARY genre_name = BINARY 'オーバー';

UPDATE songs SET genre_name = 'ポップンポップ3', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1873 AND version = 27
   AND BINARY song_name = BINARY 'Knockin'' on Red Button' AND BINARY artist_name = BINARY 'red glasses feat.藤野マナミ'
   AND BINARY genre_name = BINARY 'Knockin'' on Red Button';

UPDATE songs SET genre_name = 'スラッシュメタル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1874 AND version = 27
   AND BINARY song_name = BINARY 'The Metalist' AND BINARY artist_name = BINARY 'REI'
   AND BINARY genre_name = BINARY 'The Metalist';

UPDATE songs SET genre_name = 'ウェルカムホーム', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1875 AND version = 27
   AND BINARY song_name = BINARY 'イマココ！この瞬間' AND BINARY artist_name = BINARY '日向美ビタースイーツ♪'
   AND BINARY genre_name = BINARY 'イマココ！この瞬間';

UPDATE songs SET genre_name = 'スタイリッシュフェイカー', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1878 AND version = 27
   AND BINARY song_name = BINARY 'Liar×Girl' AND BINARY artist_name = BINARY 'BEMANI Sound Team "HuΣeR" feat.ゆきまめ'
   AND BINARY genre_name = BINARY 'Liar×Girl';

UPDATE songs SET genre_name = 'エンチャントバロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1879 AND version = 27
   AND BINARY song_name = BINARY 'Hades Doll' AND BINARY artist_name = BINARY 'BEMANI Sound Team "Power Of Nature"'
   AND BINARY genre_name = BINARY 'Hades Doll';

UPDATE songs SET genre_name = 'チリ―ステップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1880 AND version = 27
   AND BINARY song_name = BINARY 'アモ' AND BINARY artist_name = BINARY '八月二雪'
   AND BINARY genre_name = BINARY 'アモ';

UPDATE songs SET genre_name = 'ラジカルクラブジャズ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1881 AND version = 27
   AND BINARY song_name = BINARY 'Jazz is Rad' AND BINARY artist_name = BINARY 'ARM (IOSYS) + Brasscapsule'
   AND BINARY genre_name = BINARY 'Jazz is Rad';

UPDATE songs SET genre_name = 'アンカウンタブル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1882 AND version = 27
   AND BINARY song_name = BINARY '不可説不可説転' AND BINARY artist_name = BINARY 'OSTER project'
   AND BINARY genre_name = BINARY '不可説不可説転';

UPDATE songs SET genre_name = 'ドラムンベース', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1883 AND version = 27
   AND BINARY song_name = BINARY 'encounter' AND BINARY artist_name = BINARY '猫叉Master+'
   AND BINARY genre_name = BINARY 'encounter';

UPDATE songs SET genre_name = 'ロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1884 AND version = 27
   AND BINARY song_name = BINARY '閉塞的フレーション' AND BINARY artist_name = BINARY 'Pizuya''s Cell VS BEMANI Sound Team "dj TAKA"'
   AND BINARY genre_name = BINARY '閉塞的フレーション';

UPDATE songs SET genre_name = 'ハイパーデジロック', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1885 AND version = 27
   AND BINARY song_name = BINARY '残像ニ繋ガレタ追憶ノHIDEAWAY' AND BINARY artist_name = BINARY 'SOUND HOLIC Vs. BEMANI Sound Team "KE!JU" feat. Nana Takahashi'
   AND BINARY genre_name = BINARY '残像ニ繋ガレタ追憶ノHIDEAWAY';

UPDATE songs SET genre_name = '幻想フルオン', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1886 AND version = 27
   AND BINARY song_name = BINARY 'SUPER HEROINE!!' AND BINARY artist_name = BINARY 'Amateras Records vs BEMANI Sound Team "TATSUYA" feat. miko'
   AND BINARY genre_name = BINARY 'SUPER HEROINE!!';

UPDATE songs SET genre_name = 'ハードオリエンタル', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1887 AND version = 27
   AND BINARY song_name = BINARY '弾幕信仰' AND BINARY artist_name = BINARY '豚乙女×BEMANI Sound Team "PON"'
   AND BINARY genre_name = BINARY '弾幕信仰';

UPDATE songs SET genre_name = 'リバースサイトランス', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1888 AND version = 27
   AND BINARY song_name = BINARY 'UROBOROS OVERDIVE' AND BINARY artist_name = BINARY 'L.E.D. feat. YURiCa/花たん'
   AND BINARY genre_name = BINARY 'UROBOROS OVERDIVE';

UPDATE songs SET genre_name = 'ドラムステップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1889 AND version = 27
   AND BINARY song_name = BINARY 'Megalara Garuda' AND BINARY artist_name = BINARY 'SYUNN'
   AND BINARY genre_name = BINARY 'Megalara Garuda';

UPDATE songs SET genre_name = 'ドラムステップ', updated_at = CURRENT_TIMESTAMP
 WHERE song_id = 1890 AND version = 27
   AND BINARY song_name = BINARY 'Megalara Garuda' AND BINARY artist_name = BINARY 'SYUNN'
   AND BINARY genre_name = BINARY 'Megalara Garuda';
