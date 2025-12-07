package pingTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

public class PingRegisterService {

	public static void run(Screen screen) {
		try {
			StringBuilder ipInput = new StringBuilder();
			StringBuilder descInput = new StringBuilder();

			int mode = 0; // 0 = IP入力 / 1 = 説明入力

			while (true) {
				screen.clear();
				TextGraphics tg = screen.newTextGraphics();

				int x = 4;
				int y = 2;

				tg.putString(x, y, "[New registration]", SGR.BOLD);

				// --- IP 入力欄 ---
				tg.putString(x, y + 2, "Please enter your IP address:");

				String cursorPrefixIP = (mode == 0) ? "select > " : "        > ";
				String ipLine = cursorPrefixIP + ipInput.toString();

				if (mode == 0) {
					tg.putString(x, y + 3, ipLine, SGR.BOLD);
				} else {
					tg.putString(x, y + 3, ipLine);
				}

				// --- 説明 入力欄 ---
				tg.putString(x, y + 5, "Please enter a description:");

				String cursorPrefixDesc = (mode == 1) ? "select > " : "        > ";
				String descLine = cursorPrefixDesc + descInput.toString();

				if (mode == 1) {
					tg.putString(x, y + 6, descLine, SGR.BOLD);
				} else {
					tg.putString(x, y + 6, descLine);
				}

				screen.refresh();

				KeyStroke key = screen.readInput();

				if (key.getKeyType() == KeyType.Enter) {

					if (mode == 0) {
						mode = 1; // 説明入力へ
					} else {

						String ip = ipInput.toString().trim();
						String desc = descInput.toString().trim();

						// ★ 空白チェック（IP）
						if (ip.isEmpty()) {
							showMessage(screen, "The IP address is blank. Please enter it.");
							return;
						}

						// ★ 既存IP重複チェック
						if (PingCsvService.exists(ip)) {
							showMessage(screen, "This IP is already registered.");
							return;
						}

						// CSV保存
						boolean ok = PingCsvService.append(ip, desc);
						showMessage(screen, ok ? "I have registered." : "An error has occurred.");
						return;
					}
				} else if (key.getKeyType() == KeyType.Backspace) {
					if (mode == 0 && ipInput.length() > 0) {
						ipInput.deleteCharAt(ipInput.length() - 1);
					} else if (mode == 1 && descInput.length() > 0) {
						descInput.deleteCharAt(descInput.length() - 1);
					}
				} else if (key.getCharacter() != null) {
					char c = key.getCharacter();

					if (mode == 0)
						ipInput.append(c);
					else
						descInput.append(c);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void showMessage(Screen screen, String msg) throws Exception {

		screen.clear();
		TextGraphics tg = screen.newTextGraphics();

		tg.putString(2, 2, msg, SGR.BOLD);
		tg.putString(2, 4, "Press Enter to return.");

		screen.refresh();

		while (true) {
			KeyStroke key = screen.readInput();
			if (key.getKeyType() == KeyType.Enter)
				break;
		}
	}
}
