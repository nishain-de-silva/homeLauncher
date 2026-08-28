package com.ndds.homelauncher.services

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import com.ndds.homelauncher.PlayStoreMetaDataExtractor
import com.ndds.homelauncher.R
import org.json.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.set
import kotlin.text.iterator

class WordsTagsExtractor {
    fun extractTags(packageName: String, onResponse: (data: JSONArray?) -> Unit) {
        fetchGooglePlayListing(packageName, {
                result ->
            if (result == null) {
                Log.d("info", "unable to capture description for package: $packageName reason: no response data")
                onResponse(null)
                return@fetchGooglePlayListing
            }
            val collectedMetaData = PlayStoreMetaDataExtractor.getMetaData(
                result, 5,
                intArrayOf(1, 2, 72, 0, 1),  // app long description
            )
            var ignore = false
            var capturedWord: String? = null
            val words = JSONArray()
            if (collectedMetaData != null && collectedMetaData[0] != null) {
                val description = collectedMetaData[0]!!
                for (c in description) {
                    if (ignore) {
                        if (c == '>')
                            ignore = false
                        continue
                    } else if (c == '<') {
                        ignore = true
                        continue
                    }
                    if (c in 'a'..'z') {
                        if (capturedWord == null) {
                            capturedWord = c.toString()
                        } else
                            capturedWord += c
                    } else if (c in 'A'..'Z') {
                        if (capturedWord == null) {
                            capturedWord = c.lowercase()
                        } else
                            capturedWord += c.lowercase()
                    } else {
                        if (capturedWord != null) {
                            if (capturedWord.length >= 4)
                                words.put(capturedWord)
                            capturedWord = null
                        }
                    }
                }
                if (capturedWord != null) {
                    if (capturedWord.length >= 4)
                        words.put(capturedWord)
                }
                onResponse(words)
            } else {
                Log.d(
                    "info",
                    "unable to capture description for package: $packageName reason: malformed meta data"
                )
                onResponse(null)
            }
        })
    }

    private fun fetchGooglePlayListing(
        packageName: String?,
        onResponseFetch: (String?) -> Unit
    ): Thread {
        val handler = Handler(Looper.getMainLooper())
        val networkThread: Thread = object : Thread() {
            var connection: HttpURLConnection? = null
            override fun interrupt() {
                if (connection != null) connection!!.disconnect()
            }

            override fun run() {
                var reader: BufferedReader? = null
                try {
                    // Encode the query to avoid spaces or symbols breaking the URL
                    val url =
                        URL("https://play.google.com/store/apps/details?id=" + packageName + "&hl=en")
                    Log.d("info", "url here: " + url.toString())
                    connection = url.openConnection() as HttpURLConnection?
                    connection!!.setRequestMethod("GET")
                    connection!!.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                    )
                    connection!!.setConnectTimeout(5000)
                    connection!!.setReadTimeout(5000)

                    val responseCode = connection!!.getResponseCode()
                    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        onResponseFetch("")
                        return
                    }
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("info", "got the result here")
                        if (isInterrupted()) return
                        reader = BufferedReader(InputStreamReader(connection!!.getInputStream()))
                        var line: String?
                        val stringBuilder = StringBuilder()

                        while ((reader.readLine().also { line = it }) != null) {
                            stringBuilder.append(line)
                            stringBuilder.append('\n')
                        }
                        handler.post(Runnable { onResponseFetch(stringBuilder.toString()) })
                    } else {
                        handler.post(Runnable {
                            onResponseFetch(null)
                            Log.e("HTTP", "Response Code: " + responseCode)
                        })
                    }
                } catch (e: Exception) {
                    handler.post(Runnable {
                        Log.d("info", "caught level 1 exception", e)
                        onResponseFetch(null)
                    })
                } finally {
                    try {
                        if (reader != null) reader.close()
                        if (connection != null) connection!!.disconnect()
                    } catch (e: IOException) {
                        handler.post(Runnable {
                            Log.d("info", "caught level 2 exception", e)
                            onResponseFetch(null)
                        })
                    }
                }
            }
        }
        networkThread.start()
        return networkThread
    }
}