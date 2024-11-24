package com.example.webrtc

import PeersAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtc.model.UserData
import com.google.gson.Gson
import org.json.JSONObject

class MainActivity : AppCompatActivity(), SignalingChannel.Listener, WebRTCManager.WebRTCListener {
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var statusText: TextView
    private lateinit var userNameText: TextView
    private lateinit var bioText: TextView
    private lateinit var connectButton: Button
    private lateinit var sendUserProfileButton: Button
    private lateinit var pickImageButton: Button
    private lateinit var ipAddressField: EditText
    private lateinit var usernameField: EditText
    private lateinit var bioField: EditText
    private lateinit var imageView: ImageView

    private var peersList = mutableListOf<String>()
    private lateinit var peersAdapter: PeersAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        ipAddressField = findViewById(R.id.ipAddressField)
        usernameField = findViewById(R.id.usernameField)
        bioField = findViewById(R.id.bioField)
        sendUserProfileButton = findViewById(R.id.sendUserProfile)
        pickImageButton = findViewById(R.id.pickImageButton)
        imageView = findViewById(R.id.receivedImageView)


        // Connect to WebSocket server when the button is clicked
        connectButton.setOnClickListener {
            var ipAddress = ipAddressField.text.toString()
            if (ipAddress.isBlank()) {
                ipAddress = "http://3.85.163.126:3030"
            }
            setupSignalingChannel(ipAddress) // Initialize signaling channel directly
            statusText.text = "Status: Connected"
            Log.i("Received Data", "Button clicked")
        }

        sendUserProfileButton.setOnClickListener {
            var username = usernameField.text.toString()
            var bio = bioField.text.toString()

            val userData = UserData(username, bio)
            val gson = Gson()
            val json = gson.toJson(userData)
            webRTCManager.sendJson(json)
        }

        findViewById<Button>(R.id.pickImageButton).setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupSignalingChannel(signalingServerUrl: String) {
        // Define peerId, peerType, and token (replace with actual values)
        val peerId = "myUniquePeerId" // You can generate or retrieve this dynamically
        val peerType = "myPeerType" // Define your peer type
        val token = "C6fak5vMANe6HU92xDVdR2TVtgWjHUw7FB7IW5R5CY" // Define your token (if required)

        // Initialize signaling channel
        webRTCManager = WebRTCManager(this, signalingServerUrl, token)
        webRTCManager.initialize()

        // Connect to the signaling channel
        webRTCManager.connect()
        webRTCManager.setWebRTCListener(this)

//        val peersRecyclerView = findViewById<RecyclerView>(R.id.peersRecyclerView)
//        peersAdapter = PeersAdapter(webRTCManager.getConnectedPeers())
//        peersRecyclerView.adapter = peersAdapter
//
//        // Example to refresh the list when a peer connects
//        webRTCManager.onPeerConnected("newPeerId")
//        peersAdapter.notifyDataSetChanged()
//        // Update UI upon connection
    }


    override fun onPeersUpdated(newPeersList: List<String>) {
        runOnUiThread {
            peersList.clear()
            peersList.addAll(newPeersList)
            statusText.text = "Peers found"
            peersAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onImageReceived(imageBitmap: Bitmap) {
        // Run on the main thread to update the UI
        runOnUiThread {
            statusText.text = "Image Received"
            imageView.setImageBitmap(imageBitmap)
        }
    }

    override fun onJsonRecived(json: String) {
        val jsonObject = JSONObject(json)
        val username = jsonObject.getString("username")
        val bio = jsonObject.getString("bio")

        userNameText.text = username
        bioText.text = bio
    }

    fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                val mimeType = contentResolver.getType(uri)
                val imageFormat = when (mimeType) {
                    "image/jpeg" -> "JPEG"
                    "image/png" -> "PNG"
                    else -> null
                }

                if (imageFormat != null) {
                    val imageBytes = contentResolver.openInputStream(uri)?.readBytes()
                    if (imageBytes != null) {
                        webRTCManager.sendImage(imageBytes, imageFormat)
                        statusText.text = "Image sent as $imageFormat"
                    }
                } else {
                    statusText.text = "Unsupported image format"
                }
            }
        }
    }


    override fun onMessageReceived(message: String) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        webRTCManager.disconnect() // Disconnect the signaling channel when the activity is destroyed
    }
}
