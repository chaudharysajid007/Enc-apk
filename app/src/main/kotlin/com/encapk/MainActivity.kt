package com.encapk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var protectButton: Button
    private lateinit var scrollView: ScrollView

    private val TEST_FOLDER = "/storage/emulated/0/Test/all files"
    private val fileMapping = mutableMapOf<String, String>()
    private var filesProcessed = 0
    private var filesFailed = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        protectButton = findViewById(R.id.protectButton)
        scrollView = findViewById(R.id.scrollView)

        checkAndRequestPermissions()

        protectButton.setOnClickListener {
            if (checkPermissions()) {
                protectFiles()
            } else {
                Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }
    }

    private fun checkPermissions(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED

        val writeGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED

        return readGranted && writeGranted
    }

    private fun protectFiles() {
        val testDir = File(TEST_FOLDER)

        if (!testDir.exists()) {
            statusText.text = "❌ Folder not found:\n$TEST_FOLDER"
            return
        }

        val fileList = testDir.listFiles()?.filter { it.isFile }

        if (fileList.isNullOrEmpty()) {
            statusText.text = "📂 No files found"
            return
        }

        filesProcessed = 0
        filesFailed = 0
        fileMapping.clear()

        statusText.text = "🔒 Starting...\n"

        protectButton.isEnabled = false

        fileList.forEach { file ->
            try {
                val originalName = file.name
                val uniqueId = generateUniqueId(originalName)
                val randomExt = generateRandomExtension()
                val newName = "${uniqueId}_${randomExt}"

                fileMapping[newName] = originalName

                val newFile = File(testDir, newName)

                if (file.renameTo(newFile)) {
                    filesProcessed++
                    statusText.append("✅ $originalName → $newName\n")
                } else {
                    filesFailed++
                    statusText.append("❌ Failed: $originalName\n")
                }

            } catch (e: Exception) {
                filesFailed++
                statusText.append("❌ Error: ${file.name}\n")
            }
        }

        statusText.append("\nDone: $filesProcessed success, $filesFailed failed")
        protectButton.isEnabled = true
    }

    private fun generateUniqueId(filename: String): String {
        val data = "$filename${System.currentTimeMillis()}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(8)
    }

    private fun generateRandomExtension(): String {
        val extensions = listOf("xyz", "prt", "lck", "enc", "dat")
        return extensions.random()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        }
    }
}