package com.example.webrtc

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class SignalingChannel(
    private val peerId: String,
    private val peerType: String,
    signalingServerUrl: String,
    token: String,
    private val verbose: Boolean = false
) {

    private lateinit var socket: Socket

    // Initialize the socket connection with the signaling server
    init {
        val opts = IO.Options().apply {
            auth = mapOf("token" to token)
            reconnection = true   // Enable reconnection
        }

        socket = IO.socket(signalingServerUrl, opts)
        resetListeners()
    }

    // Connect to the signaling server
    fun connect() {
        updateListeners()
        socket.connect()
    }

    // Send a message to all peers
    fun send(payload: JSONObject) {
        val message = JSONObject().apply {
            put("from", peerId)
            put("target", "all")
            put("payload", payload)
        }
        socket.emit("message", message)
    }

    // Send a message to a specific peer by their ID
    fun sendTo(targetPeerId: String, payload: JSONObject) {
        val message = JSONObject().apply {
            put("from", peerId)
            put("target", targetPeerId)
            put("payload", payload)
        }
        socket.emit("messageOne", message)
    }

    // Disconnect from the signaling server and reset listeners
    fun disconnect() {
        resetListeners()
        socket.disconnect()
    }

    // Update listeners after resetting them
    private fun updateListeners() {
        socket.on(Socket.EVENT_CONNECT, onConnect)
        socket.on(Socket.EVENT_DISCONNECT, onDisconnect)
        socket.on(Socket.EVENT_CONNECT_ERROR, onError)
        socket.on("message", onMessage)
        socket.on("uniquenessError", onUniquenessError)
    }

    // Reset listeners and set default callback behaviors
    private fun resetListeners() {
        socket.off(Socket.EVENT_CONNECT, onConnect)
        socket.off(Socket.EVENT_DISCONNECT, onDisconnect)
        socket.off(Socket.EVENT_CONNECT_ERROR, onError)
        socket.off("message", onMessage)
        socket.off("uniquenessError", onUniquenessError)
    }

    // Event handlers
    private val onConnect = Emitter.Listener {
//        if (verbose) Log.d("SignalingChannel", "Connected to signaling server with id ${socket.id()}")
        socket.emit("ready", peerId, peerType)
    }

    private val onDisconnect = Emitter.Listener {
//        if (verbose) Log.d("SignalingChannel", "Disconnected from signaling server")
    }

    private val onError = Emitter.Listener { args ->
//        if (verbose) Log.e("SignalingChannel", "Signaling Server ERROR: ${args[0]}")
    }

    private val onReconnect = Emitter.Listener { args ->
//        if (verbose) Log.d("SignalingChannel", "Reconnected to signaling server after ${args[0]} attempts")
    }

    private val onMessage = Emitter.Listener { args ->
//        if (verbose) Log.d("SignalingChannel", "Received message: ${args[0]}")
    }

    private val onUniquenessError = Emitter.Listener { args ->
//        Log.e("SignalingChannel", "UniquenessError: ${args[0]}")
        throw Exception("Uniqueness error: ${args[0]}")
    }
}
