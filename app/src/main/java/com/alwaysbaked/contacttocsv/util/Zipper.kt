package com.alwaysbaked.contacttocsv.util

import android.util.Log

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Zipper {

    fun zip(_files: Array<String?>, zipFileName: String) {
        try {
            var origin: BufferedInputStream?
            val dest = FileOutputStream(zipFileName)
            val out = ZipOutputStream(BufferedOutputStream(
                    dest))
            val data = ByteArray(BUFFER)

            for (_file in _files) {
                Log.v("Compress", "Adding: $_file")
                val fi = FileInputStream(_file)
                origin = BufferedInputStream(fi, BUFFER)

                val entry = ZipEntry(_file?.substring(_file.lastIndexOf("/") + 1))
                out.putNextEntry(entry)
                var count = 0

                /* while((tmp=input.read(bytes))!=-1) { }
                 while (input.read(bytes).let { tmp = it; it != -1 }) { ... }*/

                while ((origin.read(data, 0, BUFFER)).let { count = it; it != -1 }) {
                    out.write(data, 0, count)
                }
                origin.close()
            }

            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        private const val BUFFER = 2048
    }
}
