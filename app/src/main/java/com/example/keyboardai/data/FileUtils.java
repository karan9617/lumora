package com.example.keyboardai.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    /**
     * Loads a file from a given path and converts it into a byte array.
     * This is useful for saving images or other binary data to a database or for in-memory processing.
     *
     * @param filePath The absolute path to the file to be loaded.
     * @return A byte array containing the file's data, or null if the file doesn't exist or an error occurs.
     */
    public static byte[] loadFileFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            // The file does not exist, so we can't load it.
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] buffer = new byte[1024]; // A buffer to read data in chunks
            int bytesRead;

            // Read the file content into the byte array output stream
            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Return null on error
        } finally {
            try {
                if (is != null) {
                    is.close(); // Ensure the input stream is closed
                }
                bos.close(); // Ensure the output stream is closed
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}