# スマホからAPKを作る手順（GitHub Actions）

このプロジェクトは、GitHub Actions上でAndroid SDKとGradleを用意してAPKをビルドする設定です。こちらの作業環境からユーザーのGitHubリポジトリを直接操作する権限はないため、リポジトリへの反映と実行開始はGitHub画面で行う必要があります。

## A. プロジェクトをリポジトリへ反映する

1. GitHubで、SideView代替アプリ用に作ったリポジトリを開く。
2. ルートに `app`、`.github`、`tools`、`build.gradle`、`settings.gradle`、`gradle.properties`、`README.md` などが並んでいるか確認する。
3. 既にこれらがあるなら、ZIP全体を新しいサブフォルダとして追加しない。最新版のファイルをリポジトリのルートに反映してコミットする。
4. まだファイルがない場合は、このZIPを展開し、中身をルートにアップロードしてコミットする。ZIPファイルそのものだけをアップロードしてもビルドできない。

## B. APKをビルドする

1. リポジトリ上部の **Actions** を開く。
2. 左側の **Build Android APK** を選ぶ。
3. **Run workflow** → ブランチ `main`（なければ `master`）→ **Run workflow** を押す。
4. 実行中の項目が緑のチェックになるまで待つ。赤い×ならビルド失敗なので、実行を開き、最初に赤くなったステップのログを確認する。
5. 成功した実行を開き、下部の **Artifacts** にある `SonySideViewAlternative-debug-apk` をダウンロードする。
6. ダウンロードしたZIPを展開し、`app-debug.apk` をAndroidスマホで開いてインストールする。Androidが許可を求めた場合は、ダウンロードに使ったブラウザー／ファイルアプリの「不明なアプリのインストール」を許可してから再試行する。

## 大事な制限

- GitHub Actionsが緑になるまでは、APKが生成できたとは言えない。
- このAPKはdebug署名の開発版で、Google Play配布用ではない。
- インストールできても、実機BRAVIA／BDZの機種別動作確認が別途必要。
- 外出先ライブ視聴・録画視聴は、このソースでは未実装。ビルド成功だけでSideView同等になるわけではない。
- GitHubリポジトリのURLが分かれば、こちらで公開状態とワークフローの見え方を確認して、次の一手を絞れる。
