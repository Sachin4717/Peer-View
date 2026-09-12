package com.remotesupport.desktop;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File read/write for the remote receiver. Writes are sandboxed to the
 * user's Downloads directory so a connected receiver cannot overwrite
 * arbitrary system files.
 */
public final class FileOps {

    private FileOps() {
    }

    public static Path downloadsDir() {
        return Path.of(System.getProperty("user.home"), "Downloads");
    }

    /** Rejects paths outside the Downloads sandbox. */
    public static boolean isAllowed(Path path) {
        try {
            return path.toAbsolutePath().normalize().startsWith(downloadsDir().toAbsolutePath().normalize());
        } catch (Exception e) {
            return false;
        }
    }

    /** Appends a fixed-size chunk at its index (offset = index * chunkSize). */
    public static void appendChunk(Path target, int chunkIndex, int chunkSize, byte[] data) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(target.toFile(), "rw")) {
            file.seek((long) chunkIndex * chunkSize);
            file.write(data);
        }
    }

    public static byte[] readChunk(Path file, long offset, int length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(offset);
            byte[] buffer = new byte[length];
            int read = raf.read(buffer);
            if (read < 0) {
                return new byte[0];
            }
            if (read < length) {
                byte[] trimmed = new byte[read];
                System.arraycopy(buffer, 0, trimmed, 0, read);
                return trimmed;
            }
            return buffer;
        }
    }

    public static long fileSize(Path file) throws IOException {
        return Files.size(file);
    }

    /** Lists files in the Downloads folder for the remote file browser. */
    public static String listFilesJson() {
        try (Stream<Path> files = Files.list(downloadsDir())) {
            return files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()))
                    .map(p -> p.getFileName() + " (" + sizeLabel(sizeOf(p)) + ")")
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "";
        }
    }

    private static long sizeOf(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    private static String sizeLabel(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Lists a directory for the remote file explorer. Directories first, then
     * files, each labeled with a name and size.
     */
    public static java.util.List<String[]> listDir(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .sorted((a, b) -> {
                        boolean aDir = Files.isDirectory(a);
                        boolean bDir = Files.isDirectory(b);
                        if (aDir != bDir) {
                            return aDir ? -1 : 1;
                        }
                        return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                    })
                    .map(p -> new String[]{
                            p.getFileName().toString(),
                            Files.isDirectory(p) ? "dir" : String.valueOf(sizeOf(p))
                    })
                    .collect(Collectors.toList());
        }
    }

    /** Reads a text file (any path) as UTF-8, capped at maxBytes. */
    public static String readText(Path file, int maxBytes) throws IOException {
        byte[] all = Files.readAllBytes(file);
        if (all.length > maxBytes) {
            byte[] capped = new byte[maxBytes];
            System.arraycopy(all, 0, capped, 0, maxBytes);
            return new String(capped, java.nio.charset.StandardCharsets.UTF_8)
                    + "\n... [truncated at " + maxBytes + " bytes]";
        }
        return new String(all, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Writes (or creates) a text file at any path. Returns bytes written. */
    public static long writeText(Path file, String content) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, bytes);
        return bytes.length;
    }

    /** Roots for the explorer dropdown: drives plus the user profile. */
    public static java.util.List<String> roots() {
        java.util.List<String> roots = new java.util.ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            roots.add(root.toString());
        }
        roots.add(System.getProperty("user.home"));
        roots.add(downloadsDir().toString());
        return roots;
    }
}
