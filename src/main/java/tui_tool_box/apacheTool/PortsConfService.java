package apacheTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.Screen;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class PortsConfService {

    private static final Path PORTS_CONF = Paths.get("/etc/apache2/ports.conf");

    public static void run(Screen screen) {
        try {

            if (!Files.exists(PORTS_CONF)) {
                showMessage(screen, PORTS_CONF + " does not exist.");
                return;
            }

            // 現在のポート
            String currentHttp  = detectHttpPort();
            String currentHttps = detectHttpsPort();
            if (currentHttp == null) currentHttp = "80";
            if (currentHttps == null) currentHttps = "443";

            // 編集バッファ
            StringBuilder httpBuf  = new StringBuilder(currentHttp);
            StringBuilder httpsBuf = new StringBuilder(currentHttps);

            List<String> items = List.of("HTTP port", "HTTPS port", "Save settings and return");
            int selected = 0;

            while (true) {

                // ---- 描画 ----
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();

                int x = 4;
                int y = 2;

                tg.putString(x, y, "【ports.conf Config】", SGR.BOLD);

                tg.putString(x, y + 2, "[Current settings]");
                tg.putString(x, y + 3, "HTTP : " + currentHttp);
                tg.putString(x, y + 4, "HTTPS: " + currentHttps);

                // HTTP
                if (selected == 0) {
                    tg.setForegroundColor(TextColor.ANSI.YELLOW);
                    tg.putString(x, y + 6, "> HTTP port: " + httpBuf.toString(), SGR.BOLD);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                } else {
                    tg.putString(x, y + 6, "  HTTP port: " + httpBuf.toString());
                }

                // HTTPS
                if (selected == 1) {
                    tg.setForegroundColor(TextColor.ANSI.YELLOW);
                    tg.putString(x, y + 8, "> HTTPS port: " + httpsBuf.toString(), SGR.BOLD);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                } else {
                    tg.putString(x, y + 8, "  HTTPS port: " + httpsBuf.toString());
                }

                // 保存
                if (selected == 2) {
                    tg.setForegroundColor(TextColor.ANSI.YELLOW);
                    tg.putString(x, y + 10, "> Save settings and return", SGR.BOLD);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                } else {
                    tg.putString(x, y + 10, " Save settings and return");
                }

                screen.refresh();

                // ---- 入力 ----
                KeyStroke key = screen.readInput();

                if (key.getKeyType() == KeyType.ArrowDown) {
                    selected = (selected + 1) % items.size();

                } else if (key.getKeyType() == KeyType.ArrowUp) {
                    selected = (selected - 1 + items.size()) % items.size();

                } else if (key.getKeyType() == KeyType.Enter) {

                    if (selected == 2) {
                        savePorts(screen, httpBuf.toString(), httpsBuf.toString());
                        return;
                    }
                }

                // -------- 入力反映（数字のみ） --------
                else if (key.getKeyType() == KeyType.Backspace) {
                    StringBuilder target = (selected == 0) ? httpBuf : httpsBuf;
                    if (target.length() > 0) {
                        target.deleteCharAt(target.length() - 1);
                    }

                } else if (key.getCharacter() != null) {
                    char c = key.getCharacter();

                    if (Character.isDigit(c)) {
                        if (selected == 0) httpBuf.append(c);
                        if (selected == 1) httpsBuf.append(c);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            try { showMessage(screen, "ERROR: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }

    // --------------------------------------------------------
    // ports.conf 保存処理
    // --------------------------------------------------------
    private static void savePorts(Screen screen, String httpPort, String httpsPort) throws Exception {

        if (!httpPort.matches("\\d+") || !httpsPort.matches("\\d+")) {
            showMessage(screen, "The port number is numeric only.");
            return;
        }

        ApacheFileService.backupForEdit(PORTS_CONF);

        List<String> lines = Files.readAllLines(PORTS_CONF, StandardCharsets.UTF_8);
        List<String> newLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("Listen") && trimmed.contains("443")) {
                line = "Listen " + httpsPort;

            } else if (trimmed.startsWith("Listen")) {
                if (!trimmed.contains("443")) {
                    line = "Listen " + httpPort;
                }
            }
            newLines.add(line);
        }

        Files.write(PORTS_CONF, newLines, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        ApacheFileService.backupAfterEdit(PORTS_CONF);

        showMessage(screen, "ports.conf has been updated.");
    }

    // --------------------------------------------------------
    // ポート検出
    // --------------------------------------------------------
    private static String detectHttpPort() {
        try (BufferedReader br = Files.newBufferedReader(PORTS_CONF)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("Listen")) continue;
                if (line.contains("443")) continue;
                return extractPort(line);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String detectHttpsPort() {
        try (BufferedReader br = Files.newBufferedReader(PORTS_CONF)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("Listen")) continue;
                if (!line.contains("443")) continue;
                return extractPort(line);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractPort(String line) {
        line = line.replace("Listen", "").trim();
        if (line.contains(":")) return line.substring(line.lastIndexOf(':') + 1);
        return line;
    }

    // --------------------------------------------------------
    // メッセージ画面
    // --------------------------------------------------------
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
