package apacheTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DefaultSslConfService {

    private static final Path SSL_CONF  = Paths.get("/etc/apache2/sites-available/default-ssl.conf");
    private static final Path PORTS_CONF = Paths.get("/etc/apache2/ports.conf");

    public static void run(Screen screen) {
        try {
            if (!Files.exists(SSL_CONF)) {
                showMessage(screen, SSL_CONF + " does not exist.");
                return;
            }

            // ------------------------------
            // ports.conf から HTTPS ポート検出
            // ------------------------------
            String httpsPort = detectHttpsPort();
            if (httpsPort == null) httpsPort = "443";

            // ------------------------------
            // 現在設定読み込み
            // ------------------------------
            String currentDocRoot = detect("DocumentRoot");
            if (currentDocRoot == null) currentDocRoot = "/var/www/html";

            String currentCertFile = detect("SSLCertificateFile");
            if (currentCertFile == null) currentCertFile = "/etc/ssl/certs/ssl-cert-snakeoil.pem";

            String currentCertKey = detect("SSLCertificateKeyFile");
            if (currentCertKey == null) currentCertKey = "/etc/ssl/private/ssl-cert-snakeoil.key";

            // 入力バッファ（リアルタイム編集）
            StringBuilder docRootBuf  = new StringBuilder(currentDocRoot);
            StringBuilder certFileBuf = new StringBuilder(currentCertFile);
            StringBuilder certKeyBuf  = new StringBuilder(currentCertKey);

            List<String> menu = List.of(
                    "Edit DocumentRoot",
                    "Edit SSLCertificateFile",
                    "Edit SSLCertificateKeyFile",
                    "Press Enter to return."
            );

            int selected = 0;

            while (true) {

                // ------------------------------
                // 描画
                // ------------------------------
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();

                int x = 4;
                int y = 2;

                tg.putString(x, y, "[default-ssl.conf Config]", SGR.BOLD);

                tg.putString(x, y + 2, "[Current settings]");
                tg.putString(x, y + 3, "VirtualHost : *:" + httpsPort);
                tg.putString(x, y + 4, "DocumentRoot: " + currentDocRoot);
                tg.putString(x, y + 5, "SSLCertificateFile    : " + currentCertFile);
                tg.putString(x, y + 6, "SSLCertificateKeyFile : " + currentCertKey);

                // ---- 入力欄 ----
                if (selected == 0)
                    tg.putString(x, y + 8, "> DocumentRoot: " + docRootBuf, SGR.BOLD);
                else
                    tg.putString(x, y + 8, "  DocumentRoot: " + docRootBuf);

                if (selected == 1)
                    tg.putString(x, y + 10, "> SSLCertificateFile: " + certFileBuf, SGR.BOLD);
                else
                    tg.putString(x, y + 10, "  SSLCertificateFile: " + certFileBuf);

                if (selected == 2)
                    tg.putString(x, y + 12, "> SSLCertificateKeyFile: " + certKeyBuf, SGR.BOLD);
                else
                    tg.putString(x, y + 12, "  SSLCertificateKeyFile: " + certKeyBuf);

                if (selected == 3)
                    tg.putString(x, y + 14, "> Press Enter to return.", SGR.BOLD);
                else
                    tg.putString(x, y + 14, "  Press Enter to return.");

                tg.putString(x, y + 16,
                        "※ SSL/TLS This is the configuration file for the site.",
                        SGR.BOLD);

                screen.refresh();

                // ------------------------------
                // キー入力
                // ------------------------------
                KeyStroke key = screen.readInput();

                // 移動
                if (key.getKeyType() == KeyType.ArrowDown) {
                    selected = (selected + 1) % menu.size();
                    continue;
                }
                if (key.getKeyType() == KeyType.ArrowUp) {
                    selected = (selected - 1 + menu.size()) % menu.size();
                    continue;
                }

                // 保存
                if (key.getKeyType() == KeyType.Enter && selected == 3) {
                    saveSslConf(screen, httpsPort,
                            docRootBuf.toString(),
                            certFileBuf.toString(),
                            certKeyBuf.toString());
                    return;
                }

                // ---- 入力処理（選択されている行 only） ----
                if (key.getKeyType() == KeyType.Backspace) {
                    if (selected == 0 && docRootBuf.length() > 0)
                        docRootBuf.deleteCharAt(docRootBuf.length() - 1);
                    else if (selected == 1 && certFileBuf.length() > 0)
                        certFileBuf.deleteCharAt(certFileBuf.length() - 1);
                    else if (selected == 2 && certKeyBuf.length() > 0)
                        certKeyBuf.deleteCharAt(certKeyBuf.length() - 1);

                } else if (key.getCharacter() != null) {
                    char c = key.getCharacter();
                    if (selected == 0) docRootBuf.append(c);
                    else if (selected == 1) certFileBuf.append(c);
                    else if (selected == 2) certKeyBuf.append(c);
                }
            }

        } catch (Exception e) {
            try { showMessage(screen, "ERROR: " + e.getMessage()); }
            catch (Exception ignored) {}
        }
    }

    // ----------------------------------------------------------
    // 保存処理
    // ----------------------------------------------------------
    private static void saveSslConf(Screen screen,
                                    String httpsPort,
                                    String docRoot,
                                    String certFile,
                                    String certKey) throws Exception {

        ApacheFileService.backupForEdit(SSL_CONF);

        List<String> lines = Files.readAllLines(SSL_CONF, StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();

        for (String line : lines) {
            String t = line.trim();

            if (t.startsWith("<VirtualHost") && t.contains("_default_")) {
                line = "    <VirtualHost _default_:" + httpsPort + ">";
            }
            if (t.startsWith("DocumentRoot") || t.startsWith("#DocumentRoot")) {
                line = "    DocumentRoot " + docRoot;
            }
            if (t.startsWith("SSLCertificateFile") || t.startsWith("#SSLCertificateFile")) {
                line = "    SSLCertificateFile " + certFile;
            }
            if (t.startsWith("SSLCertificateKeyFile") || t.startsWith("#SSLCertificateKeyFile")) {
                line = "    SSLCertificateKeyFile " + certKey;
            }

            out.add(line);
        }

        Files.write(SSL_CONF, out, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        ApacheFileService.backupAfterEdit(SSL_CONF);

        showMessage(screen, "Updated default-ssl.conf.");
    }

    // ----------------------------------------------------------
    // 検出系
    // ----------------------------------------------------------
    private static String detectHttpsPort() {
        try (BufferedReader br = Files.newBufferedReader(PORTS_CONF)) {
            String l;
            while ((l = br.readLine()) != null) {
                String t = l.trim();
                if (t.startsWith("Listen") && t.contains("443")) {
                    return extractPort(t);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractPort(String line) {
        line = line.replace("Listen", "").trim();
        if (line.contains(":")) return line.substring(line.lastIndexOf(':') + 1);
        return line;
    }

    private static String detect(String key) {
        try (BufferedReader br = Files.newBufferedReader(SSL_CONF)) {
            String l;
            while ((l = br.readLine()) != null) {
                String t = l.trim();
                if (t.startsWith(key)) return t.replace(key, "").trim();
                if (t.startsWith("#" + key)) return t.replace("#" + key, "").trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void showMessage(Screen screen, String msg) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.putString(2, 2, msg, SGR.BOLD);
        tg.putString(2, 4, "Press Enter to return.");
        screen.refresh();

        while (true) {
            KeyStroke k = screen.readInput();
            if (k.getKeyType() == KeyType.Enter) break;
        }
    }
}
