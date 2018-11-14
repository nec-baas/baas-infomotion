enebular InfoMotion向け JSONオブジェクトストレージ取得API
==========================================================================
enebular InfoMotion向けのJSONオブジェクトストレージ取得を行う
CloudFunctions カスタムAPIです。

# ビルド方法

下記のように操作することで、ユーザコードのtar.gzパッケージを作成できます

    $ mvn verify

target ディレクトリ以下に、下記のパッケージが作成されます。

    infomotion-7.5.0-SNAPSHOT-cloudfn.tar.gz

# モバイルバックエンド基盤サーバへのアップロード方法

nebula-cliを予めインストールしておいてください。

    $ cd infomotion
    $ nebula init-config

nebula_config.jsonが作成されるので、サーバ接続設定を追記してください。
以下のコマンドでユーザコードがサーバへアップロードされます。

    $ nebula create-api api.yaml
    $ nebula create-function function.yaml
    $ nebula create-code --file target/infomotion-7.5.0-SNAPSHOT-cloudfn.tar.gz

