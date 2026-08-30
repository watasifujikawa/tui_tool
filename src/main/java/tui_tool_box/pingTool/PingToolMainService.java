package pingTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.util.Arrays;
import java.util.List;

/**
 * Pingツールのメインメニュー画面
 * 新規登録 / 疎通確認 / 終了（main_tuiに戻る）
 */
public class PingToolMainService {

    public static void run(Screen screen) {
        try {
            List<String> menuItems = Arrays.asList(
                    "New registration",
                    "Communication confirmation",
                    "end"
            );

            int selected = 0;

            while (true) {
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();
                TerminalSize size = screen.getTerminalSize();

                int width = 40;
                int height = menuItems.size() + 4;
                int startX = (size.getColumns() - width) / 2;
                int startY = (size.getRows() - height) / 2;

                // 枠
                drawBox(tg, startX, startY, width, height);

                tg.putString(startX + 2, startY + 1, "Ping tools menu", SGR.BOLD);

                // メニュー一覧
                for (int i = 0; i < menuItems.size(); i++) {
                    String prefix = (i == selected) ? "> " : "  ";

                    if (i == selected) {
                        tg.setForegroundColor(TextColor.ANSI.YELLOW);
                        tg.putString(startX + 2, startY + 2 + i, prefix + menuItems.get(i), SGR.BOLD);
                        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                    } else {
                        tg.putString(startX + 2, startY + 2 + i, prefix + menuItems.get(i));
                    }
                }

                screen.refresh();

                // キー入力
                KeyStroke key = screen.readInput();

                if (key.getKeyType() == KeyType.ArrowDown) {
                    selected = (selected + 1) % menuItems.size();

                } else if (key.getKeyType() == KeyType.ArrowUp) {
                    selected = (selected - 1 + menuItems.size()) % menuItems.size();

                } else if (key.getKeyType() == KeyType.Enter) {

                    // メニュー選択動作
                    switch (selected) {
                        case 0: // 新規登録
                            PingRegisterService.run(screen);
                            break;

                        case 1: // 疎通確認
                            PingExecuteService.run(screen);
                            break;

                        case 2: // 終了 → メインTUIに戻る
                            return;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
