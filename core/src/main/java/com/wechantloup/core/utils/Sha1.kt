package com.wechantloup.core.utils

import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.experimental.and

private fun convertToHex(data: ByteArray): String? {
    val buf = StringBuilder()
    for (b in data) {
        var halfbyte: Int = b.toInt() ushr 4 and 0x0F
        var twoHalfs = 0
        do {
            buf.append(if (halfbyte in 0..9) ('0'.toInt() + halfbyte).toChar() else ('a'.toInt() + (halfbyte - 10)).toChar())
            halfbyte = (b and 0x0F).toInt()
        } while (twoHalfs++ < 1)
    }
    return buf.toString()
}

@Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
fun SHA1(text: String): String? {
    val md: MessageDigest = MessageDigest.getInstance("SHA-1")
    val textBytes = text.toByteArray(charset("iso-8859-1"))
    md.update(textBytes, 0, textBytes.size)
    val sha1hash: ByteArray = md.digest()
    return convertToHex(sha1hash)
}