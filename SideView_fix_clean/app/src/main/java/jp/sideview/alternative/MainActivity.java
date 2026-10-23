package jp.sideview.alternative;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.net.wifi.WifiManager;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private LinearLayout deviceList;
    private TextView status;
    private EditText ipInput, pskInput;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Device> devices = new ArrayList<>();
    private Device selected;

    static class Device {
        String name, location, address;
        Device(String n, String l, String a) { name=n; location=l; address=a; }
        public String toString() { return name + " (" + address + ")"; }
    }

    // Common Sony IRCC key codes used by many supported BRAVIA models.
    // Compatibility varies by model, firmware, and remote-control settings.
    private static final Map<String,String> IRCC = new HashMap<>();
    static {
        IRCC.put("Power", "AAAAAQAAAAEAAAAvAw==");
        IRCC.put("Input", "AAAAAQAAAAEAAAAlAw==");
        IRCC.put("Home", "AAAAAQAAAAEAAABgAw==");
        IRCC.put("Return", "AAAAAQAAAAEAAAB8Aw==");
        IRCC.put("Up", "AAAAAQAAAAEAAAB0Aw==");
        IRCC.put("Down", "AAAAAQAAAAEAAAB1Aw==");
        IRCC.put("Left", "AAAAAQAAAAEAAAA0Aw==");
        IRCC.put("Right", "AAAAAQAAAAEAAAAzAw==");
        IRCC.put("Confirm", "AAAAAQAAAAEAAABlAw==");
        IRCC.put("VolumeUp", "AAAAAQAAAAEAAAASAw==");
        IRCC.put("VolumeDown", "AAAAAQAAAAEAAAATAw==");
        IRCC.put("Mute", "AAAAAQAAAAEAAAAUAw==");
        IRCC.put("ChannelUp", "AAAAAQAAAAEAAAAQAw==");
        IRCC.put("ChannelDown", "AAAAAQAAAAEAAAARAw==");
    }

    @Override public void onCreate(Bundle saved) { super.onCreate(saved); buildUi(); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private TextView text(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size);
        t.setTextColor(Color.rgb(30,40,50)); t.setGravity(Gravity.CENTER); return t;
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setOnClickListener(v -> action.run()); return b;
    }
    private EditText field(String hint) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint);
        e.setTextSize(15); return e;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(16),dp(18),dp(20)); root.setBackgroundColor(Color.rgb(245,247,250));
        scroll.addView(root); setContentView(scroll);
        TextView title = text("SideView代替リモコン", 24); title.setTextColor(Color.rgb(21,32,43)); root.addView(title);
        root.addView(text("Sony BRAVIA向けネットワークリモコン試作版", 14));
        status = text("テレビとスマホを同じWi-Fiに接続してください。", 14);
        status.setPadding(0,dp(10),0,dp(10)); root.addView(status);

        root.addView(text("接続先テレビ", 17));
        ipInput = field("テレビのIPアドレス（例 192.168.1.20）"); root.addView(ipInput);
        pskInput = field("テレビ側で設定したPSK（未設定なら空欄）");
        pskInput.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(pskInput);
        root.addView(button("接続設定を確認", () -> {
            String ip = ipInput.getText().toString().trim();
            if (ip.isEmpty()) status.setText("テレビのIPアドレスを入力してください。");
            else status.setText("接続先: " + ip + "。テレビ側でIP制御・リモート操作を有効にしてください。");
        }));

        root.addView(button("機器を検索（SSDP）", this::scan));
        deviceList = new LinearLayout(this); deviceList.setOrientation(LinearLayout.VERTICAL); root.addView(deviceList);
        TextView section = text("リモコン", 20); section.setPadding(0,dp(14),0,dp(6)); root.addView(section);
        root.addView(button("電源", () -> send("Power")));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER);
        row.addView(button("入力切替", () -> send("Input")));
        row.addView(button("ホーム", () -> send("Home")));
        row.addView(button("戻る", () -> send("Return"))); root.addView(row);
        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.VERTICAL); nav.setGravity(Gravity.CENTER);
        nav.addView(button("▲", () -> send("Up")));
        LinearLayout mid = new LinearLayout(this); mid.setGravity(Gravity.CENTER);
        mid.addView(button("◀", () -> send("Left")));
        mid.addView(button("決定", () -> send("Confirm")));
        mid.addView(button("▶", () -> send("Right")));
        nav.addView(mid); nav.addView(button("▼", () -> send("Down"))); root.addView(nav);
        LinearLayout vol = new LinearLayout(this); vol.setGravity(Gravity.CENTER);
        vol.addView(button("音量 −", () -> send("VolumeDown")));
        vol.addView(button("音量 ＋", () -> send("VolumeUp")));
        vol.addView(button("消音", () -> send("Mute"))); root.addView(vol);
        LinearLayout ch = new LinearLayout(this); ch.setGravity(Gravity.CENTER);
        ch.addView(button("チャンネル −", () -> send("ChannelDown")));
        ch.addView(button("チャンネル ＋", () -> send("ChannelUp"))); root.addView(ch);
        TextView note = text("注意：対応するBRAVIAでIP制御が有効な場合に限り操作できます。テレビ側の設定・認証・機種により動作しない場合があります。レコーダー操作、番組表、録画予約は未実装です。", 12);
        note.setPadding(0,dp(16),0,0); root.addView(note);
    }

    private void scan() {
        status.setText("検索中です。最大約5秒かかります…");
        deviceList.removeAllViews(); devices.clear();
        executor.execute(() -> {
            List<Device> found = new ArrayList<>(); MulticastSocket socket = null;
            WifiManager.MulticastLock multicastLock = null;
            try {
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                if (wifi != null) {
                    multicastLock = wifi.createMulticastLock("SideViewAlternativeSsdp");
                    multicastLock.setReferenceCounted(false);
                    multicastLock.acquire();
                }
                socket = new MulticastSocket(); socket.setSoTimeout(350); socket.setTimeToLive(2);
                String msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n";
                byte[] data = msg.getBytes("UTF-8");
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("239.255.255.250"), 1900));
                long end = System.currentTimeMillis() + 5000; Set<String> seen = new HashSet<>();
                while (System.currentTimeMillis() < end) {
                    byte[] buf = new byte[8192]; DatagramPacket response = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(response);
                        String raw = new String(response.getData(), 0, response.getLength(), "UTF-8");
                        String lower = raw.toLowerCase(Locale.ROOT);
                        if (lower.contains("sony") || lower.contains("bravia") || lower.contains("bdp") || lower.contains("bdz")) {
                            String loc = header(raw, "location"); String addr = response.getAddress().getHostAddress();
                            if (seen.add(addr + "|" + loc)) found.add(new Device("Sony系機器候補", loc, addr));
                        }
                    } catch (SocketTimeoutException ignored) {}
                }
            } catch (Exception e) { runOnUiThread(() -> status.setText("検索エラー: " + e.getMessage())); }
            finally {
                if (socket != null) socket.close();
                if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
            }
            final List<Device> results = found;
            runOnUiThread(() -> {
                devices.addAll(results); deviceList.removeAllViews();
                if (results.isEmpty()) status.setText("機器が見つかりません。IPアドレスを直接入力して接続も試せます。");
                else status.setText(results.size() + "件の候補が見つかりました。タップするとIPを入力欄へ反映します。");
                for (Device d : results) deviceList.addView(button(d.toString(), () -> {
                    ipInput.setText(d.address); selected = d;
                    status.setText("接続先候補: " + d.address + "。テレビ側のIP制御設定を確認してください。");
                }));
            });
        });
    }
    private String header(String raw, String name) {
        for (String line : raw.split("\\r?\\n"))
            if (line.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT) + ":"))
                return line.substring(line.indexOf(':') + 1).trim();
        return "";
    }
    private void send(String key) {
        String ip = ipInput.getText().toString().trim();
        if (ip.isEmpty()) { status.setText("テレビのIPアドレスを入力するか、機器を検索してください。"); return; }
        String code = IRCC.get(key);
        if (code == null) { status.setText("未対応のボタンです: " + key); return; }
        String psk = pskInput.getText().toString();
        status.setText("送信中: " + key + " …");
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + "/sony/ircc");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); conn.setConnectTimeout(2500); conn.setReadTimeout(2500);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                conn.setRequestProperty("SOAPACTION", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"");
                if (!psk.isEmpty()) conn.setRequestProperty("X-Auth-PSK", psk);
                String xml = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:X_SendIRCC xmlns:u=\"urn:schemas-sony-com:service:IRCC:1\"><IRCCCode>" + code + "</IRCCCode></u:X_SendIRCC></s:Body></s:Envelope>";
                try (OutputStream os = conn.getOutputStream()) { os.write(xml.getBytes("UTF-8")); }
                int result = conn.getResponseCode();
                final String msg = result >= 200 && result < 300
                    ? "送信完了: " + key + "（HTTP " + result + "）"
                    : "送信失敗: HTTP " + result + "。IP制御設定・PSK・機種対応を確認してください。";
                runOnUiThread(() -> status.setText(msg));
            } catch (Exception e) {
                final String msg = "通信失敗: " + e.getMessage() + "。IPアドレス、同じWi-Fi、テレビ側のIP制御設定を確認してください。";
                runOnUiThread(() -> status.setText(msg));
            } finally { if (conn != null) conn.disconnect(); }
        });
    }
    @Override protected void onDestroy() { super.onDestroy(); executor.shutdownNow(); }
}
