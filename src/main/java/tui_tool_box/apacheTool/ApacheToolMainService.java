package apacheTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.util.Arrays;
import java.util.List;

public class ApacheToolMainService {

	public static void run(Screen screen) {
		try {
			// /etc/apache2 存在チェック
			if (!ApacheFileService.checkApacheDir()) {
				return; // メインメニューに戻る
			}

			List<String> menuItems = Arrays.asList("ports.conf port settings", "000-default.conf settings",
					"default-ssl.conf settings", "mods-enabled/dir.conf DirectoryIndex settings",
					"ApacheService Control", "end");
			int selected = 0;

			while (true) {
				screen.clear();
				TextGraphics tg = screen.newTextGraphics();
				TerminalSize size = screen.getTerminalSize();

				int boxWidth = 70;
				int boxHeight = menuItems.size() + 4;
				int startX = (size.getColumns() - boxWidth) / 2;
				int startY = (size.getRows() - boxHeight) / 2;

				drawBox(tg, startX, startY, boxWidth, boxHeight);
				tg.putString(startX + 2, startY + 1, "Apache Initial setting tool", SGR.BOLD);

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
				KeyStroke key = screen.readInput();

				if (key.getKeyType() == KeyType.ArrowDown) {
					selected = (selected + 1) % menuItems.size();
				} else if (key.getKeyType() == KeyType.ArrowUp) {
					selected = (selected - 1 + menuItems.size()) % menuItems.size();
				} else if (key.getKeyType() == KeyType.Enter) {
					switch (selected) {
					case 0:
						PortsConfService.run(screen);
						break;
					case 1:
						DefaultHttpConfService.run(screen);
						break;
					case 2:
						DefaultSslConfService.run(screen);
						break;
					case 3:
						DirConfService.run(screen);
						break;
					case 4:
						ApacheServiceControl.run(screen);
						break;
					case 5:
						return; // メインTUIに戻る
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void drawBox(TextGraphics tg, int x, int y, int width, int height) {
		tg.putString(x, y, "┌" + "─".repeat(width - 2) + "┐");
		for (int i = 1; i < height - 1; i++) {
			tg.putString(x, y + i, "│" + " ".repeat(width - 2) + "│");
		}
		tg.putString(x, y + height - 1, "└" + "─".repeat(width - 2) + "┘");
	}
}
