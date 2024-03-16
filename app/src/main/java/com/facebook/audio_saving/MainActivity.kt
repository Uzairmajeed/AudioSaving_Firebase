package com.facebook.audio_saving

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var uploadButton: Button
    private lateinit var getDataButton: Button
    private lateinit var fetchedDataTextView: TextView
    private lateinit var fetchedFromRealtimeTextView: TextView
    private lateinit var storage: FirebaseStorage
    private lateinit var database: DatabaseReference
    private val PICK_AUDIO_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Storage and Realtime Database
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Find the buttons and TextViews by ID
        uploadButton = findViewById(R.id.uploadbutton)
        getDataButton = findViewById(R.id.getDataButton)
        fetchedDataTextView = findViewById(R.id.fetchedDataTextView)
        fetchedFromRealtimeTextView = findViewById(R.id.fetchedFromRealtime)

        // Set click listener for the uploadButton
        uploadButton.setOnClickListener {
            // This is for Open file picker to select audio file
            val intent = Intent()
            intent.type = "audio/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Audio"), PICK_AUDIO_REQUEST)
        }

        // Set click listener for the getDataButton
        getDataButton.setOnClickListener {
            // Fetch and display the url names of saved audio files
            fetchAudioUrls()
        }

        // Set click listener for the savetorealtime button
        val savetorealtimeButton: Button = findViewById(R.id.savetorealtime)
        savetorealtimeButton.setOnClickListener {
            saveToFbRealtimeDatabase()

            // Fetch data from Realtime Database on startup
            fetchFromFbRealtimeDatabase()
        }

    }

    // This is for getting the audio file's url..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            // Get the selected audio file's URI
            val audioUri: Uri = data.data ?: return

            // Upload the selected audio file to Firebase Storage
            uploadAudio(audioUri)
        }
    }

    // Upload audio file to Firebase Storage
    private fun uploadAudio(audioUri: Uri) {
        val storageRef = storage.reference
        val audioRef = storageRef.child("audio/${System.currentTimeMillis()}.mp3") // Generate a unique filename
        val uploadTask = audioRef.putFile(audioUri)

        uploadTask.addOnSuccessListener {
            // Audio upload successful
            Toast.makeText(this, "Audio uploaded successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { exception ->
            // Handle unsuccessful uploads
            Toast.makeText(this, "Failed to upload audio: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch URLs of audio files from Firebase Storage
    private fun fetchAudioUrls() {
        val storageRef = storage.reference.child("audio")

        // List all items in the "audio" folder
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                // Get URLs of all audio files and display them in the TextView
                val urls = mutableListOf<String>()
                for (item in listResult.items) {
                    item.downloadUrl.addOnSuccessListener { uri ->
                        urls.add(uri.toString())
                        fetchedDataTextView.text = urls.joinToString("\n")
                    }.addOnFailureListener { exception ->
                        // Handle any errors
                        Toast.makeText(this, "Failed to fetch audio URLs: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle unsuccessful fetch
                Toast.makeText(this, "Failed to fetch audio names: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Save fetched URL to Firebase Realtime Database
    private fun saveToFbRealtimeDatabase() {
        val fetchedUrl = fetchedDataTextView.text.toString().trim()
        if (fetchedUrl.isNotEmpty()) {
            val key = database.child("audioUrls").push().key ?: ""
            database.child("audioUrls").child(key).setValue(fetchedUrl)
                .addOnSuccessListener {
                    Toast.makeText(this, "URL saved to Realtime Database", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to save URL: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No URL to save", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch data from Firebase Realtime Database
    private fun fetchFromFbRealtimeDatabase() {
        database.child("audioUrls").limitToLast(1).get()
            .addOnSuccessListener { snapshot ->
                for (childSnapshot in snapshot.children) {
                    val fetchedUrl = childSnapshot.value.toString()
                    fetchedFromRealtimeTextView.text = fetchedUrl
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch URL from Realtime Database: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
