-- Generated migration for confirmed new High Cheers songs and charts.
START TRANSACTION;
CREATE TEMPORARY TABLE new_song_catalog (
  song_hash CHAR(64) PRIMARY KEY, genre_name VARCHAR(255) NOT NULL,
  song_name VARCHAR(255) NOT NULL, artist_name VARCHAR(255) NOT NULL,
  version INT NOT NULL, jacket_url VARCHAR(512) NOT NULL, is_upper BOOLEAN NOT NULL,
  light_level TINYINT, normal_level TINYINT, hyper_level TINYINT, ex_level TINYINT
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO new_song_catalog VALUES
('72aabb2e749be3e07b407e3b8a31d2862cdf937b1875926974020b7a875d61b8', 'バンギャー', 'A Certified Banger Moment', 'めめめ', 29, 'https://static.popn.gg/72aabb2e749be3e07b407e3b8a31d2862cdf937b1875926974020b7a875d61b8.png', FALSE, 11, 29, 40, 47),
('eff6d66c1d7771cf583202cf072c2a37a11e527df42e2367b1c65275aa24c560', 'DTMスピードラン', 'Any%', 'めめめ', 29, 'https://static.popn.gg/eff6d66c1d7771cf583202cf072c2a37a11e527df42e2367b1c65275aa24c560.png', FALSE, 11, 28, 41, 47),
('517b742085b186db324353b1b4ea5f8736b5e5164ac5ae9123363cc0447c4a0a', 'パニックポップ', 'BABY P', 'Plus-tech Squeeze Box', 29, 'https://static.popn.gg/517b742085b186db324353b1b4ea5f8736b5e5164ac5ae9123363cc0447c4a0a.png', FALSE, 8, 25, 35, 42),
('d79e13d0ce491c4516abeb10ff72ee6e5517b31bac6c4ff2fd849854affdc712', 'FMジャジーテック', 'BitRacer', 'red glasses vs Ujico*', 29, 'https://static.popn.gg/d79e13d0ce491c4516abeb10ff72ee6e5517b31bac6c4ff2fd849854affdc712.png', FALSE, 16, 34, 42, 48),
('0544caf88393e7028ffe914449a57541febc230d373723bafc62bb0a0df29130', 'ガルタナネオ', 'BITTER-ENDER', 'タケベエイスケ feat.終ワ子', 29, 'https://static.popn.gg/0544caf88393e7028ffe914449a57541febc230d373723bafc62bb0a0df29130.png', FALSE, 7, 27, 38, 45),
('7b37e2d677306d00c0b09b157ae8862d06241568f04e0af6382c2f25d09f27a0', 'Bling-Bang-Bang-Born', 'Bling-Bang-Bang-Born', 'Covered by uno(IOSYS) feat. Fra', 29, 'https://static.popn.gg/7b37e2d677306d00c0b09b157ae8862d06241568f04e0af6382c2f25d09f27a0.png', FALSE, 10, 21, 35, 42),
('ae8bd331e2aee2387c7ad4bc66d90d64c5cd33ab17c36ca453a323e2905ebf1a', 'マグナムユーロ', 'BOMBER!BOMBER!BABY!', 'STEVIE(44MAGNUM) Prod. by L.E.D.', 29, 'https://static.popn.gg/ae8bd331e2aee2387c7ad4bc66d90d64c5cd33ab17c36ca453a323e2905ebf1a.png', FALSE, 15, 30, 43, 48),
('17e2641c93febd1d5b8b9987417d6600ae384f7c90b8aedaf3d61b5e72af23ea', 'BOW AND ARROW', 'BOW AND ARROW', '米津玄師', 29, 'https://static.popn.gg/17e2641c93febd1d5b8b9987417d6600ae384f7c90b8aedaf3d61b5e72af23ea.png', FALSE, 5, 18, 34, 41),
('229d59d429f7b7e9c8a49e8e1c57ffe0271bad13deb8e0eb5659c3bf9e80c701', 'BRAND NEW STARS!!', 'BRAND NEW STARS!!', 'Switch', 29, 'https://static.popn.gg/229d59d429f7b7e9c8a49e8e1c57ffe0271bad13deb8e0eb5659c3bf9e80c701.png', FALSE, 3, 17, 31, 39),
('b77deebd42b1f94d05565b572ecdd18cb9c713c7de91b7bede00c7f241cc0722', 'BRIGHTEST STARS!!', 'BRIGHTEST STARS!!', 'Knights', 29, 'https://static.popn.gg/b77deebd42b1f94d05565b572ecdd18cb9c713c7de91b7bede00c7f241cc0722.png', FALSE, 4, 20, 34, 40),
('93e71704495c439a6c3e3ed5eb99fd09e459446794c715f4b86458b1ec5146a3', 'バスマティ', 'Candy love', '阿部靖広 feat.Shiori', 29, 'https://static.popn.gg/93e71704495c439a6c3e3ed5eb99fd09e459446794c715f4b86458b1ec5146a3.png', FALSE, 10, 23, 38, 42),
('c2dd59604d2b1248823d9d212c5a7ca3fb4e8aefaf099ceb10989be5ca479276', 'ユーロビート', 'Daisuke', 'Y&Co.', 29, 'https://static.popn.gg/c2dd59604d2b1248823d9d212c5a7ca3fb4e8aefaf099ceb10989be5ca479276.png', FALSE, 6, 29, 41, 46),
('94f4af875901e71df3d3681129e522ab1c7d3bd309d2da0b4b0c46a10745986b', '丼ベース', 'Deep tenDon Reflex', 'BEMANI Sound Team "Coyaan"', 29, 'https://static.popn.gg/94f4af875901e71df3d3681129e522ab1c7d3bd309d2da0b4b0c46a10745986b.png', FALSE, 13, 31, 41, 47),
('b0ce27f596b065b12c7482cd6640a136e3dec2febb11178c1b896c34bfb96c1f', 'アンサラー', 'Fragarach', 'ARForest vs. Zekk', 29, 'https://static.popn.gg/b0ce27f596b065b12c7482cd6640a136e3dec2febb11178c1b896c34bfb96c1f.png', FALSE, 16, 31, 43, 48),
('fb90ca2282e0cd274484e204dab806e6dc45bcb654025a7c19fd5753b1eaff7d', 'FUSIONIC STARS!!', 'FUSIONIC STARS!!', 'Valkyrie', 29, 'https://static.popn.gg/fb90ca2282e0cd274484e204dab806e6dc45bcb654025a7c19fd5753b1eaff7d.png', FALSE, 4, 18, 30, 41),
('417dbc742869c03b0dbbc18489f2aedf12f6799f774cb890b06d715569ec7108', 'ハイパーデジメタル', 'FUZIN RIZIN', 'SOUND HOLIC feat. Nana Takahashi & 709sec.', 29, 'https://static.popn.gg/417dbc742869c03b0dbbc18489f2aedf12f6799f774cb890b06d715569ec7108.png', FALSE, 12, 31, 41, 48),
('f765472024b00584154f2df332bfa1e9e99f9042fa087b67edf9a801d2fcddc2', 'ソフトロックＬＯＮＧ', 'Homesick Pt.2&3', 'covered by 不知火フレア', 29, 'https://static.popn.gg/f765472024b00584154f2df332bfa1e9e99f9042fa087b67edf9a801d2fcddc2.png', FALSE, 4, 21, 33, 42),
('40cf5863d93e7d4ce1a0733c945f19899e5226a3c92db0ba8bcbd1e8ca9c4d47', 'オービタリック K-スタイル', 'HYPER LUV', 'Rev Girls and Boy feat. U1-ASAMi', 29, 'https://static.popn.gg/40cf5863d93e7d4ce1a0733c945f19899e5226a3c92db0ba8bcbd1e8ca9c4d47.png', FALSE, 11, 28, 37, 45),
('da23e311dcf9f41d4a6fe9b0ac5c5d17bc3076fd37744e559a2d8bfe3f18b2b0', 'ゴシックナイトメア2', 'JADAMGA', 'onoken', 29, 'https://static.popn.gg/da23e311dcf9f41d4a6fe9b0ac5c5d17bc3076fd37744e559a2d8bfe3f18b2b0.png', FALSE, 11, 33, 43, 48),
('0a2774e49c49dc0ba793eefbfc2b0628ef11b28c5b887b6b38b528f37e6e97fa', 'KAWAII FESTIVAL', 'KAWAII FESTIVAL', 'ハローキティ', 29, 'https://static.popn.gg/0a2774e49c49dc0ba793eefbfc2b0628ef11b28c5b887b6b38b528f37e6e97fa.png', FALSE, 5, 13, 25, 33),
('4f9b99ed095ea88136b1d737c1dbd2c7242863f3f084dddd650bb61f1be14518', 'KING', 'KING', 'Remixed by SOUND HOLIC feat. はるの', 29, 'https://static.popn.gg/4f9b99ed095ea88136b1d737c1dbd2c7242863f3f084dddd650bb61f1be14518.png', FALSE, 9, 20, 35, 44),
('06f7585fd4fb9974a536fcb0204e7600d6faa508bcdab923b224c6f916ffc0aa', 'オルタナティブアビス', 'Klopfgeist', 'Akino × 零 -zero-', 29, 'https://static.popn.gg/06f7585fd4fb9974a536fcb0204e7600d6faa508bcdab923b224c6f916ffc0aa.png', FALSE, 13, 28, 39, 48),
('e47d20cac6145a68eae7a4b826fe6bed75a0411c41e5ff63bd993a2b54a33591', 'レサジー', 'meme', '袖野あらわ feat.終末うにこ', 29, 'https://static.popn.gg/e47d20cac6145a68eae7a4b826fe6bed75a0411c41e5ff63bd993a2b54a33591.png', FALSE, 6, 22, 37, 43),
('30d42e939dfb51a2d18039538b21b913044ea7099c7805c588c92356615b2640', 'アマノガワレイヴ', 'MILKY HIGHWAY', 'PON feat.NU-KO', 29, 'https://static.popn.gg/30d42e939dfb51a2d18039538b21b913044ea7099c7805c588c92356615b2640.png', FALSE, 14, 31, 40, 46),
('b3d1170680b156ec82a4f3350578ef175a57ec0e9df4b0067b38eae907b94243', 'ハードダンス', 'Monkey Business', 'kors k', 29, 'https://static.popn.gg/b3d1170680b156ec82a4f3350578ef175a57ec0e9df4b0067b38eae907b94243.png', FALSE, 11, 27, 40, 47),
('6b79a8bb41e8dd55bff602f40a3e0bf6b430135c3109d1b7c564d0ef54c004b5', 'グリッチダブステップ', 'Necroxis Girl', 'BEMANI Sound Team "HuΣeR"', 29, 'https://static.popn.gg/6b79a8bb41e8dd55bff602f40a3e0bf6b430135c3109d1b7c564d0ef54c004b5.png', FALSE, 15, 32, 40, 47),
('835ed4ed8436e8b46039b58102670dd2c8917498979a40a73d4223559616f669', 'One with One', 'One with One', 'Ra*bits', 29, 'https://static.popn.gg/835ed4ed8436e8b46039b58102670dd2c8917498979a40a73d4223559616f669.png', FALSE, 5, 21, 32, 42),
('5536f6f92d77e768df3398bba40c3da12b294e6943793b018c2b68464c222073', 'セレスタル', 'Oort', 'Yvya', 29, 'https://static.popn.gg/5536f6f92d77e768df3398bba40c3da12b294e6943793b018c2b68464c222073.png', FALSE, 18, 35, 43, 49),
('7cae827b3332533ac7ec67be811e39dd7f2ccb961c63dab65a4603c2b0720fb5', 'トリップポップ', 'poppin'' journey', 'オトノマート（小池理子×chocck）', 29, 'https://static.popn.gg/7cae827b3332533ac7ec67be811e39dd7f2ccb961c63dab65a4603c2b0720fb5.png', FALSE, 9, 24, 36, 43),
('d5b00cd0ee56531f4177e887ebedf34a3cda0927184c456e8c17cdc59a1be77b', 'カウボーイ2', 'Red Mountain', 'T-Bone', 29, 'https://static.popn.gg/d5b00cd0ee56531f4177e887ebedf34a3cda0927184c456e8c17cdc59a1be77b.png', FALSE, 18, 34, 44, 49),
('0ceaa561e81fff84dc633bef54c68cdb6e7287498f0852269eeb90d26d2569dc', 'ハードサイスピードコア', 'RUINA', 'Dustup VS MAX MAXIMIZER', 29, 'https://static.popn.gg/0ceaa561e81fff84dc633bef54c68cdb6e7287498f0852269eeb90d26d2569dc.png', FALSE, 17, 33, 44, 49),
('ccd8ea186462e4a3df7e40b8452690ba9d4b188d7f7740319607bf9e226489d3', 'プログレッシブトライバル', 'Saturn', 'Mr.Saturn', 29, 'https://static.popn.gg/ccd8ea186462e4a3df7e40b8452690ba9d4b188d7f7740319607bf9e226489d3.png', FALSE, 12, 34, 42, 47),
('916d327f93816562df3678a70e9e80a6035599eda275cafd438d48852d2b1396', 'デジタル J-ポップ', 'Secret Rouge', 'ハレトキドキ', 29, 'https://static.popn.gg/916d327f93816562df3678a70e9e80a6035599eda275cafd438d48852d2b1396.png', FALSE, 12, 26, 38, 45),
('b52457eeba81d2f9638d68f52f94c0d903287c8a156f33e6a626197775921a4c', 'リプルトーン', 'SeLaS', 'm@sumi', 29, 'https://static.popn.gg/b52457eeba81d2f9638d68f52f94c0d903287c8a156f33e6a626197775921a4c.png', FALSE, 9, 25, 37, 46),
('28d17f1db86470cb577f57271ba529e73ae25ff8c8c95b06f6e2b84f0dabee68', 'プロジェクトジルコン', 'Signs and Wonders', 'ネオン(CV:藤川茜)、芽兎めう(CV:五十嵐裕美)', 29, 'https://static.popn.gg/28d17f1db86470cb577f57271ba529e73ae25ff8c8c95b06f6e2b84f0dabee68.png', FALSE, 5, 23, 34, 41),
('32fc40c545e092328c9d31bd43724e5484c0f38b516dda114d403ecdff9e9857', 'Silent Flame,Never Fade', 'Silent Flame,Never Fade', '不知火フレア', 29, 'https://static.popn.gg/32fc40c545e092328c9d31bd43724e5484c0f38b516dda114d403ecdff9e9857.png', FALSE, 6, 19, 35, 44),
('ffacf6e4c0dfa57ec4f5f319e5b2cb1598b9e06693ff69ec92af4a18d74ca4bc', 'ハムパラドックス', 'Smintheus', 'TAN1CHU', 29, 'https://static.popn.gg/ffacf6e4c0dfa57ec4f5f319e5b2cb1598b9e06693ff69ec92af4a18d74ca4bc.png', FALSE, 15, 32, 42, 48),
('0c0d53550b4315d280749931968809b869db96d58cdaa4ce282d6cc96655b867', 'スイートウィスパー', 'Sugar Holic Candy Magic', 'かゆき+うぐ', 29, 'https://static.popn.gg/0c0d53550b4315d280749931968809b869db96d58cdaa4ce282d6cc96655b867.png', FALSE, 10, 25, 36, 45),
('7da2bef1b8bccd8179f168d65402811dc137173e8fcb5fa06958e260521e208a', 'ときめきハウス', 'Take It! Make It!', 'dj TAKA feat.ゆめめ', 29, 'https://static.popn.gg/7da2bef1b8bccd8179f168d65402811dc137173e8fcb5fa06958e260521e208a.png', FALSE, 7, 25, 39, 45),
('e4751d4df48aeb57b89ee68aba46bd81c39cee19a97da7a49baf8012203370da', 'エンパシー', 'This love', 'sei☆shin ft.江崎友梨 from アイドルは下剋上', 29, 'https://static.popn.gg/e4751d4df48aeb57b89ee68aba46bd81c39cee19a97da7a49baf8012203370da.png', FALSE, 8, 25, 38, 45),
('d6396ef33464b21892deb51c24a347a4b094219b5fb2de5a1e28384f806bd56e', 'MPB', 'Tizona d''El Cid', 'TOMOSUKE', 29, 'https://static.popn.gg/d6396ef33464b21892deb51c24a347a4b094219b5fb2de5a1e28384f806bd56e.png', FALSE, 11, 30, 39, 46),
('4962419b25379ad4d4775bb0a2df0cc20bcaf1828bea68386e072ca2378829be', 'ハーツ', 'TOGETOGE feat.パンダの流儀', 'namae.', 29, 'https://static.popn.gg/4962419b25379ad4d4775bb0a2df0cc20bcaf1828bea68386e072ca2378829be.png', FALSE, 14, 28, 38, 46),
('697db05e5108cd787d21fe7cd6ee20d5a67ea7e20e8495d5ad1c024ea216d8f6', 'ウインドシャッフル', 'ÜBER BLANKENESE', 'SOUND HOLIC', 29, 'https://static.popn.gg/697db05e5108cd787d21fe7cd6ee20d5a67ea7e20e8495d5ad1c024ea216d8f6.png', FALSE, 13, 28, 41, 47),
('0ad03fbadc16b6aa194e3b9b8d37c847a2e097a6216945733455bc2a2282a6f5', 'エピックノヴァ', 'Vi∀', '不知火フレア Prod.by PON', 29, 'https://static.popn.gg/0ad03fbadc16b6aa194e3b9b8d37c847a2e097a6216945733455bc2a2282a6f5.png', FALSE, 10, 26, 41, 47),
('c19e7c810df3e183d33f3bc46ffc42e6883ec6ada109fc924de2aa1ab0ddc680', 'Wanderlust', 'Wanderlust', 'キミのね', 29, 'https://static.popn.gg/c19e7c810df3e183d33f3bc46ffc42e6883ec6ada109fc924de2aa1ab0ddc680.png', FALSE, 6, 14, 31, 42),
('2b884edf47f89a83be82e355ccd2c17a4a493b740ff8e3ad1b235e5fb0cde5a0', 'ピアニシモ', '靉靆の小景', 'red glasses', 29, 'https://static.popn.gg/2b884edf47f89a83be82e355ccd2c17a4a493b740ff8e3ad1b235e5fb0cde5a0.png', FALSE, 5, 21, 35, 43),
('4c0ab513040cbb0475c3e9173b6697c01f5ba0640ccef34a8f7d7194db14373e', 'アイドル', 'アイドル', 'Covered by BEMANI Sound Team "HuΣeR × wac × Yvya" feat. 佐伯伊織', 29, 'https://static.popn.gg/4c0ab513040cbb0475c3e9173b6697c01f5ba0640ccef34a8f7d7194db14373e.png', FALSE, 6, 20, 37, 44),
('b34d122ffd52f179a8ecb3d7f9997e3de14c256bc4d1fe1baa7c782d58b1b92f', 'イガク', 'イガク', '原口沙輔 feat.重音テト', 29, 'https://static.popn.gg/b34d122ffd52f179a8ecb3d7f9997e3de14c256bc4d1fe1baa7c782d58b1b92f.png', FALSE, 5, 17, 32, 41),
('fa620aafbba8226c347d1e382cd7d3b8d10ae245000958461547928f530f74db', 'いますぐ輪廻', 'いますぐ輪廻', 'なきそ', 29, 'https://static.popn.gg/fa620aafbba8226c347d1e382cd7d3b8d10ae245000958461547928f530f74db.png', FALSE, 10, 22, 36, 45),
('0d6dd50036d8a55f9db4a95949ece261b0a22836c27c6f6f689d1735a033fb2b', 'インフェルノ', 'インフェルノ', '♪♪♪♪♪', 29, 'https://static.popn.gg/0d6dd50036d8a55f9db4a95949ece261b0a22836c27c6f6f689d1735a033fb2b.png', FALSE, 7, 20, 33, 41),
('f1e6c2ba2088e164dade60931cd0252430539d2923d8fee5710df4d6a0ce0548', 'ニコニコサンフラワーキッス', '魚氷に上り　耀よひて', 'あさき', 29, 'https://static.popn.gg/f1e6c2ba2088e164dade60931cd0252430539d2923d8fee5710df4d6a0ce0548.png', FALSE, 10, 27, 36, 44),
('68a509af8d16aa06b7f39f1f8b51dd8f90990cf2ceff6e20078fef71a20d5c1b', 'うっせぇわ', 'うっせぇわ', 'Ado', 29, 'https://static.popn.gg/68a509af8d16aa06b7f39f1f8b51dd8f90990cf2ceff6e20078fef71a20d5c1b.png', FALSE, 7, 15, 33, 40),
('2b22a5e3f29fd10847ede900658f072e81d5e6e7bae3c549d634b874c0193f73', 'ウミユリ海底譚', 'ウミユリ海底譚', 'n-buna', 29, 'https://static.popn.gg/2b22a5e3f29fd10847ede900658f072e81d5e6e7bae3c549d634b874c0193f73.png', FALSE, 7, 24, 36, 45),
('209d7468f9f8a90880d95ebb739eb158bee55b092482505b02964f7e66abce8b', 'オーバーライド', 'オーバーライド', '吉田夜世 feat.重音テトSV', 29, 'https://static.popn.gg/209d7468f9f8a90880d95ebb739eb158bee55b092482505b02964f7e66abce8b.png', FALSE, 6, 20, 39, 45),
('66320ec4df42955861c6e92ec17344583538441e04a5bc8e1affd52675275a54', '踊', '踊', 'Ado', 29, 'https://static.popn.gg/66320ec4df42955861c6e92ec17344583538441e04a5bc8e1affd52675275a54.png', FALSE, 4, 10, 28, 40),
('e831d3fda842d0559f218c92ef7e2e22e3ba7f87ac6f35fc79e86f59e6c97f92', '怪獣', '怪獣', '♪♪♪♪♪', 29, 'https://static.popn.gg/e831d3fda842d0559f218c92ef7e2e22e3ba7f87ac6f35fc79e86f59e6c97f92.png', FALSE, 6, 21, 37, 44),
('c4236216777911da84736d8968b1060d03a67844c03d4df9a171a6423b2b9468', '怪獣の花唄', '怪獣の花唄', '♪♪♪♪♪', 29, 'https://static.popn.gg/c4236216777911da84736d8968b1060d03a67844c03d4df9a171a6423b2b9468.png', FALSE, 4, 19, 33, 40),
('3166cc58e9d423f998180e818485d96d79925921096b518dcd7f9843ed464c90', 'モダンジャパネスク', '幽世幻夜', 'TAN1CHU feat.妃那子', 29, 'https://static.popn.gg/3166cc58e9d423f998180e818485d96d79925921096b518dcd7f9843ed464c90.png', FALSE, 11, 27, 37, 45),
('e4656a41d4b55ccb0d8e3fce69c679946413637f42791f804e2e0613a3456e7a', 'オトギタンテイ', '華麗なる！音戯探偵ひなビタ♫', '音戯探偵ひなビタ♫', 29, 'https://static.popn.gg/e4656a41d4b55ccb0d8e3fce69c679946413637f42791f804e2e0613a3456e7a.png', FALSE, 9, 27, 38, 46),
('97fcabe7179f99cca7ec444cf38f563557524823229e37287b5c3c43279d5a34', 'かわいいだけじゃだめですか？', 'かわいいだけじゃだめですか？', 'CUTIE STREET', 29, 'https://static.popn.gg/97fcabe7179f99cca7ec444cf38f563557524823229e37287b5c3c43279d5a34.png', FALSE, 6, 16, 33, 41),
('47413af8d033ebda9809c044de3ad5c6d5fe34feae5349d4dfe048df5516ce43', '可愛くてごめん', '可愛くてごめん', 'Covered by ちょぴん', 29, 'https://static.popn.gg/47413af8d033ebda9809c044de3ad5c6d5fe34feae5349d4dfe048df5516ce43.png', FALSE, 4, 15, 33, 41),
('742d7890ad2724901ba03bdd4022896fe26ff181207c9c2979ef1ebb5da66dde', 'キミのこと、だいだいすきだもん☆', 'キミのこと、だいだいすきだもん☆', 'シナモロール', 29, 'https://static.popn.gg/742d7890ad2724901ba03bdd4022896fe26ff181207c9c2979ef1ebb5da66dde.png', FALSE, 5, 11, 19, 31),
('83cf033b607dd4024e51a0f4a3e8bc2eecae9cea2af102c1f97106d326dd9298', 'ラブリーモータウン２', 'キミのそばに', 'KE!JU feat. ねんね', 29, 'https://static.popn.gg/83cf033b607dd4024e51a0f4a3e8bc2eecae9cea2af102c1f97106d326dd9298.png', FALSE, 8, 25, 34, 42),
('0daec93535bf7194cda1aba09aa0a2002812c54c90401b93a30781918bb92580', 'コロコロック', '心転々', 'PHQUASE feat.紫崎 雪', 29, 'https://static.popn.gg/0daec93535bf7194cda1aba09aa0a2002812c54c90401b93a30781918bb92580.png', FALSE, 11, 27, 40, 46),
('993208d469b9f4825876cd6ec8c6beb2489761ca59d8563939f5e79cc4fee66b', 'ケルティックヴァース', '朔望', 'DJ TOTTO vs Cororo', 29, 'https://static.popn.gg/993208d469b9f4825876cd6ec8c6beb2489761ca59d8563939f5e79cc4fee66b.png', FALSE, 17, 30, 44, 48),
('aa7011652bb66cdea4e6f2d49accc73f8702f2c38a688cb86f14660611f1a4f6', 'トワ', 'さらさ', 'BEMANI Sound Team "PON"', 29, 'https://static.popn.gg/aa7011652bb66cdea4e6f2d49accc73f8702f2c38a688cb86f14660611f1a4f6.png', FALSE, 13, 28, 39, 46),
('0da1df82c41a2b71d9395623762b0552a37387be391665330dad6816a91f6bfe', '少女レイ', '少女レイ', 'みきとP', 29, 'https://static.popn.gg/0da1df82c41a2b71d9395623762b0552a37387be391665330dad6816a91f6bfe.png', FALSE, 5, 18, 33, 42),
('c21c15af77ca672b60dd2348e3cf0bf3010cc641297878f7e0c7616ca68b3891', '怪談スウィング', 'スペクター・チェイサー', '音戯探偵ひなビタ♫', 29, 'https://static.popn.gg/c21c15af77ca672b60dd2348e3cf0bf3010cc641297878f7e0c7616ca68b3891.png', FALSE, 8, 27, 35, 44),
('ff44793ca8c1bad4a4ece381e5fc942f8286c23d2c6a952dcedbda925a08def4', 'ダークナイト', 'その闇を薙いで', 'ELFENSJóN', 29, 'https://static.popn.gg/ff44793ca8c1bad4a4ece381e5fc942f8286c23d2c6a952dcedbda925a08def4.png', FALSE, 16, 31, 42, 48),
('296ebc438d9772d425b74869d5ec617b313fc440589eab1f1cdb9f4314c29db2', 'トラウマパンク', '大釈迦', '筋肉少女帯', 29, 'https://static.popn.gg/296ebc438d9772d425b74869d5ec617b313fc440589eab1f1cdb9f4314c29db2.png', FALSE, 10, 26, 34, 48),
('db291e94e7906f37e174e01ff5c50bb6df40d7c4dfc4538f5776b22877d900ba', 'ダイダイダイダイダイキライ', 'ダイダイダイダイダイキライ', '雨良 Amala', 29, 'https://static.popn.gg/db291e94e7906f37e174e01ff5c50bb6df40d7c4dfc4538f5776b22877d900ba.png', FALSE, 7, 18, 36, 43),
('d963b4092c683749e09c10bc8501126c956894c3a7e1db1016f91726e15dd018', '太陽系デスコ', '太陽系デスコ', 'ナユタン星人', 29, 'https://static.popn.gg/d963b4092c683749e09c10bc8501126c956894c3a7e1db1016f91726e15dd018.png', FALSE, 7, 18, 36, 44),
('b185ea72348aa1735e9fb783a7811ba480c72e6b67686081eaf16bb29acb1354', 'ジャッジメントシンフォニックスピードコア', '断罪のミメシス', 'KE!JU', 29, 'https://static.popn.gg/b185ea72348aa1735e9fb783a7811ba480c72e6b67686081eaf16bb29acb1354.png', FALSE, 14, 36, 43, 49),
('2992293451d0de312f08ab8e96f882fb98ed024669afa487faddaffd5f950ee0', 'デンパズルビート', 'ちくたく²ちく²ぱ', '音戯探偵ひなビタ♫', 29, 'https://static.popn.gg/2992293451d0de312f08ab8e96f882fb98ed024669afa487faddaffd5f950ee0.png', FALSE, 16, 31, 42, 48),
('2607e7f7668180c0d3cc710ad1d0a27cc6e7baddfbe9949709ddbd35614f60c9', '演説２', '告げてみことや かのもとに', 'あさき', 29, 'https://static.popn.gg/2607e7f7668180c0d3cc710ad1d0a27cc6e7baddfbe9949709ddbd35614f60c9.png', FALSE, 12, 31, 40, 47),
('f8d047589d359e32c0e6a34760815108343e173d3b7806b8a1b37edc951b853c', 'テトリス', 'テトリス', '柊マグネタイト feat.重音テト', 29, 'https://static.popn.gg/f8d047589d359e32c0e6a34760815108343e173d3b7806b8a1b37edc951b853c.png', FALSE, 4, 12, 34, 44),
('1e8aaa8d6f632f0b8ec00aec970ccf7b654390b6d6cce3bcd85f784c0a5418e7', 'TGIFフィーバー', 'トーキョーサマーナイト（華金Remix）', 'あまみ×ひなみ', 29, 'https://static.popn.gg/1e8aaa8d6f632f0b8ec00aec970ccf7b654390b6d6cce3bcd85f784c0a5418e7.png', FALSE, 10, 26, 39, 46),
('27bb10dc4190b6abb8f9eaabd5d0f77756031da1620bac2f5591d3f2222a7060', 'ニューギャルディスコ', '謎解き☆クイーン！', '音戯探偵ひなビタ♫', 29, 'https://static.popn.gg/27bb10dc4190b6abb8f9eaabd5d0f77756031da1620bac2f5591d3f2222a7060.png', FALSE, 9, 24, 34, 43),
('d1417a235eb0f4af3fb0e22f7bed1a9f18e4334cc5467b4d8cd1572a0fb77cc8', 'スウィートリドルポップ', 'なんてシュペール', '音戯探偵ひなビタ♫', 29, 'https://static.popn.gg/d1417a235eb0f4af3fb0e22f7bed1a9f18e4334cc5467b4d8cd1572a0fb77cc8.png', FALSE, 11, 24, 37, 45),
('f790c7c1d61a8ff8be8473b9e69f5acb50255196f53de9798cdb2eea6db20c48', '空想ファンタジーシリーズ', '眠りの国のステラ', 'BEMANI Sound Team "DJ TOTTO"', 29, 'https://static.popn.gg/f790c7c1d61a8ff8be8473b9e69f5acb50255196f53de9798cdb2eea6db20c48.png', FALSE, 14, 31, 40, 47),
('ea76b8ac5af3e03e062c60801d3de761afeaa24269b08b80c86eeebb4f5c707d', '倍倍FIGHT!', '倍倍FIGHT!', 'CANDY TUNE', 29, 'https://static.popn.gg/ea76b8ac5af3e03e062c60801d3de761afeaa24269b08b80c86eeebb4f5c707d.png', FALSE, 8, 17, 35, 44),
('19f7075179cc1689b0128dafbb2afe124332fd613331e4d8139e33107e9fe8c9', 'はいよろこんで', 'はいよろこんで', 'こっちのけんと', 29, 'https://static.popn.gg/19f7075179cc1689b0128dafbb2afe124332fd613331e4d8139e33107e9fe8c9.png', FALSE, 2, 14, 27, 38),
('06d48cdcd2d9e5dcf638d34498584496a3c2a3c95fac799e6f678d30cc05545c', 'ラストアーツ', '刃図羅', '猫叉Master vs HuΣeR', 29, 'https://static.popn.gg/06d48cdcd2d9e5dcf638d34498584496a3c2a3c95fac799e6f678d30cc05545c.png', FALSE, 13, 30, 41, 47),
('387cb616db9398f19a1053b6c7723251569aa456bcf1feed2a74d4b6a85f20ab', '初音ミクの消失', '初音ミクの消失', 'cosMo@暴走P', 29, 'https://static.popn.gg/387cb616db9398f19a1053b6c7723251569aa456bcf1feed2a74d4b6a85f20ab.png', FALSE, 13, 30, 43, 48),
('0e74f1c3d958a4db507bf279102390fc231d30dcda7e450eee38c45e9f5a8337', 'ハッピーラッキーチャッピー', 'ハッピーラッキーチャッピー', 'ano', 29, 'https://static.popn.gg/0e74f1c3d958a4db507bf279102390fc231d30dcda7e450eee38c45e9f5a8337.png', FALSE, 3, 11, 32, 39),
('5c0a0ddec16aee156438e33d95e689acf2ab25e2143b17cc9a04b75603818b8d', 'パプリカ', 'パプリカ', '♪♪♪♪♪', 29, 'https://static.popn.gg/5c0a0ddec16aee156438e33d95e689acf2ab25e2143b17cc9a04b75603818b8d.png', FALSE, 1, 10, 27, 37),
('3b2cdafd82177189a65d6c32a3dca3355ad56b4a6e9073b9510d977b0e7378c7', 'ひとりごつ～バンドVer.～', 'ひとりごつ～バンドVer.～', 'ハチワレ（CV:田中誠人）', 29, 'https://static.popn.gg/3b2cdafd82177189a65d6c32a3dca3355ad56b4a6e9073b9510d977b0e7378c7.png', FALSE, 2, 10, 25, 39),
('a22ceac65f9ed9392a6f6e65e0401bc60e4783b1410d6da70611d3f652b608ea', 'ハピコア', 'ビビッド ☆+*。キラキライム', 'Machico', 29, 'https://static.popn.gg/a22ceac65f9ed9392a6f6e65e0401bc60e4783b1410d6da70611d3f652b608ea.png', FALSE, 12, 27, 40, 46),
('fab01a3939aae9c2f4dac4915d418c65d1a90844f7c77675459d41ae878585ea', 'ビビデバ', 'ビビデバ', '星街すいせい', 29, 'https://static.popn.gg/fab01a3939aae9c2f4dac4915d418c65d1a90844f7c77675459d41ae878585ea.png', FALSE, 4, 16, 32, 41),
('a286365986a7b479d7328aea84e3387a874257d26f62321d89ce22430df8a81f', 'フォニイ', 'フォニイ', 'mami', 29, 'https://static.popn.gg/a286365986a7b479d7328aea84e3387a874257d26f62321d89ce22430df8a81f.png', FALSE, 6, 19, 33, 43),
('1cc9f519f08bfef05b8a2181ccaa3358c8a50aede37192df4acb8f4ce2d11154', 'コモーション', 'フラフラ', 'indoor cats.', 29, 'https://static.popn.gg/1cc9f519f08bfef05b8a2181ccaa3358c8a50aede37192df4acb8f4ce2d11154.png', FALSE, 7, 23, 36, 42),
('f42e25c966d5e6a22982223af0e70e0c2c86806a985612e6c94b270d121610af', 'プリンとマフィンのポムポムビート☆', 'プリンとマフィンのポムポムビート☆', 'ポムポムプリン', 29, 'https://static.popn.gg/f42e25c966d5e6a22982223af0e70e0c2c86806a985612e6c94b270d121610af.png', FALSE, 6, 14, 20, 32),
('b59e31a21ad4f6c7e64e096bada541ae5aa40b467119c2e8d4d3d758cb95875e', 'SNSミクスチャー', 'ブロックしよ♡', 'My Complex of Academy', 29, 'https://static.popn.gg/b59e31a21ad4f6c7e64e096bada541ae5aa40b467119c2e8d4d3d758cb95875e.png', FALSE, 12, 26, 37, 45),
('d8206ee7e8f5541bac27f0c6a63493ca63823b90919d85b59ff6090c935a5fdd', 'ニューミュージック', '僕の飛行機', 'covered by 不知火フレア', 29, 'https://static.popn.gg/d8206ee7e8f5541bac27f0c6a63493ca63823b90919d85b59ff6090c935a5fdd.png', FALSE, 9, 25, 36, 43),
('e24512e4bc79375a4e2d1bcfe6e8f436fb5947ac31956fdc966242314905bd81', 'マツケンサンバII', 'マツケンサンバII', '♪♪♪♪♪', 29, 'https://static.popn.gg/e24512e4bc79375a4e2d1bcfe6e8f436fb5947ac31956fdc966242314905bd81.png', FALSE, 3, 16, 31, 38),
('55802f6802c57f1b94664807fcde9822e8aeb6545437d0adada0a770e4608887', '名探偵コナン', '「名探偵コナン」 メイン・テーマ', '♪♪♪♪♪', 29, 'https://static.popn.gg/55802f6802c57f1b94664807fcde9822e8aeb6545437d0adada0a770e4608887.png', FALSE, 7, 19, 33, 38),
('be89c5e13424ae1f66b874a0e42ca53312a3f7e25b8196d9572faffbdb8aa8e7', 'モニタリング', 'モニタリング', 'DECO*27', 29, 'https://static.popn.gg/be89c5e13424ae1f66b874a0e42ca53312a3f7e25b8196d9572faffbdb8aa8e7.png', FALSE, 9, 21, 35, 43),
('9d2cf2601e5ce6938f333a562ec61a85fbcfa79d41ce14c7b59fb938cdbc5bee', 'ロシアンシンフォニー', '雪のラヴニーナ', '劇団レコード', 29, 'https://static.popn.gg/9d2cf2601e5ce6938f333a562ec61a85fbcfa79d41ce14c7b59fb938cdbc5bee.png', FALSE, 10, 27, 39, 45),
('440b5042c4368ea2fb7502c05cb3cf7d238bf377578d29f8494a2799fcc6008e', 'ガールズティーパーティー', 'レシピのリドル', '音戯探偵ひなビタ♫', 29, 'https://static.popn.gg/440b5042c4368ea2fb7502c05cb3cf7d238bf377578d29f8494a2799fcc6008e.png', FALSE, 9, 25, 36, 46),
('b775f9b493c0127e0812a21d4d4b1a66dd4f033849459131687f0a04bb514abb', 'わたしの一番かわいいところ', 'わたしの一番かわいいところ', 'FRUITS ZIPPER', 29, 'https://static.popn.gg/b775f9b493c0127e0812a21d4d4b1a66dd4f033849459131687f0a04bb514abb.png', FALSE, 4, 14, 27, 38),
('45dcc8ab3579db504bce0598be95b191434ac697ae71588640a6c15b76befa68', 'レヴェラチューン', 'Fate No.23', 'PON feat.秋成', 29, 'https://static.popn.gg/45dcc8ab3579db504bce0598be95b191434ac697ae71588640a6c15b76befa68.png', TRUE, 14, 32, 45, 49),
('382a3015d13fc4a3ddd330baca74fcd85e0b0807388effa2f1db7deaab6be256', 'ドリーミードラムン', 'Sprite Digital', 'Serph', 29, 'https://static.popn.gg/382a3015d13fc4a3ddd330baca74fcd85e0b0807388effa2f1db7deaab6be256.png', TRUE, 10, 28, 39, 47),
('d3e3e02304f48ed23f7f9bc674e0ff10e5f6afc39eaa4ed86f0fba5f64c701f5', 'ダークネス4', '終末の序曲～オワリノハジマリ～', 'フレディ波多江とエレハモニカ', 29, 'https://static.popn.gg/d3e3e02304f48ed23f7f9bc674e0ff10e5f6afc39eaa4ed86f0fba5f64c701f5.png', TRUE, 13, 26, 35, 46),
('5312c7f2503e43be0c077d9884e7b164f8ee0dde2bdbae533769e96610c4eba1', '人妖絵巻1', '人妖絵巻其の一「狐」～ 紅楼ノ夢 ～', '群青キネマ feat.YAMATO', 29, 'https://static.popn.gg/5312c7f2503e43be0c077d9884e7b164f8ee0dde2bdbae533769e96610c4eba1.png', TRUE, 11, 26, 38, 46),
('26822f656a55232252355337f0543b5c6b92f7f5f7e0a30d27ca40f51c577311', 'Ａ．Ｉ．デイトポップ', '隅田川夏恋歌', 'seiya-murai feat.ALT', 29, 'https://static.popn.gg/26822f656a55232252355337f0543b5c6b92f7f5f7e0a30d27ca40f51c577311.png', TRUE, 14, 26, 41, 45),
('c34b6a5ee946d1974ac38fae4c044e41c2cdaacad7a64a9c764808070713ca44', 'あさきのコラボロック', '透明はまだらに世界を告げて', 'あさき×剣', 29, 'https://static.popn.gg/c34b6a5ee946d1974ac38fae4c044e41c2cdaacad7a64a9c764808070713ca44.png', TRUE, 11, 31, 42, 47),
('47b558c89afad03339cb5ada5ea3d0cc11adc334802a808d10e796276417044b', 'ステッチ', 'モヘア', 'seiya-murai feat.藤野マナミ', 29, 'https://static.popn.gg/47b558c89afad03339cb5ada5ea3d0cc11adc334802a808d10e796276417044b.png', TRUE, 10, 25, 35, 44),
('df2cdad74a76fe7b5e634fcd25a9a08749ff4f7ee4c83d6d5155cd997aa77ae7', 'ドラムンコアダスト2', 'ラピストリアの約束', 'positive MAD-crew', 29, 'https://static.popn.gg/df2cdad74a76fe7b5e634fcd25a9a08749ff4f7ee4c83d6d5155cd997aa77ae7.png', TRUE, 12, 31, 42, 48);
CREATE TEMPORARY TABLE migration_assertions (ok TINYINT NOT NULL CHECK (ok = 1));
-- Existing hashes are allowed only when their metadata already equals this migration.
INSERT INTO migration_assertions
SELECT IF(COUNT(*) = 0, 1, 0) FROM new_song_catalog n JOIN songs s ON s.song_hash = n.song_hash
WHERE NOT (BINARY s.genre_name <=> BINARY n.genre_name)
   OR NOT (BINARY s.song_name <=> BINARY n.song_name)
   OR NOT (BINARY s.artist_name <=> BINARY n.artist_name)
   OR s.version <> n.version
   OR NOT (BINARY s.jacket_url <=> BINARY n.jacket_url);
INSERT INTO songs (song_hash, genre_name, song_name, artist_name, version, jacket_url, created_at, updated_at)
SELECT n.song_hash, n.genre_name, n.song_name, n.artist_name, n.version, n.jacket_url,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM new_song_catalog n
WHERE NOT EXISTS (SELECT 1 FROM songs s WHERE s.song_hash = n.song_hash);
-- Every staged hash must now resolve to exactly one song row.
INSERT INTO migration_assertions
SELECT IF(COUNT(*) = 0, 1, 0) FROM (
  SELECT n.song_hash FROM new_song_catalog n LEFT JOIN songs s ON s.song_hash = n.song_hash
  GROUP BY n.song_hash HAVING COUNT(s.song_id) <> 1
) invalid_hashes;
CREATE TEMPORARY TABLE new_chart_catalog (
  song_hash CHAR(64) NOT NULL, difficulty_code TINYINT NOT NULL,
  difficulty_label VARCHAR(16) NOT NULL, level TINYINT NOT NULL,
  chart_version INT NOT NULL, is_upper BOOLEAN NOT NULL,
  PRIMARY KEY (song_hash, difficulty_code)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO new_chart_catalog
SELECT n.song_hash, d.difficulty_code, d.difficulty_label,
       CASE d.difficulty_code WHEN 1 THEN n.light_level WHEN 2 THEN n.normal_level
            WHEN 3 THEN n.hyper_level WHEN 4 THEN n.ex_level END,
       n.version, n.is_upper
FROM new_song_catalog n
CROSS JOIN (
  SELECT 1 AS difficulty_code, 'LIGHT' AS difficulty_label
  UNION ALL SELECT 2, 'NORMAL' UNION ALL SELECT 3, 'HYPER' UNION ALL SELECT 4, 'EX'
) d
WHERE CASE d.difficulty_code WHEN 1 THEN n.light_level WHEN 2 THEN n.normal_level
      WHEN 3 THEN n.hyper_level WHEN 4 THEN n.ex_level END IS NOT NULL;
-- Existing chart keys must agree with the staged level and Upper type.
INSERT INTO migration_assertions
SELECT IF(COUNT(*) = 0, 1, 0)
FROM new_chart_catalog n JOIN songs s ON s.song_hash = n.song_hash
JOIN charts c ON c.song_id = s.song_id AND c.difficulty_code = n.difficulty_code
                 AND c.is_upper = n.is_upper
WHERE c.level <> n.level OR c.chart_version <> n.chart_version;
INSERT INTO charts (song_id, difficulty_code, difficulty_label, level, chart_version,
                    has_strict_judgement, has_strict_gauge, is_upper, is_deleted,
                    created_at, updated_at)
SELECT s.song_id, n.difficulty_code, n.difficulty_label, n.level, n.chart_version,
       FALSE, FALSE, n.is_upper, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM new_chart_catalog n JOIN songs s ON s.song_hash = n.song_hash
WHERE NOT EXISTS (
  SELECT 1 FROM charts c WHERE c.song_id = s.song_id
    AND c.difficulty_code = n.difficulty_code AND c.is_upper = n.is_upper
);
-- Verify all requested chart rows before committing.
INSERT INTO migration_assertions
SELECT IF(COUNT(*) = 0, 1, 0) FROM new_chart_catalog n
JOIN songs s ON s.song_hash = n.song_hash
LEFT JOIN charts c ON c.song_id = s.song_id AND c.difficulty_code = n.difficulty_code
                  AND c.is_upper = n.is_upper
WHERE c.chart_id IS NULL OR c.level <> n.level OR c.chart_version <> n.chart_version;
SELECT COUNT(*) AS staged_new_songs FROM new_song_catalog;
SELECT COUNT(*) AS staged_new_charts FROM new_chart_catalog;
COMMIT;
