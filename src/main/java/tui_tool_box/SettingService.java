import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.Screen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import common.common_log;

public class SettingService {

	public static void run(Screen screen) {
		String message = "";
		try {
			List<String> jarList = loadJarList();
			jarList.add("exit");

			int selected = 0;

			while (true) {

				screen.clear();
				TextGraphics tg = screen.newTextGraphics();
				TerminalSize size = screen.getTerminalSize();

				int boxWidth = 60;
				int boxHeight = jarList.size() + 6;

				int startX = (size.getColumns() - boxWidth) / 2;
				int startY = (size.getRows() - boxHeight) / 2;

				drawBox(tg, startX, startY, boxWidth, boxHeight);
				tg.putString(startX + 2, startY + 1, "Select the JAR to run", SGR.BOLD);

				for (int i = 0; i < jarList.size(); i++) {
					String prefix = (i == selected) ? "> " : "  ";
					String text = prefix + jarList.get(i);

					if (i == selected) {
						tg.setForegroundColor(TextColor.ANSI.YELLOW);
						tg.putString(startX + 2, startY + 2 + i, text, SGR.BOLD);
						tg.setForegroundColor(TextColor.ANSI.DEFAULT);
					} else {
						tg.putString(startX + 2, startY + 2 + i, text);
					}
				}

				if (!message.isEmpty()) {
					tg.setForegroundColor(TextColor.ANSI.RED);
					tg.putString(startX + 2, startY + jarList.size() + 3, message);
					tg.setForegroundColor(TextColor.ANSI.DEFAULT);
				}

				screen.refresh();

				KeyStroke key = screen.readInput();

				if (key.getKeyType() == KeyType.ArrowDown) {
					selected = (selected + 1) % jarList.size();

				} else if (key.getKeyType() == KeyType.ArrowUp) {
					selected = (selected - 1 + jarList.size()) % jarList.size();

				} else if (key.getKeyType() == KeyType.Enter) {

					if (jarList.get(selected).equals("exit")) {
						return; // メニューに戻る
					}

					String jar = jarList.get(selected);
					File f = new File(jar);

					if (!f.exists()) {
						message = "JAR does not exist: " + jar;
					} else {
						// ★書き込み不要！メモリにセットするだけ
						ConfigService.setSelectedJar(jar);
						return; // ← メニューに戻る
					}
				}
			}

		} catch (Exception e) {
			common_log.warn("SettingService:" + message + " setting error");
		}
	}

	private static List<String> loadJarList() {
		List<String> list = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader("./config/config.csv"))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().isEmpty())
					list.add(line.trim());
			}
		} catch (Exception e) {
			common_log.warn("Error loading config.csv");
		}
		return list;
	}

	private static void drawBox(TextGraphics tg, int x, int y, int width, int height) {

		tg.putString(x, y, "┌" + "─".repeat(width - 2) + "┐");

		for (int i = 1; i < height - 1; i++) {
			tg.putString(x, y + i, "│" + " ".repeat(width - 2) + "│");
		}

		tg.putString(x, y + height - 1, "└" + "─".repeat(width - 2) + "┘");
	}
}
