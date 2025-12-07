import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import common.common_log;

/**
 * config.csv を読み取り、jar の一覧を保持し、
 * 設定画面で選択された jar を保持するクラス。
 * CSV を書き換えることはしない。
 */
public class ConfigService {

    private static final String CONFIG_FILE = "./config/config.csv";

    // CSV 内の jar 一覧
    private static final List<String> jarList = new ArrayList<>();

    // ユーザーが選択した jar
    private static String selectedJar = null;
    /**
     * config.csv から jar のリストを読み込み
     */
    public static void loadConfig() {
        jarList.clear();
        File f = new File(CONFIG_FILE);
        if (!f.exists()) return;
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue;

                jarList.add(line);
            }
        } catch (Exception e) {
        common_log.warn(f+":"+line+" "+"File loading error");;
        }

        // 初期選択（既にあれば保持）
        if (selectedJar == null && !jarList.isEmpty()) {
            selectedJar = jarList.get(0);
        }
    }

    /**
     * jar 一覧を取得
     */
    public static List<String> getJarList() {
        return jarList;
    }

    /**
     * 選択された jar をセット（CSV には保存しない）
     */
    public static void setSelectedJar(String jar) {
        selectedJar = jar;
    }

    /**
     * 現在選択されている jar を取得
     */
    public static String getSelectedJar() {
        return selectedJar;
    }
}
