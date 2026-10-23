# Sony SideView代替（Androidスマホ向けソース）

**重要：これはインストール用APKではなく、ビルド前のソースプロジェクトです。** APK生成およびSony実機での通信試験は、この環境では完了していません。完成版として扱わないでください。

## 現状

実装済み（ソース上）
- 日本語のBRAVIA向けネットワークリモコン画面
- IPアドレス／PSK入力
- SSDPによるLAN内機器候補検索
- IRCC-IP/SOAPでの基本キー送信（対応機種・設定に依存）

未実装／未検証
- BDZレコーダーの操作
- 番組表、番組検索、録画予約、録画一覧、再生
- 全機種対応保証
- APKビルドと実機接続テスト

## ビルド条件

- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.7.3に対応するGradle
- インターネット接続（依存関係の取得）

このZIPにはGradle Wrapperが含まれていません。AndroidIDEが起動直後に終了する場合、CircleCIのログイン画面はこのプロジェクトのビルドに必須ではありません。GitHub Actionsを使う場合はGitHubアカウントとリポジトリへのアップロードが必要です。

## GitHub ActionsでAPKを作る場合

1. ZIPを展開し、`sideviewfix` フォルダー内のファイルをGitHubリポジトリのルートへアップロードします（フォルダー自体を二重に入れない）。
2. GitHubの **Actions** タブで **Build Android APK** を選び、**Run workflow** を実行します。
3. 成功した実行結果の **Artifacts** から `SonySideViewAlternative-debug-apk` をダウンロードします。
4. ZIPを展開し、`app-debug.apk` をAndroid端末に移してインストールします。

この方法はアカウントが必要です。アカウントなしでこのZIPだけからAPKを得られるわけではありません。

## 動作確認

スマホと対応BRAVIAを同じWi-Fiに接続し、テレビ側のIP制御／リモート操作を設定してから試します。PSKが必要な機種ではテレビに設定した値を入力します。HTTP通信のため、信頼できる自宅LAN内でのみ使用してください。機器検索に出ない場合はテレビのIPを直接入力します。

## 正直な進捗

画面と基本リモコン送信処理のソースはありますが、実機確認なしに「使える完成品」とは断言できません。特にBDZ録画機能は別途実装が必要です。
