import common.common_log;
/**
 * アプリケーション終了処理を担当
 */
public class ExitService {
    public static void run() {
        System.out.println("exit..");
        common_log.info("tui_tool exit");
        System.exit(0);
    }
}
