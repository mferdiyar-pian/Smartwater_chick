import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class FixUnicode {
    public static void main(String[] args) throws IOException {
        String dirPath = "d:/Smartwater_chick/app/src/main/java/com/example/smartwaterchick";
        File dir = new File(dirPath);
        processDir(dir);
        System.out.println("Done!");
    }

    static void processDir(File dir) throws IOException {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                processDir(f);
            } else if (f.getName().endsWith(".java")) {
                fixFile(f);
            }
        }
    }

    static void fixFile(File file) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);
        String newContent = content
            .replace("âœ…", "✅")
            .replace("â Œ", "❌")
            .replace("âš ï¸ ", "⚠️ ")
            .replace("âš\u00A0ï¸ ", "⚠️ ")
            .replace("âš", "⚠️ ")
            .replace("ï¸", "")
            .replace("â ³", "⏳")
            .replace("â”€", "─")
            .replace("â€”", "—")
            .replace("â†’", "→")
            .replace("âœ“", "✓")
            .replace("Ã¢â‚¬Â¢", "•");
            
        // Because "âš ï¸ " can get messed up, let's also just strip left over parts if any.
        newContent = newContent.replace("⚠️ \u00A0", "⚠️");
        
        if (!content.equals(newContent)) {
            Files.write(Paths.get(file.getAbsolutePath()), newContent.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fixed " + file.getName());
        }
    }
}
