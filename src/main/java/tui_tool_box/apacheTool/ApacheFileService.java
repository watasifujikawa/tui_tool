package apacheTool;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

public class ApacheFileService {

    private static final Path APACHE_DIR = Paths.get("/etc/apache2");

    /**
     * /etc/apache2 が存在するかチェックする
     */
    public static boolean checkApacheDir() {
        return Files.exists(APACHE_DIR) && Files.isDirectory(APACHE_DIR);
    }

    /**
     * 初回：_org 作成
     * 2回目以降：_org → 正規ファイルに強制復元
     */
    public static void backupForEdit(Path originalFile) throws IOException {

        if (!Files.exists(originalFile)) return;

        Path orgBackup = Paths.get(originalFile.toString() + "_org");

        // 初回
        if (!Files.exists(orgBackup)) {
            Files.copy(originalFile, orgBackup, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        // 2回目以降 → _org から復元
        Files.copy(orgBackup, originalFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 修正後バックアップ
     * <file>_YYYYMMDD
     */
    public static void backupAfterEdit(Path originalFile) throws IOException {

        if (!Files.exists(originalFile)) return;

        String date = LocalDate.now().toString().replace("-", "");
        Path postBackup = Paths.get(originalFile.toString() + "_" + date);

        Files.copy(originalFile, postBackup, StandardCopyOption.REPLACE_EXISTING);
    }
}
