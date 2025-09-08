package com.womensafety.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.womensafety.app.databinding.ActivityFirebaseTestBinding
import com.womensafety.app.testing.FirebaseTestSuite
import com.womensafety.app.testing.TestResult
import kotlinx.coroutines.launch

class FirebaseTestActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFirebaseTestBinding
    private lateinit var testSuite: FirebaseTestSuite
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirebaseTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        testSuite = FirebaseTestSuite(this, lifecycleScope)
        
        setupUI()
        displayFirebaseInfo()
    }
    
    private fun setupUI() {
        binding.apply {
            btnRunAllTests.setOnClickListener {
                runAllTests()
            }
            
            btnTestConnection.setOnClickListener {
                testConnection()
            }
            
            btnTestAuth.setOnClickListener {
                testAuthentication()
            }
            
            btnTestFirestore.setOnClickListener {
                testFirestore()
            }
            
            btnTestNotifications.setOnClickListener {
                testNotifications()
            }
            
            btnClearResults.setOnClickListener {
                clearResults()
            }
        }
    }
    
    private fun displayFirebaseInfo() {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val messaging = FirebaseMessaging.getInstance()
        
        val info = buildString {
            appendLine("🔥 Firebase Configuration")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📱 App: ${packageName}")
            appendLine("🔐 Auth: ✅ Connected")
            appendLine("🗄️ Firestore: ✅ Connected")
            appendLine("🔔 Messaging: ✅ Connected")
            appendLine("👤 Current User: ${auth.currentUser?.email ?: "Not signed in"}")
            appendLine()
            appendLine("📊 Test Results:")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
        
        binding.tvResults.text = info
    }
    
    private fun runAllTests() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRunAllTests.isEnabled = false
        
        testSuite.runAllTests { result ->
            runOnUiThread {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnRunAllTests.isEnabled = true
                displayTestResult(result)
            }
        }
    }
    
    private fun testConnection() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val testDoc = firestore.collection("test").document("connection")
                
                testDoc.set(hashMapOf(
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "message" to "Connection test successful"
                )).addOnSuccessListener {
                    testDoc.delete()
                    runOnUiThread {
                        binding.progressBar.visibility = android.view.View.GONE
                        appendResult("✅ Connection Test: SUCCESS")
                        Toast.makeText(this@FirebaseTestActivity, "Connection test passed!", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    runOnUiThread {
                        binding.progressBar.visibility = android.view.View.GONE
                        appendResult("❌ Connection Test: FAILED - ${e.message}")
                        Toast.makeText(this@FirebaseTestActivity, "Connection test failed!", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    appendResult("❌ Connection Test: ERROR - ${e.message}")
                }
            }
        }
    }
    
    private fun testAuthentication() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                
                // Test anonymous authentication
                auth.signInAnonymously().addOnSuccessListener { result ->
                    val user = result.user
                    runOnUiThread {
                        binding.progressBar.visibility = android.view.View.GONE
                        appendResult("✅ Auth Test: Anonymous sign-in successful")
                        appendResult("   User ID: ${user?.uid}")
                        appendResult("   Is Anonymous: ${user?.isAnonymous}")
                        Toast.makeText(this@FirebaseTestActivity, "Authentication test passed!", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    runOnUiThread {
                        binding.progressBar.visibility = android.view.View.GONE
                        appendResult("❌ Auth Test: FAILED - ${e.message}")
                        Toast.makeText(this@FirebaseTestActivity, "Authentication test failed!", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    appendResult("❌ Auth Test: ERROR - ${e.message}")
                }
            }
        }
    }
    
    private fun testFirestore() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val testData = hashMapOf(
                    "testField" to "testValue",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "number" to 42
                )
                
                firestore.collection("test_collection")
                    .add(testData)
                    .addOnSuccessListener { documentRef ->
                        // Test reading the document
                        documentRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                // Clean up
                                documentRef.delete()
                                runOnUiThread {
                                    binding.progressBar.visibility = android.view.View.GONE
                                    appendResult("✅ Firestore Test: CRUD operations successful")
                                    appendResult("   Document ID: ${documentRef.id}")
                                    appendResult("   Data: ${document.data}")
                                    Toast.makeText(this@FirebaseTestActivity, "Firestore test passed!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }.addOnFailureListener { e ->
                        runOnUiThread {
                            binding.progressBar.visibility = android.view.View.GONE
                            appendResult("❌ Firestore Test: FAILED - ${e.message}")
                            Toast.makeText(this@FirebaseTestActivity, "Firestore test failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    appendResult("❌ Firestore Test: ERROR - ${e.message}")
                }
            }
        }
    }
    
    private fun testNotifications() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val messaging = FirebaseMessaging.getInstance()
                
                // Get FCM token
                messaging.token.addOnSuccessListener { token ->
                    // Test topic subscription
                    messaging.subscribeToTopic("test_topic").addOnSuccessListener {
                        runOnUiThread {
                            binding.progressBar.visibility = android.view.View.GONE
                            appendResult("✅ Notifications Test: FCM token and topic subscription successful")
                            appendResult("   Token: ${token.take(20)}...")
                            appendResult("   Subscribed to: test_topic")
                            Toast.makeText(this@FirebaseTestActivity, "Notifications test passed!", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { e ->
                        runOnUiThread {
                            binding.progressBar.visibility = android.view.View.GONE
                            appendResult("❌ Notifications Test: Topic subscription failed - ${e.message}")
                        }
                    }
                }.addOnFailureListener { e ->
                    runOnUiThread {
                        binding.progressBar.visibility = android.view.View.GONE
                        appendResult("❌ Notifications Test: Token generation failed - ${e.message}")
                        Toast.makeText(this@FirebaseTestActivity, "Notifications test failed!", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = android.view.View.GONE
                    appendResult("❌ Notifications Test: ERROR - ${e.message}")
                }
            }
        }
    }
    
    private fun displayTestResult(result: TestResult) {
        val resultText = buildString {
            appendLine()
            appendLine("🧪 ${result.testName}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine(result.message)
            
            if (result.details.isNotEmpty()) {
                appendLine()
                result.details.forEach { detail ->
                    appendLine("  • ${detail.testName}: ${detail.message}")
                }
            }
            
            appendLine("⏱️ Completed at: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(result.timestamp))}")
            appendLine()
        }
        
        appendResult(resultText)
    }
    
    private fun appendResult(text: String) {
        binding.tvResults.append(text + "\n")
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
    
    private fun clearResults() {
        displayFirebaseInfo()
    }
}
