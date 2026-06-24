package com.example.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    @Throws(IOException::class)
    fun compressFileOrFolder(sourceFile: File, destinationZipFile: File, onProgress: (Float) -> Unit = {}) {
        val zipOut = ZipOutputStream(FileOutputStream(destinationZipFile))
        val totalFiles = countFiles(sourceFile)
        var compressedCount = 0

        fun zipFile(fileToZip: File, fileName: String) {
            if (fileToZip.isHidden) {
                return
            }
            if (fileToZip.isDirectory) {
                val children = fileToZip.listFiles() ?: return
                for (childFile in children) {
                    zipFile(childFile, "$fileName/${childFile.name}")
                }
                return
            }
            FileInputStream(fileToZip).use { fis ->
                val zipEntry = ZipEntry(fileName)
                zipOut.putNextEntry(zipEntry)
                val bytes = ByteArray(4096)
                var length: Int
                while (fis.read(bytes).also { length = it } >= 0) {
                    zipOut.write(bytes, 0, length)
                }
                compressedCount++
                onProgress(compressedCount.toFloat() / totalFiles.coerceAtLeast(1))
                try {
                    Thread.sleep(150) // Beautiful UX pacing delay
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }

        zipFile(sourceFile, sourceFile.name)
        zipOut.close()
    }

    @Throws(IOException::class)
    fun decompressZip(zipFile: File, destDirectory: File, onProgress: (Float) -> Unit = {}) {
        if (!destDirectory.exists()) {
            destDirectory.mkdirs()
        }

        // Count entries first for progress estimation
        var totalEntries = 0
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            while (zis.nextEntry != null) {
                totalEntries++
            }
        }

        var processedEntries = 0
        val buffer = ByteArray(4096)
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newFile = File(destDirectory, zipEntry.name)
                if (zipEntry.isDirectory) {
                    if (!newFile.isDirectory && !newFile.mkdirs()) {
                        throw IOException("Failed to create directory $newFile")
                    }
                } else {
                    val parent = newFile.parentFile
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw IOException("Failed to create directory $parent")
                    }
                    FileOutputStream(newFile).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                processedEntries++
                onProgress(processedEntries.toFloat() / totalEntries.coerceAtLeast(1))
                try {
                    Thread.sleep(150) // Beautiful UX pacing delay
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                zis.closeEntry()
                zipEntry = zis.nextEntry
            }
        }
    }

    private fun countFiles(file: File): Int {
        if (file.isFile) return 1
        var count = 0
        val children = file.listFiles() ?: return 0
        for (child in children) {
            count += countFiles(child)
        }
        return count
    }
}
