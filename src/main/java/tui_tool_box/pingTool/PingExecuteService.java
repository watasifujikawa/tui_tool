package pingTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class PingExecuteService {

    public static void run(Screen screen) {
        try {
            // CSV から読む（後で実装する PingCsvService を使用）
            List<String[]> entries = PingCsvService.loadAll();

            if (entries.isEmpty()) {
                showMessage(screen, "No IP data available. (Please register)");
                return;
            }

            int selected = 0;

            // ======== IP選択画面 ========
            while (true) {
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();
                TerminalSize size = screen.getTerminalSize();

                int boxWidth = 50;
                int boxHeight = entries.size() + 4;
                int startX = (size.getColumns() - boxWidth) / 2;
                int startY = (size.getRows() - boxHeight) / 2;

                drawBox(tg, startX, startY, boxWidth, boxHeight);
                tg.putString(startX + 2, startY + 1, "Communication Check - Select IP", SGR.BOLD);

                for (int i = 0; i < entries.size(); i++) {
                    String ip = entries.get(i)[0];
                    String desc = entries.get(i)[1];
                    String row = ip + "  (" + desc + ")";

                    String prefix = (i == selected) ? "> " : "  ";

                    if (i == selected) {
                        tg.putString(startX + 2, startY + 2 + i, prefix + row, SGR.BOLD);
                    } else {
                        tg.putString(startX + 2, startY + 2 + i, prefix + row);
                    }
                }

                screen.refresh();
                KeyStroke key = screen.readInput();

                if (key.getKeyType() == KeyType.ArrowDown) {
                    selected = (selected + 1) % entries.size();
                } else if (key.getKeyType() == KeyType.ArrowUp) {
                    selected = (selected - 1 + entries.size()) % entries.size();
                } else if (key.getKeyType() == KeyType.Enter) {
                    break;
                }
            }

            // 選択されたIP
            String targetIp = entries.get(selected)[0];

            // ======== ping 実行画面 ========
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.putString(2, 1, "Ping in progress: " + targetIp, SGR.BOLD);
            screen.refresh();

            // Linux用 ping（無限 ping の場合は -O、回数制限なら -c）
            ProcessBuilder pb = new ProcessBuilder("ping", "-O", targetIp);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            final int[] y = {3};
            TerminalSize size = screen.getTerminalSize();
            int maxRows = size.getRows();

            Thread readerThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        String line;
                        while ((line = br.readLine()) != null) {

                            synchronized (screen) {
                                tg.putString(2, y[0], line);
                                y[0]++;

                                if (y[0] >= maxRows - 2) {
                                    screen.clear();
                                    tg.putString(2, 1, "Ping in progress: " + targetIp, SGR.BOLD);
                                    y[0] = 3;
                                }

                                screen.refresh();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
            readerThread.start();

            // Enterで終了
            tg.putString(2, maxRows - 2, "[Enter] Stop");
            screen.refresh();

            while (true) {
                KeyStroke key = screen.readInput();
                if (key.getKeyType() == KeyType.Enter) {
                    proc.destroy();
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** 汎用メッセージ */
    private static void showMessage(Screen screen, String msg) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.putString(2, 2, msg, SGR.BOLD);
        tg.putString(2, 4, "Press Enter to return.");
        screen.refresh();

        while (true) {
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Enter) break;
        }
    }

    /** 枠描画 */
    private static void drawBox(TextGraphics tg, int x, int y, int width, int height) {
        tg.putString(x, y, "┌" + "─".repeat(width - 2) + "┐");

        for (int i = 1; i < height - 1; i++) {
            tg.putString(x, y + i, "│" + " ".repeat(width - 2) + "│");
        }

        tg.putString(x, y + height - 1, "└" + "─".repeat(width - 2) + "┘");
    }
}
