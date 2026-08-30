package apacheTool;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.Screen;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DirConfService {

    private static final Path DIR_CONF = Paths.get("/etc/apache2/mods-enabled/dir.conf");

    public static void run(Screen screen) {
        try {
            if (!Files.exists(DIR_CONF)) {
                showMessage(screen, DIR_CONF + " does not exist.");
                return;
            }

            String currentIndexLine = detectDirectoryIndex();
            if (currentIndexLine == null)
                currentIndexLine = "DirectoryIndex index.html";

            List<String> wrappedIndex = wrapLines(currentIndexLine, screen);

            StringBuilder addBuf = new StringBuilder();

            List<String> menu = List.of("Enter additional file names", "save and return");
            int selected = 0;

            while (true) {
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();
                TerminalSize size = screen.getTerminalSize();

                int x = 4;
                int y = 2;

                tg.putString(x, y, "[dir.conf Config]", SGR.BOLD);
                tg.putString(x, y + 2, "[current DirectoryIndex]");

                int lineY = y + 3;
                for (String s : wrappedIndex) {
                    tg.putString(x, lineY, s);
                    lineY++;
                }

                // ---------- 入力欄 ----------
                if (selected == 0)
                    tg.putString(x, lineY + 1, "> additional file name: " + addBuf, SGR.BOLD);
                else
                    tg.putString(x, lineY + 1, "  additional file name: " + addBuf);

                // ---------- 保存 ----------
                if (selected == 1)
                    tg.putString(x, lineY + 3, "> Press Enter to return.", SGR.BOLD);
                else
                    tg.putString(x, lineY + 3, "  Press Enter to return.");

                tg.putString(x, lineY + 5,
                        "※ DirectoryIndex: The file name to load first.",
                        SGR.BOLD);

                screen.refresh();

                KeyStroke key = screen.readInput();

                // カーソル移動
                if (key.getKeyType() == KeyType.ArrowDown) {
                    selected = (selected + 1) % menu.size();
                    continue;
                }
                if (key.getKeyType() == KeyType.ArrowUp) {
                    selected = (selected - 1 + menu.size()) % menu.size();
                    continue;
                }

                // 保存
                if (selected == 1 && key.getKeyType() == KeyType.Enter) {
                    saveDirConf(screen, currentIndexLine, addBuf.toString());
                    return;
                }

                // 入力欄選択時のみ文字入力
                if (selected == 0) {
                    if (key.getKeyType() == KeyType.Backspace) {
                        if (addBuf.length() > 0)
                            addBuf.deleteCharAt(addBuf.length() - 1);
                    } else if (key.getCharacter() != null) {
                        char c = key.getCharacter();
                        if (c != ' ') {         // 半角スペース禁止
                            addBuf.append(c);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                showMessage(screen, "エラー: " + e.getMessage());
            } catch (Exception ignore) {}
        }
    }

    // ----------- 折り返し（絶対に止まらない版）-----------
    private static List<String> wrapLines(String text, Screen screen) {
        int maxWidth = screen.getTerminalSize().getColumns() - 8;
        List<String> result = new ArrayList<>();

        int idx = 0;
        while (idx < text.length()) {
            int end = Math.min(idx + maxWidth, text.length());
            result.add(text.substring(idx, end));
            idx = end;
        }
        return result;
    }

    // ----------- 保存処理 -----------
    private static void saveDirConf(Screen screen, String currentIndex, String add) throws Exception {

        if (add == null || add.isEmpty()) {
            showMessage(screen, "Enter the name of the file to add.");
            return;
        }

        ApacheFileService.backupForEdit(DIR_CONF);

        List<String> lines = Files.readAllLines(DIR_CONF, StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();

        boolean replaced = false;

        for (String line : lines) {
            String trim = line.trim();

            if (trim.startsWith("DirectoryIndex")) {
                String base = trim.replace("DirectoryIndex", "").trim();
                line = "    DirectoryIndex " + base + " " + add;
                replaced = true;
            }
            out.add(line);
        }

        if (!replaced) {
            out.add("    DirectoryIndex " + add);
        }

        Files.write(DIR_CONF, out, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        ApacheFileService.backupAfterEdit(DIR_CONF);

        showMessage(screen, "dir.conf has been updated.");
    }

    // ----------- DirectoryIndex 検出 -----------
    private static String detectDirectoryIndex() {
        try (BufferedReader br = Files.newBufferedReader(DIR_CONF)) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("DirectoryIndex")) return t;
            }
        } catch (Exception ignore) {}
        return null;
    }

    // ----------- メッセージ -----------
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
