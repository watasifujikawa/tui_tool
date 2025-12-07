package pingTool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PingCsvService {

    private static final String CSV_FILE = "./config/ip_tool_configfile.csv";

    /** CSV に 1 行追加 */
    public static boolean append(String ip, String desc) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE, true))) {
            pw.println(ip + "," + desc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** CSV 全読み込み（必要に応じて使用） */
    public static List<String[]> loadAll() {
        List<String[]> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", 2);
                list.add(cols);
            }
        } catch (Exception e) {
            // ファイルが無い場合は空のまま返す
        }

        return list;
    }

    /** 既存IPかチェックする機能 → ★今回の追加部分★ */
    public static boolean exists(String ip) {
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", 2);
                if (cols.length > 0 && cols[0].trim().equals(ip.trim())) {
                    return true; // 既存
                }
            }
        } catch (Exception e) {
            // ファイルなしの場合は false
        }
        return false;
    }
}
