import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.io.*;
import java.nio.charset.StandardCharsets;

import common.common_log;

public class StartService {

	public static void run(Screen screen) {
		try {
			// ★ 修正：設定画面で選択されたJARを取得
			String jar = ConfigService.getSelectedJar();

			if (jar == null || jar.isEmpty()) {
				showMessage(screen, "Error: No JAR selected.\nPlease open Settings.");
				return;
			}

			ProcessBuilder pb = new ProcessBuilder("java", "-jar", jar);
			pb.redirectErrorStream(true);
			Process proc = pb.start();

			BufferedReader fromJar = new BufferedReader(
					new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
			BufferedWriter toJar = new BufferedWriter(
					new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8));

			screen.clear();
			TextGraphics tg = screen.newTextGraphics();
			TerminalSize size = screen.getTerminalSize();

			final int maxRows = size.getRows();
			final int[] y = { 2 };

			tg.putString(2, 1, "=== Running: " + jar + " ===", SGR.BOLD);
			screen.refresh();

			// --- 外部JAR → TUI ---
			Thread readerThread = new Thread(() -> {
				try {
					String line;
					while ((line = fromJar.readLine()) != null) {
						synchronized (screen) {
							tg.putString(2, y[0], line);
							y[0]++;

							if (y[0] >= maxRows - 2) {
								screen.clear();
								tg.putString(2, 1, "=== Running: " + jar + " ===", SGR.BOLD);
								y[0] = 2;
							}
							screen.refresh();
						}
					}
				} catch (Exception ignored) {
				}
			});

			readerThread.start();

			// --- TUI → 外部JAR ---
			while (true) {
				KeyStroke key = screen.readInput();

				if (!proc.isAlive())
					break;

				if (key.getKeyType() == KeyType.Escape) {
					proc.destroy();
					break;
				}

				if (key.getKeyType() == KeyType.Enter) {
					toJar.write("\n");
					toJar.flush();
				} else if (key.getCharacter() != null) {
					toJar.write(key.getCharacter());
					toJar.flush();
				}
			}

			proc.waitFor();

			tg.putString(2, y[0] + 1, "=== Finished (Press Enter to return) ===");
			screen.refresh();

			while (true) {
				KeyStroke key = screen.readInput();
				if (key.getKeyType() == KeyType.Enter)
					break;
			}

		} catch (Exception e) {
			common_log.warn("StartService: Errors during or after jar acquisition");
		}
	}

	private static void showMessage(Screen screen, String msg) throws Exception {
		screen.clear();
		TextGraphics tg = screen.newTextGraphics();
		tg.putString(2, 2, msg, SGR.BOLD);
		tg.putString(2, 4, "Press Enter to return");
		screen.refresh();

		while (true) {
			KeyStroke key = screen.readInput();
			if (key.getKeyType() == KeyType.Enter)
				break;
		}
	}
}
