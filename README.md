NEC BaaS Custom API for Enebular InfoMotion
===========================================

概要
----

株式会社 ウフル が提供する [Enebular](https://enebular.com) [InfoMotion](https://docs.enebular.com/ja/InfoMotion/Introduction.html) から NEC モバイルバックエンド基盤(NEC BaaS) に REST API でアクセスしてデータを取得するための CloudFunctions カスタム API です。
本 API を利用することで、 NEC BaaS に蓄積した IoT データなどの JSON オブジェクトデータを、 InfoMotion を使って可視化することができます。

以下にシステム構成図を示します。

<div align="center">
<img src="https://github.com/nec-baas/baas-infomotion/blob/master/img/baas-infomotion_%E3%82%B7%E3%82%B9%E3%83%86%E3%83%A0%E6%A7%8B%E6%88%90%E5%9B%B3.jpg?raw=true" width="80%" alt="システム構成図">
</div>

# カスタム API 登録
NEC BaaS から JSON オブジェクトデータを取得するカスタム API を登録します。
以降の説明でテナント、アプリケーション、ユーザの入力を行うため、事前に登録してください。
また、テナント情報の CORS 設定を有効にし、 CORS 許可ドメインに InfoMotion のドメインを追加してください。

## API 定義登録
本プロジェクトに格納されている API 定義を登録します。
api.yaml の内容をコピーし、デベロッパーコンソールの API Gateway から登録します。
Basic 認証を行う場合は、 x-acl の値を実行許可するユーザIDやグループ名に設定することを推奨します。以下に例を示します。

    x-acl:
       - 1234567890abcdefxxxxxxxx // 特定ユーザを指定する場合
       - g:infomotion // infomotion グループに所属するユーザを指定する場合
       - g:authenticated // 認証ユーザ全員を指定する場合

ACL の詳細は、 [NEC BaaS のマニュアル](https://nec-baas.github.io/baas-manual/latest/developer/ja/developer/functions/acl.html) をご参照ください。

## Function 定義登録
本プロジェクトに格納されている Function 定義を登録します。
function.yaml の内容をコピーし、デベロッパーコンソールの Cloud Functions > Functions から登録します。登録する際の名前は function.yaml の1行目に記載されている Function 名を入力してください。

## ユーザコード登録
ユーザコードは、コンパイル済みバイナリを GitHub より取得し、登録してください。

URL：https://github.com/nec-baas/baas-infomotion/releases

### ビルド
ユーザコードをソースからビルドすることも可能です。
以下のコマンドで実行します。

    $ mvn verify

ビルドが完了すると target フォルダ配下に 「infomotion-7.5.0-SNAPSHOT-cloudfn.tar.gz」 が作成されます。

### バケット作成
デベロッパーコンソールのファイルバケットからバケット名を CUSTOM_CODE としてファイルバケットを作成します。
ACL の設定は、バケット ACL 、コンテンツ ACL 共に全ての権限を空に設定してください。

### ファイル登録
ファイルバケット一覧から作成したファイルバケットを選択し、 infomotion-7.5.0-SNAPSHOT-cloudfn.tar.gz を登録してください。

# アクセス方法
Enebular InfoMotion からの NEC BaaS カスタムAPI の呼び出し方法について記述します。
カスタムAPI の呼び出しには以下の設定を行います。

* InfoType
* DataSource
* InfoMotion

## InfoType 設定
infomotion-tool を使用して作成、もしくは以下の URL から任意の InfoType を選択します。 ここでは URL にある Enebular が提供する linechart を使用して説明します。

URL：https://enebular.com/discover

## DataSource 設定
Enebular のプロジェクトダッシュボードから DataSource を追加します。

DataSource 設定画面では以下の項目が設定できます。下記項目に値を設定します。

|設定項目             |説明                                                                             |
|:--------------------|:--------------------------------------------------------------------------------|
|Title                |任意のタイトル                                                                   |
|DataSource Type      |NEC-BaaS                                                                         |
|Endpoint             |https://{APIサーバホスト名}/api/1/{テナント名}/api/{API名}/{サブパス}?{検索条件} |
|App Id               |NEC BaaS のアプリケーションID                                                     |
|App Key              |NEC BaaS のアプリケーションキー                                                   |
|Username (optional)  |NEC BaaS に登録済みのユーザ名                                                     |
|Password (optional)  |NEC BaaS に登録済みユーザのパスワード                                             |


* テナント名(ID)、アプリケーション ID、アプリケーションキーは NEC BaaS のデベロッパーコンソール上で確認してください。
* API 名は infomotion を指定してください。
* サブパスには /search/{バケット名} を指定してください。バケット名は使用するバケットを指定してください。
* 検索条件の設定は任意となります。オブジェクトクエリの検索条件と同フォーマットで where が指定できます。
* DataSource Type は NEC BaaS との連携を行うため、 NEC-BaaS を指定してください。

## InfoMotion 設定
Enebular のプロジェクトダッシュボードから InfoMotion を追加します。

追加した InfoMotion を開き、 Manage Graphs をクリックします。
右側にグラフを追加するためのメニューが表示されるので Create Graph をクリックします。


下記項目に値を設定し、 Add をクリックしてグラフを追加してください。

|設定項目               |説明                               |
|:----------------------|:----------------------------------|
|Name                   |任意の名前                         |
|Select InfoType        |登録した InfoType                   |
|Select Data DataSource |登録した DataSource                 |
|label                  |凡例                               |
|value                  |値のキー名                         |
|limit                  |表示件数                           |
