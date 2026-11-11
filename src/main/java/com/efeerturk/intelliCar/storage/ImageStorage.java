package com.efeerturk.intelliCar.storage;

import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * İlan görsellerini yerel diskte saklar ve /uploads/** adresinden sunulur.
 * Güvenlik: dosyanın türü istemcinin söylediği Content-Type'a ya da dosya adına göre değil, dosyanın ilk baytlarına
 * göre belirlenir (JPEG, PNG, WEBP). Dosya adı rastgele üretilir, yani kullanıcıdan gelen ad diske hiç yazılmaz.
 * Gerçek ortamda bu sınıf S3 gibi bir nesne depolamasıyla değiştirilebilir.
 */
@Component
@Slf4j
public class ImageStorage {

    public static final String URL_PREFIX = "/uploads/";

    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final Path directory;
    private final long maxBytes;

    public ImageStorage(@Value("${intellicar.upload.dir:uploads}") String dir,
                        @Value("${intellicar.upload.max-file-bytes:5242880}") long maxBytes) {
        this.directory = Path.of(dir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Yükleme klasörü oluşturulamadı: " + directory, e);
        }
    }

    public Path directory() {
        return directory;
    }

    /** Dosyayı doğrular ve kaydeder. Dönen değer, /uploads/ ile başlayan göreli yoldur. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("Boş dosya");
        }
        if (file.getSize() > maxBytes) {
            throw invalid("Dosya çok büyük (en fazla " + (maxBytes / (1024 * 1024)) + " MB)");
        }
        try {
            byte[] head = new byte[12];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.readNBytes(head, 0, head.length);
            }
            String extension = detectExtension(head, read);
            if (extension == null) {
                throw invalid("Sadece JPEG, PNG veya WEBP görseller yüklenebilir");
            }
            String name = UUID.randomUUID() + "." + extension;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return URL_PREFIX + name;
        } catch (IOException e) {
            log.error("Görsel kaydedilemedi", e);
            throw new BaseException(new ErrorMessage(MessageType.IMAGE_STORAGE_FAILED, null));
        }
    }

    /** Bu depolamaya ait bir görseli siler (başka bir adresteki URL'lere dokunmaz). Hata fırlatmaz. */
    public void deleteQuietly(String url) {
        try {
            storedName(url).ifPresent(name -> {
                try {
                    Files.deleteIfExists(directory.resolve(name));
                } catch (IOException e) {
                    log.warn("Görsel dosyası silinemedi: {}", name);
                }
            });
        } catch (RuntimeException e) {
            log.warn("Görsel dosyası silinemedi", e);
        }
    }

    /** URL bizim yükleme klasörümüzdeki bir dosyaya aitse dosya adını döner. */
    static Optional<String> storedName(String url) {
        if (url == null) {
            return Optional.empty();
        }
        int idx = url.lastIndexOf(URL_PREFIX);
        if (idx < 0) {
            return Optional.empty();
        }
        String name = url.substring(idx + URL_PREFIX.length());
        Matcher matcher = STORED_NAME.matcher(name);
        return matcher.matches() ? Optional.of(name) : Optional.empty();
    }

    /** Dosyanın ilk baytlarına bakarak uzantıyı belirler. Desteklenmeyen türde null döner. */
    static String detectExtension(byte[] b, int length) {
        if (length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "png";
        }
        if (length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private BaseException invalid(String detail) {
        return new BaseException(new ErrorMessage(MessageType.INVALID_IMAGE_FILE, detail));
    }
}
