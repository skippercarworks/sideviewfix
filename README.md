# Sony SideView代替アプリ（Android開発版）

Android 13以降を想定したJavaプロジェクトです。現状は **開発版であり、SideView完全互換版ではありません**。Android SDKを使ったAPKビルド・実機テストはまだ完了していません。

## 現在の機能（機種・設定依存）

- BRAVIA向けIPリモコン：電源、入力切替、ホーム、方向・決定、音量、チャンネル、数字、再生系キー
- テレビIP／PSK、BDZ IP／MACの端末内保存、接続テスト、LAN内Sony系機器のSSDP検索
- BDZの接続情報、番組表データ取得、録画予約一覧／作成／削除、録画タイトル一覧／保護設定／タイトル名変更／削除、レコーダーへの再生指示を試行
- BDZの録画予約一覧・録画済みタイトル一覧は300件単位で取得し、最大3,000件までページ取得。IDが空の機器応答では開始時刻・タイトル・チャンネルを使って重複を抑制
- DLNA ContentDirectory対応機器のメディア一覧取得（200件単位でページ取得、最大2,000件）と、Androidが再生できる形式のLAN内再生を試行
- GガイドWebサイト表示（アプリ内の戻る・更新ボタン、読み込み進捗、通信エラー表示、危険なURLスキームのブロック）

## 未完了・制限

- SonyはVideo & TV SideViewのサービス終了を **2027年3月30日** と案内しています。外出先視聴は対応BDZ、登録、ネットワーク、著作権保護などの条件に依存するため、ローカルLAN用の非公式アプリだけで同じクラウド機能を再現できるとは確認できていません。

- **外出先からのライブ視聴／録画視聴は未実装です。** BDZのリモート視聴には機種・登録・著作権保護を含む仕組みがあり、単純なHTTP動画再生では置き換えられません。
- Sony SideViewの画面・番組表・すべての機能を完全再現していません。
- BDZのLAN APIは非公式の観察仕様を使っており、機種・ファームウェアごとの実機確認が必要です。未確認の操作は成功を保証しません。
- 番組表バイナリの解析は合成データと決定的な不正データ20,000件の耐性テストで基本確認済みですが、実機から取得した全機種のデータで検証したものではありません。
- BDZの電源OFF時WOLには、ユーザーが正しいMACアドレスを入力する必要があります。機器のUUIDからMACを推測しません。
- 録画削除は取り消せません。アプリ内で削除前の確認を行います。

## オフライン検査（2026-10-24）

JDK 17以降で以下を実行すると、BDZ番組表パーサー／予約ページング29件、DLNA XML／URL安全性・ページング19件、ホスト検証19件、番組表URLポリシー19件の合計86件の合成データテストを実行できます。今回の作業環境でも `javac -Xlint:all -Werror` でコンパイルし、4つのオフラインテストが合格しました。BDZの予約・録画済み一覧はTotalMatches/NumberReturnedが応答に含まれる場合、短いページでも残件があれば続行します。総件数不明時は短いページで終了します。DLNAのTotalMatchesを返すサーバーが要求件数より少ないページを返しても、残件がある場合は次ページへ進むように修正しました。これは実機の通信・再生テストではありません。

```sh
mkdir -p /tmp/bdz-smoke
javac -Xlint:all -Werror -d /tmp/bdz-smoke app/src/main/java/jp/sideview/alternative/BdzClient.java app/src/main/java/jp/sideview/alternative/DlnaBrowser.java app/src/main/java/jp/sideview/alternative/NetworkAddress.java app/src/main/java/jp/sideview/alternative/GuideUrlPolicy.java tools/BdzClientSmokeTest.java tools/DlnaBrowserSmokeTest.java tools/NetworkAddressSmokeTest.java tools/GuideUrlPolicySmokeTest.java
java -cp /tmp/bdz-smoke jp.sideview.alternative.BdzClientSmokeTest
java -cp /tmp/bdz-smoke jp.sideview.alternative.DlnaBrowserSmokeTest
java -cp /tmp/bdz-smoke jp.sideview.alternative.NetworkAddressSmokeTest
java -cp /tmp/bdz-smoke jp.sideview.alternative.GuideUrlPolicySmokeTest
```

## APKビルド

GitHub Actionsの `Build Android APK` ワークフローで `assembleDebug` を実行し、`SonySideViewAlternative-debug-apk` という成果物を保存する設定です。ビルド後の署名検査はAndroid SDK Build Tools内の `apksigner` をフルパスで呼び出します。ローカルビルドにはJDK 17、Android SDK Platform 35、Build Tools 35.0.0、Gradle 8.9が必要です。

この作業環境にはAndroid SDK／Gradleがなく、ネットワークから取得もできないため、ここではAPK生成・インストール検証を完了できていません。


### DLNA録画の全画面再生（v0.2.0）

DLNAサーバーが再生URLを公開している録画は、専用の横向き全画面プレーヤーで開きます。再生・一時停止・シークはAndroidのメディアコントローラーを使用し、読み込み中表示、再生エラー表示、画面スリープ防止を備えています。機器が公開する形式や著作権保護によっては再生できません。

この変更はソース上の実装です。Android SDK/Gradleがこの実行環境にないため、APKビルドと実機再生は未検証です。

### 番組表画面の操作改善（v0.3.0）

番組表WebViewにページ戻り・更新・進捗表示・メインフレーム通信エラー表示を追加しました。HTTP/HTTPS以外のURLスキームへの遷移は拒否します。これは番組表サイトの表示改善であり、BDZ番組表のネイティブEPG統合や録画予約との自動連携を意味しません。Android SDK／GradleがないためAPKビルドと実機確認は未実施です。


### v0.4.3 追加変更
- テレビ／BDZのホスト名入力を厳格に検証し、URL・ポート・改行などの混入を拒否します。
- テレビへのPSKヘッダーを含むHTTPリクエストはリダイレクトを追跡しません。
- PSKに改行が含まれる場合や256文字を超える場合は送信前に拒否します。
- ホスト入力検証のオフライン回帰テストを追加しました。


### v0.4.4 番組表WebViewのナビゲーション制御
- アプリ内表示はHTTPSの `bangumi.org` ドメインに限定します。
- 外部のHTTP/HTTPSリンクは外部ブラウザーへ渡し、`javascript:`, `file:` などのスキームは拒否します。
- URLポリシーの19ケースをオフライン検査に追加しました。
- この変更はWebViewの遷移制御です。Android SDKがないためAPKビルド・端末実機テストは未実施です。


### v0.4.5 CIビルド検証の強化
- GitHub Actionsで番組表URLポリシーの19ケースも実行するよう修正しました。READMEに記載していたテストがCIで抜けていた問題を解消します。
- Androidのdebug APKを生成した後、ファイルが空でないこととAPK署名検証を実行し、SHA-256チェックサムも成果物に含めます。
- 成果物の保存期間を14日に設定しました。
- ここでの変更はCI定義とバージョン情報です。GitHub Actions上でワークフローを実行した結果はまだ取得していません。


### v0.4.9 番組表の予約可能時間チェック
- 検索語が入っている場合でも、放送終了済み番組を録画予約候補から除外します。
- 予約確認時と予約送信直前にも終了時刻を再検証し、番組表を開いたまま時間が経過した場合の誤予約を防ぎます。
- 判定は日本時間（Asia/Tokyo）で行い、欠損・不正な終了時刻は予約不可として扱います。
- オフライン回帰テストを4件追加しました。これは実機BDZ通信・APKビルドの確認ではありません。
