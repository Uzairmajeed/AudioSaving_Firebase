package com.facebook.audio_saving

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {
    private lateinit var uploadButton: Button
    private lateinit var getDataButton: Button
    private lateinit var fetchedDataTextView: TextView
    private lateinit var storage: FirebaseStorage
    private val PICK_AUDIO_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance()

        // Find the button by ID
        uploadButton = findViewById(R.id.uploadbutton)
        getDataButton = findViewById(R.id.getDataButton)
        fetchedDataTextView = findViewById(R.id.fetchedDataTextView)

        // Set click listener for the button
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
    }

    //This is for getting the audio file's url..

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            // Get the selected audio file's URI
            val audioUri: Uri = data.data ?: return

            // Upload the selected audio file to Firebase Storage
            uploadAudio(audioUri)
        }
    }

    //Updating Data To Firebase..

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

    //Fetching URl Data From Firebase..

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


}
