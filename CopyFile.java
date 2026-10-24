import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CopyFile {

    // Метод копирования (через FileInputStream)
    public static void copyWithStreams(String sourceName, String destPath) throws IOException {
        File source = new File(sourceName);
        File destination = new File(destPath);

        if (destination.getParentFile() != null) {
            destination.getParentFile().mkdirs();
        }

        try (FileInputStream sourceStream = new FileInputStream(source);
             FileOutputStream destinationStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = sourceStream.read(buffer)) > 0) {
                destinationStream.write(buffer, 0, bytesRead);
            }
        }
    }

    // Этот метод сам создаст файлы, если их нет
    public static void createFilesIfMissing() throws IOException {
        Path p1 = Paths.get("source1.txt");
        Path p2 = Paths.get("source2.txt");

        if (!Files.exists(p1)) {
            Files.writeString(p1, "Тестовый файл 1. ".repeat(50000)); // создаст ~700 КБ текста
            System.out.println("Создан файл: " + p1.toAbsolutePath());
        }
        if (!Files.exists(p2)) {
            Files.writeString(p2, "Тестовый файл 2. ".repeat(50000));
            System.out.println("Создан файл: " + p2.toAbsolutePath());
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. Создаем исходные файлы, если их нет
        createFilesIfMissing();

        // 2. Задаем пути (теперь они точно лежат в корне проекта)
        String src1 = "source1.txt";
        String src2 = "source2.txt";

        String seqDst1 = "output/seq/dest1.txt";
        String seqDst2 = "output/seq/dest2.txt";
        String parDst1 = "output/par/dest1.txt";
        String parDst2 = "output/par/dest2.txt";

        // 3. ПОСЛЕДОВАТЕЛЬНОЕ КОПИРОВАНИЕ
        long startSeq = System.nanoTime();
        copyWithStreams(src1, seqDst1);
        copyWithStreams(src2, seqDst2);
        long endSeq = System.nanoTime();
        long seqMs = TimeUnit.NANOSECONDS.toMillis(endSeq - startSeq);
        System.out.println("Последовательное копирование: " + seqMs + " мс");

        // 4. ПАРАЛЛЕЛЬНОЕ КОПИРОВАНИЕ
        ExecutorService executor = Executors.newFixedThreadPool(2);
        long startPar = System.nanoTime();

        executor.submit(() -> {
            try { copyWithStreams(src1, parDst1); } catch (IOException e) { e.printStackTrace(); }
        });
        executor.submit(() -> {
            try { copyWithStreams(src2, parDst2); } catch (IOException e) { e.printStackTrace(); }
        });

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);

        long endPar = System.nanoTime();
        long parMs = TimeUnit.NANOSECONDS.toMillis(endPar - startPar);
        System.out.println("Параллельное копирование: " + parMs + " мс");

        System.out.println("Ускорение: " + (parMs > 0 ? String.format("%.2f", (double) seqMs / parMs) : "n/a") + "x");
    }
}