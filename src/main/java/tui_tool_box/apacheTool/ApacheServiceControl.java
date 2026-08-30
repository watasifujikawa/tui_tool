package apacheTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ApacheServiceControl {

    public static void run(Screen screen) {
        try {

            String status = getApacheStatus();

            String[] menu = {
                    "Strart Apache",
                    "Stop Apache",
                    "restart Apache",
                    "return"
            };

            int selected = 0;

            while (true) {
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();
                int x = 4;
                int y = 2;

                tg.putString(x, y, "[Apache management]", SGR.BOLD);

                // ▼ Null を渡さずステータスを表示（他のソースと同じ方式）
                if (status.equals("active")) {
                    tg.putString(x, y + 2, "Current status: Starting up", SGR.BOLD);
                } else {
                    tg.putString(x, y + 2, "Current status: Stopped");
                }

                // ▼ メニュー描画
                for (int i = 0; i < menu.length; i++) {
                    if (i == selected) {
                        tg.setForegroundColor(TextColor.ANSI.YELLOW);
                        tg.putString(x, y + 4 + i, "> " + menu[i], SGR.BOLD);
                        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                    } else {
                        tg.putString(x, y + 4 + i, "  " + menu[i]);
                    }
                }

                screen.refresh();

                KeyStroke key = screen.readInput();

                if (key.getKeyType() == KeyType.ArrowDown) {
                    selected = (selected + 1) % menu.length;

                } else if (key.getKeyType() == KeyType.ArrowUp) {
                    selected = (selected - 1 + menu.length) % menu.length;

                } else if (key.getKeyType() == KeyType.Enter) {

                    switch (selected) {

                        case 0: // 起動
                            execSystemctl(screen, "start");
                            status = getApacheStatus();
                            break;

                        case 1: // 停止
                            execSystemctl(screen, "stop");
                            status = getApacheStatus();
                            break;

                        case 2: // 再起動
                            execSystemctl(screen, "restart");
                            status = getApacheStatus();
                            break;

                        case 3: // 戻る
                            return;
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------
    // 状態取得
    // ------------------------------
    private static String getApacheStatus() {
        try {
            ProcessBuilder pb = new ProcessBuilder("systemctl", "is-active", "apache2");
            Process p = pb.start();
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            String line = br.readLine();
            if (line == null) return "inactive";
            return line.trim();
        } catch (Exception e) {
            return "inactive";
        }
    }

    // ------------------------------
    // Apache 制御
    // ------------------------------
    private static void execSystemctl(Screen screen, String cmd) throws Exception {

        String command = "systemctl " + cmd + " apache2";

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        Process proc = pb.start();
        proc.waitFor();

        String status = getApacheStatus();

        showMessage(screen,
                "Apache " + msgOf(cmd) + "I did. (" +
                        (status.equals("active") ? "Starting up" : "Stop") + ")");
    }

    private static String msgOf(String cmd) {
        return switch (cmd) {
            case "start" -> "start";
            case "stop" -> "stop";
            case "restart" -> "restart";
            default -> cmd;
        };
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
