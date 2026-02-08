package com.example.smartspend.data.ai

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Helper class to sign requests using AWS Signature V4.
 */
object AwsSigV4Signer {

    private const val ALGORITHM = "AWS4-HMAC-SHA256"
    private const val SERVICE = "s3"
    private const val REGION = "us-east-1" // R2 recommends us-east-1 for broad S3 compatibility
    private const val TERMINATOR = "aws4_request"

    fun getSignedHeaders(
        method: String,
        url: String,
        accessKey: String,
        secretKey: String,
        contentSha256: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" // Empty payload hash
    ): Map<String, String> {
        val uri = URI(url)
        val host = uri.host
        val path = uri.path.ifEmpty { "/" }

        val now = Date()
        val amzDate = getAmzDate(now) // Format: yyyyMMdd'T'HHmmss'Z'
        val dateStamp = getDateStamp(now) // Format: yyyyMMdd

        // 1. Canonical Request
        val canonicalHeaders = "host:$host\nx-amz-content-sha256:$contentSha256\nx-amz-date:$amzDate\n"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
        val payloadHash = contentSha256

        val canonicalRequest = "$method\n" +
                "$path\n" +
                "\n" + // Query (empty)
                "$canonicalHeaders\n" +
                "$signedHeaders\n" +
                payloadHash

        // 2. String to Sign
        val credentialScope = "$dateStamp/$REGION/$SERVICE/$TERMINATOR"
        val stringToSign = "$ALGORITHM\n" +
                "$amzDate\n" +
                "$credentialScope\n" +
                hash(canonicalRequest)

        // 3. Signature
        val signingKey = getSignatureKey(secretKey, dateStamp, REGION, SERVICE)
        val signature = bytesToHex(hmacSHA256(stringToSign, signingKey))

        // 4. Authorization Header
        val authorization = "$ALGORITHM Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

        return mapOf(
            "Authorization" to authorization,
            "x-amz-date" to amzDate,
            "x-amz-content-sha256" to contentSha256,
            "Host" to host // OkHttp adds Host automatically, but good to know it's part of sign
        )
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4" + key).toByteArray(StandardCharsets.UTF_8)
        val kDate = hmacSHA256(dateStamp, kSecret)
        val kRegion = hmacSHA256(regionName, kDate)
        val kService = hmacSHA256(serviceName, kRegion)
        return hmacSHA256(TERMINATOR, kService)
    }

    private fun hmacSHA256(data: String, key: ByteArray): ByteArray {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun hash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(hash)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getAmzDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    private fun getDateStamp(date: Date): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }
}
