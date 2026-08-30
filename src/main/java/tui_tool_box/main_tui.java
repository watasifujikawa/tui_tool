import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import apacheTool.ApacheToolMainService;
import pingTool.PingToolMainService;

import java.util.Arrays;
import java.util.List;

import common.common_log;

public class main_tui {

	public static void main(String[] args) {
		try {
			ConfigService.loadConfig();

			Screen screen = new DefaultTerminalFactory().createScreen();
			screen.startScreen();

			List<String> menuItems = Arrays.asList("start", "config", "PingTool", "ApacheTool", "exit");
			int selected = 0;

			while (true) {

				// メニュー再描画
				drawMenu(screen, menuItems, selected);
				KeyStroke key = screen.readInput();

				if (key.getKeyType() == KeyType.ArrowDown) {
					selected = (selected + 1) % menuItems.size();

				} else if (key.getKeyType() == KeyType.ArrowUp) {
					selected = (selected - 1 + menuItems.size()) % menuItems.size();

				} else if (key.getKeyType() == KeyType.Enter) {

					switch (selected) {

					case 0:
						// 開始 → Screen を止めない
						StartService.run(screen);
						break;

					case 1:
						// 設定画面に移動（Screen をそのまま渡す）
						SettingService.run(screen);
						break;

					case 2: // PingToolを呼びだす。
						PingToolMainService.run(screen);
						break;

					case 3: // ApacheToolを呼びだす。
						ApacheToolMainService.run(screen);
						break;

					case 4: // 終了時 ONLY stopScreen を呼ぶ
						screen.stopScreen();
						ExitService.run();
						return;

					}
				}
			}

		} catch (Exception e) {
			common_log.error("tui execution error");
		}
	}

	private static void drawMenu(Screen screen, List<String> menuItems, int selected) throws Exception {

		screen.clear();
		TextGraphics tg = screen.newTextGraphics();
		TerminalSize size = screen.getTerminalSize();

		int boxWidth = 30;
		int boxHeight = menuItems.size() + 4;

		int startX = (size.getColumns() - boxWidth) / 2;
		int startY = (size.getRows() - boxHeight) / 2;

		drawBox(tg, startX, startY, boxWidth, boxHeight);
		tg.putString(startX + 2, startY + 1, "menu", SGR.BOLD);

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
	}

	private static void drawBox(TextGraphics tg, int x, int y, int width, int height) {

		tg.putString(x, y, "┌" + "─".repeat(width - 2) + "┐");

		for (int i = 1; i < height - 1; i++) {
			tg.putString(x, y + i, "│" + " ".repeat(width - 2) + "│");
		}

		tg.putString(x, y + height - 1, "└" + "─".repeat(width - 2) + "┘");
	}
}