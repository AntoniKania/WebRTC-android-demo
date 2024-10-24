package com.example.webrtc

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {

    private lateinit var mSocket: Socket
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)

        // Initialize the WebSocket connection 192.0.2.120
        try {
            mSocket = IO.socket("http://127.0.0.1:3000")  // Your local server IP address
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        // Connect to the WebSocket server when the button is clicked
        connectButton.setOnClickListener {
            connectWebSocket()
        }

        // Handle connection events
        mSocket.on(Socket.EVENT_CONNECT) {
            runOnUiThread {
                statusText.text = "Status: Connected"
            }
        }

        mSocket.on(Socket.EVENT_DISCONNECT) {
            runOnUiThread {
                statusText.text = "Status: Disconnected"
            }
        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) {
            runOnUiThread {
                statusText.text = "Status: Connection Error"
            }
        }
    }

    private fun connectWebSocket() {
        mSocket.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect()  // Disconnect WebSocket when the activity is destroyed
    }
}
