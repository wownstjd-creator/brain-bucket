package com.brainbucket.post;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PosterStorage {

  private final Path uploadDir;

  public PosterStorage(@Value("${app.upload-dir}") String uploadDir) {
    this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
  }

  public StoredPoster store(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
      throw badImage("이미지는 비어 있지 않아야 하며 10MB 이하여야 합니다.");
    }

    BufferedImage image;
    String format;

    try (var input = file.getInputStream();
        ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {

      if (imageInput == null) {
        throw badImage("이미지를 읽을 수 없습니다.");
      }

      var readers = ImageIO.getImageReaders(imageInput);
      if (!readers.hasNext()) {
        throw badImage("지원하는 이미지가 아닙니다.");
      }

      ImageReader reader = readers.next();

      try {
        reader.setInput(imageInput);
        format = reader.getFormatName().toLowerCase(Locale.ROOT);

        if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
          throw badImage("JPG 또는 PNG 이미지만 가능합니다.");
        }

        int width = reader.getWidth(0);
        int height = reader.getHeight(0);

        if (width <= 0
            || height <= 0
            || width > 10000
            || height > 10000
            || (long) width * height > 20_000_000) {
          throw badImage("이미지 크기는 최대 2천만 픽셀입니다.");
        }

        image = reader.read(0);

        if (image == null) {
          throw badImage("이미지를 읽을 수 없습니다.");
        }
      } finally {
        reader.dispose();
      }
    } catch (IOException exception) {
      throw badImage("손상되었거나 읽을 수 없는 이미지입니다.");
    }

    String extension = format.equals("png") ? "png" : "jpg";
    String contentType = extension.equals("png") ? "image/png" : "image/jpeg";

    String storageKey = UUID.randomUUID() + "." + extension;
    Path destination = uploadDir.resolve(storageKey);

    try {
      Files.createDirectories(uploadDir);

      try (OutputStream output =
          Files.newOutputStream(
              destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        if (!ImageIO.write(image, extension, output)) {
          throw new IOException("이미지 저장 형식을 지원하지 않습니다.");
        }
      }

      return new StoredPoster(storageKey, contentType, Files.size(destination));
    } catch (IOException exception) {
      try {
        Files.deleteIfExists(destination);
      } catch (IOException cleanupException) {
        exception.addSuppressed(cleanupException);
      }

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.", exception);
    }
  }

  private ResponseStatusException badImage(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public record StoredPoster(String storageKey, String contentType, long sizeBytes) {}

  public void deleteStored(String storageKey) throws IOException {
    Path file = uploadDir.resolve(storageKey).normalize();

    if (!uploadDir.equals(file.getParent())) {
      throw new IOException("잘못된 저장 경로입니다.");
    }

    Files.deleteIfExists(file);
  }

  public Path loadStored(String storageKey) {
    Path file = uploadDir.resolve(storageKey).normalize();

    if (!uploadDir.equals(file.getParent()) || !Files.isRegularFile(file)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    return file;
  }
}
