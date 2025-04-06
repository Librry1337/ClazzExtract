package dev.librry.extract;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class ClassExtractor {

    private static final byte[] CLASS_MAGIC_NUMBER = {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\Root\\Downloads\\AyuGram Desktop\\journaltrace-dumper.bin";
        String outputJarPath = "extracted_classes.jar";

        try {
            System.out.println("Reading input file: " + inputFilePath);
            byte[] fileBytes = Files.readAllBytes(Paths.get(inputFilePath));
            System.out.println("Input file size: " + fileBytes.length + " bytes");

            System.out.println("Extracting potential class data...");
            List<byte[]> potentialClasses = extractPotentialClasses(fileBytes);

            if (potentialClasses.isEmpty()) {
                System.out.println("No potential class data found (no CAFEBABE markers detected).");
                return;
            }

            System.out.println("Found " + potentialClasses.size() + " potential class data segments.");
            System.out.println("Creating JAR file: " + outputJarPath);

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(new Attributes.Name("Clazz"), "ClassExtractor");

            try (FileOutputStream fos = new FileOutputStream(outputJarPath);
                 JarOutputStream jos = new JarOutputStream(fos, manifest)) {

                int addedCount = 0;
                for (int i = 0; i < potentialClasses.size(); i++) {
                    byte[] classData = potentialClasses.get(i);

                    if (classData.length >= CLASS_MAGIC_NUMBER.length && matches(classData, 0, CLASS_MAGIC_NUMBER)) {
                        String entryName = "ExtractedClass_" + (i + 1) + ".class";
                        JarEntry jarEntry = new JarEntry(entryName);

                        jos.putNextEntry(jarEntry);
                        jos.write(classData);
                        jos.closeEntry();
                        System.out.println("  Added entry: " + entryName + " (" + classData.length + " bytes)");
                        addedCount++;
                    } else {
                        System.err.println("  Warning: Skipping segment " + (i + 1) + " as it doesn't start with CAFEBABE (length: " + classData.length + ")");
                    }
                }
                System.out.println("Finished adding entries. Total added: " + addedCount);

            }

            System.out.println("Successfully created JAR file: " + outputJarPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<byte[]> extractPotentialClasses(byte[] fileBytes) {
        List<byte[]> result = new ArrayList<>();
        List<Integer> startIndices = findMarkerIndices(fileBytes, CLASS_MAGIC_NUMBER);

        if (startIndices.isEmpty()) {
            return result; 
        }

        System.out.println("Found " + startIndices.size() + " occurrences of the magic number.");

        for (int i = 0; i < startIndices.size(); i++) {
            int startIndex = startIndices.get(i);
            int endIndex;

            if (i + 1 < startIndices.size()) {
                endIndex = startIndices.get(i + 1);
            } else {
                endIndex = fileBytes.length;
            }

            if (startIndex < endIndex) {
                int length = endIndex - startIndex;
                byte[] classData = new byte[length];
                System.arraycopy(fileBytes, startIndex, classData, 0, length);
                result.add(classData);
                // System.out.println("  -> Segment " + (i+1) + ": Start=" + startIndex + ", End=" + endIndex + ", Length=" + length);
            } else {
            }
        }
        return result;
    }


    private static List<Integer> findMarkerIndices(byte[] data, byte[] marker) {
        List<Integer> indices = new ArrayList<>();
        if (marker == null || marker.length == 0) {
            return indices;
        }
        for (int i = 0; (i = indexOf(data, marker, i)) != -1; i++) {
            indices.add(i);
        }

        /*
        for (int i = 0; i <= data.length - marker.length; i++) {
            if (matches(data, i, marker)) {
                indices.add(i);
                // i += marker.length - 1;
            }
        }
        */
        return indices;
    }


    private static boolean matches(byte[] data, int index, byte[] marker) {
        if (marker == null || index < 0 || index + marker.length > data.length) {
            return false;
        }
        for (int i = 0; i < marker.length; i++) {
            if (data[index + i] != marker[i]) {
                return false;
            }
        }
        return true;
    }



    private static int indexOf(byte[] data, byte[] marker, int fromIndex) {
        if (marker == null || marker.length == 0 || data == null || data.length < marker.length) {
            return -1;
        }
        int dataLimit = data.length - marker.length;
        for (int i = Math.max(fromIndex, 0); i <= dataLimit; i++) {
            boolean found = true;
            for (int j = 0; j < marker.length; j++) {
                if (data[i + j] != marker[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }



    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
