# hdf-tutorials-ja

- プロジェクトの概要
- Ambariをさわってみよう
  - HDFスタックの構成
  - HDFクラスタの管理概要  
- HDFを活用したシステムを作ってみよう
  - データの取込
    - NiFiでHTTP POSTを受信
    - NiFiでJSONから情報を抽出
    - Kafkaトピックの作成
    - NiFiからKafkaへメッセージ転送
  - データの公開
    - NiFiでKafkaトピックからメッセージを受信
    - NiFiでファイルシステムへ保存
  - リアルタイムストリーム分析
    - StormでKafkaからメッセージ受信
    - StormのリアルタイムWindow分析
    - Stormの分析結果をKafkaへと送信
- おまけ
  - ZeppelinからSparkを利用しJSONファイルをSQLで分析


## その他のチュートリアル
本プロジェクトのWikiページではHDF(Hortonworks Data Flow)チュートリアルの日本語訳も公開しています。
訳しながら順次更新していきます。

- [Apache NiFiの慣例を学ぶ](../../wiki/Learning-the-Ropes-of-Apache-NiFi) - 2016/09/21 訳完
  - 最も基本的なチュートリアルです。このチュートリアルの目的は、データフローの構築を通じてApache NiFiの機能に触れることです。
  このチュートリアルを完了するために、プログラミングの経験、フローベースのプログラミングシンタックスや機能の知識は必要ありません。

© 2011-2017 Hortonworks Inc. All Rights Reserved.
