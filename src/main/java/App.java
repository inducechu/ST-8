import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        System.setProperty(
                "webdriver.chrome.driver",
                "C:\\Users\\aa561\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        Path inputConfigPath = Paths.get("data", "data.txt");
        if (!Files.exists(inputConfigPath)) {
            System.err.println("Критическая ошибка: отсутствует файл данных " + inputConfigPath.toAbsolutePath());
            return;
        }

        String targetArtist = "";
        String targetAlbum = "";
        List<String> trackList = new ArrayList<>();

        for (String fileRow : Files.readAllLines(inputConfigPath)) {
            fileRow = fileRow.trim();
            if (fileRow.toLowerCase().startsWith("artist:")) {
                targetArtist = fileRow.substring(7).trim();
            } else if (fileRow.toLowerCase().startsWith("title:")) {
                targetAlbum = fileRow.substring(6).trim();
            } else if (fileRow.toLowerCase().matches("track \\d+:.*")) {
                int separatorIdx = fileRow.indexOf(':');
                if (separatorIdx != -1) {
                    trackList.add(fileRow.substring(separatorIdx + 1).trim());
                }
            }
        }

        String finalDownloadDirectory = Paths.get("result").toAbsolutePath().toString();
        Files.createDirectories(Paths.get(finalDownloadDirectory));

        Map<String, Object> chromePrefsMap = new HashMap<>();
        chromePrefsMap.put("download.default_directory", finalDownloadDirectory);
        chromePrefsMap.put("download.prompt_for_download", false);
        chromePrefsMap.put("download.directory_upgrade", true);
        chromePrefsMap.put("plugins.always_open_pdf_externally", true);
        chromePrefsMap.put("safebrowsing.enabled", false);

        ChromeOptions customBrowserOptions = new ChromeOptions();
        customBrowserOptions.setExperimentalOption("prefs", chromePrefsMap);
        customBrowserOptions.addArguments("--remote-allow-origins=*", "--ignore-certificate-errors", "--disable-safe-browsing");
        customBrowserOptions.setAcceptInsecureCerts(true);

        WebDriver webBrowser = new ChromeDriver(customBrowserOptions);
        WebDriverWait dynamicWait = new WebDriverWait(webBrowser, Duration.ofSeconds(20));

        try {
            webBrowser.get("https://www.papercdcase.com/index.php");
            dynamicWait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));

            WebElement artistInput = webBrowser.findElement(By.name("artist"));
            artistInput.clear();
            artistInput.sendKeys(targetArtist);

            WebElement titleInput = webBrowser.findElement(By.name("title"));
            titleInput.clear();
            titleInput.sendKeys(targetAlbum);

            for (int idx = 0; idx < trackList.size() && idx < 16; idx++) {
                String inputId = "track" + (idx + 1);
                try {
                    WebElement currentTrackField = webBrowser.findElement(By.name(inputId));
                    currentTrackField.clear();
                    currentTrackField.sendKeys(trackList.get(idx));
                } catch (Exception ignored) {
                }
            }

            WebElement jewelTypeBox = webBrowser.findElement(By.xpath("//input[@value='jewel']"));
            if (!jewelTypeBox.isSelected()) jewelTypeBox.click();

            WebElement sizeA4Box = webBrowser.findElement(By.xpath("//input[@value='a4']"));
            if (!sizeA4Box.isSelected()) sizeA4Box.click();

            WebElement actionSubmitBtn = webBrowser.findElement(By.xpath("//input[@type='submit']"));
            actionSubmitBtn.submit();

            executeFileMonitoring(finalDownloadDirectory, 25000);
            processTargetFileRenaming(finalDownloadDirectory);

        } finally {
            Thread.sleep(1500);
            webBrowser.quit();
        }
    }

    private static void executeFileMonitoring(String folderPath, long maxTimeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < maxTimeout) {
            File[] folderFiles = new File(folderPath).listFiles(file ->
                    file.isFile() && file.getName().toLowerCase().endsWith(".pdf")
                            && !file.getName().endsWith(".crdownload") && !file.getName().startsWith(".com.google"));

            if (folderFiles != null && folderFiles.length > 0) return;
            Thread.sleep(1000);
        }
    }

    private static void processTargetFileRenaming(String folderPath) throws IOException {
        File[] scannedPdfs = new File(folderPath).listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".pdf"));
        if (scannedPdfs == null || scannedPdfs.length == 0) return;

        File destinationFile = new File(folderPath, "cd.pdf");
        if (destinationFile.exists()) {
            destinationFile.delete();
        }

        if (!scannedPdfs[0].renameTo(destinationFile)) {
            Files.copy(scannedPdfs[0].toPath(), destinationFile.toPath());
            scannedPdfs[0].delete();
        }
        System.out.println("Процесс завершен. Файл сохранен как: result/cd.pdf");
    }
}
