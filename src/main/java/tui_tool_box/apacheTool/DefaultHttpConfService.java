package apacheTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.Screen;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DefaultHttpConfService {

    private static final Path HTTP_CONF = Paths.get("/etc/apache2/sites-available/000-default.conf");
    private static final Path PORTS_CONF = Paths.get("/etc/apache2/ports.conf");

    public static void run(Screen screen) {
        try {
            if (!Files.exists(HTTP_CONF)) {
                showMessage(screen, HTTP_CONF + " does not exist.");
                return;
            }

            // ------------------------------
            // ports.confからHTTPポート取得
            // ------------------------------
            String httpPort = detectHttpPort();
            if (httpPort == null) httpPort = "80";

            // ------------------------------
            // 現在の DocumentRoot
            // ------------------------------
            String currentDocRoot = detectDocumentRoot();
            if (currentDocRoot == null) currentDocRoot = "/var/www/html";

            // 入力バッファ
            StringBuilder docRootBuf = new StringBuilder(currentDocRoot);

            List<String> menu = List.of("Edit DocumentRoot", "save and return");
            int selected = 0;

            while (true) {
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();
                int x = 4;
                int y = 2;

                tg.putString(x, y, "[000-default.conf Config]", SGR.BOLD);
                tg.putString(x, y + 2, "[Current settings]");
                tg.putString(x, y + 3, "VirtualHost : *:" + httpPort);
                tg.putString(x, y + 4, "DocumentRoot: " + currentDocRoot);

                // DocumentRoot 入力欄
                if (selected == 0)
                    tg.putString(x, y + 6, "> DocumentRoot: " + docRootBuf, SGR.BOLD);
                else
                    tg.putString(x, y + 6, "  DocumentRoot: " + docRootBuf);

                // 保存して戻る
                if (selected == 1)
                    tg.putString(x, y + 8, "> save and return", SGR.BOLD);
                else
                    tg.putString(x, y + 8, "  save and return");

                tg.putString(x, y + 10, "※ Changing this setting will change the public directory.", SGR.BOLD);

                screen.refresh();

                // キー取得
                KeyStroke key = screen.readInput();

                // ▼ 項目移動
                if (key.getKeyType() == KeyType.ArrowDown) {
                    selected = (selected + 1) % menu.size();
                    continue;
                }
                if (key.getKeyType() == KeyType.ArrowUp) {
                    selected = (selected - 1 + menu.size()) % menu.size();
                    continue;
                }

                // ▼ 保存
                if (selected == 1 && key.getKeyType() == KeyType.Enter) {
                    saveHttpConf(screen, httpPort, docRootBuf.toString());
                    return;
                }

                // ▼ DocumentRoot 入力
                if (selected == 0) {

                    if (key.getKeyType() == KeyType.Backspace) {
                        if (docRootBuf.length() > 0)
                            docRootBuf.deleteCharAt(docRootBuf.length() - 1);

                    } else if (key.getCharacter() != null) {
                        char c = key.getCharacter();
                        docRootBuf.append(c);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                showMessage(screen, "ERROR: " + e.getMessage());
            } catch (Exception ignore) {}
        }
    }

    // ----------------------------------------------------------
    // 保存処理（バックアップ仕様に完全対応）
    // ----------------------------------------------------------
    private static void saveHttpConf(Screen screen, String httpPort, String docRoot) throws Exception {

        ApacheFileService.backupForEdit(HTTP_CONF);

        List<String> lines = Files.readAllLines(HTTP_CONF, StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();

        for (String line : lines) {
            String trim = line.trim();

            // VirtualHost ポート変更
            if (trim.startsWith("<VirtualHost") && trim.contains("*:")) {
                line = "    <VirtualHost *:" + httpPort + ">";
            }

            // DocumentRoot 書き換え
            if (trim.startsWith("DocumentRoot")) {
                line = "    DocumentRoot " + docRoot;
            }

            out.add(line);
        }

        Files.write(HTTP_CONF, out, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        ApacheFileService.backupAfterEdit(HTTP_CONF);

        showMessage(screen, "000-default.conf has been updated.");
    }

    // ----------------------------------------------------------
    // ports.conf から HTTP ポート抽出
    // ----------------------------------------------------------
    private static String detectHttpPort() {
        try (BufferedReader br = Files.newBufferedReader(PORTS_CONF)) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (!t.startsWith("Listen")) continue;  // Listen 行
                if (t.contains("443")) continue;        // HTTPS 行は除外
                return extractPort(t);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractPort(String line) {
        line = line.replace("Listen", "").trim();
        if (line.contains(":")) return line.substring(line.lastIndexOf(':') + 1);
        return line;
    }

    // ----------------------------------------------------------
    // DocumentRoot 抽出
    // ----------------------------------------------------------
    private static String detectDocumentRoot() {
        try (BufferedReader br = Files.newBufferedReader(HTTP_CONF)) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("DocumentRoot"))
                    return t.replace("DocumentRoot", "").trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ----------------------------------------------------------
    // メッセージ画面
    // ----------------------------------------------------------
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
