package com.example.webrtc

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SignalingChannel(
    private val peerId: String,
    private val peerType: String,
    signalingServerUrl: String,
    token: String,
    private val verbose: Boolean = false,
    private val webRTCManager: WebRTCManager
) {
    interface Listener {
        fun onPeersUpdated(peersList: List<String>)
        fun onMessageReceived(message: String)
    }

    private lateinit var socket: Socket
    private var listener: Listener? = null

    init {
        val opts = IO.Options().apply {
            auth = mapOf("token" to token)
            reconnection = true
        }

        socket = IO.socket(signalingServerUrl, opts)
        resetListeners()
    }

    fun connect() {
        updateListeners()
        socket.connect()
    }

    fun send(payload: JSONObject) {
        val message = JSONObject().apply {
            put("from", peerId)
            put("target", "all")
            put("payload", payload)
        }
        socket.emit("message", message)
    }

    fun sendTo(targetPeerId: String, payload: JSONObject) {
        val message = JSONObject().apply {
            put("from", peerId)
            put("target", targetPeerId)
            put("payload", payload)
        }
        socket.emit("messageOne", message)
    }

    fun disconnect() {
        resetListeners()
        socket.disconnect()
    }

    private fun updateListeners() {
        socket.on(Socket.EVENT_CONNECT, onConnect)
        socket.on(Socket.EVENT_DISCONNECT, onDisconnect)
        socket.on(Socket.EVENT_CONNECT_ERROR, onError)
        socket.on("message", onMessage)
        socket.on("uniquenessError", onUniquenessError)
    }

    private fun resetListeners() {
        socket.off(Socket.EVENT_CONNECT, onConnect)
        socket.off(Socket.EVENT_DISCONNECT, onDisconnect)
        socket.off(Socket.EVENT_CONNECT_ERROR, onError)
        socket.off("message", onMessage)
        socket.off("uniquenessError", onUniquenessError)
    }


    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private val onConnect = Emitter.Listener {
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

    private val onUniquenessError = Emitter.Listener { args ->
//        Log.e("SignalingChannel", "UniquenessError: ${args[0]}")
        throw Exception("Uniqueness error: ${args[0]}")
    }

    private val onMessage = Emitter.Listener { args ->
        val message = args[0] as JSONObject
        val target = message.getString("target")

        if (target == peerId || target == "all") {
            val payload = message.getJSONObject("payload")
            if (payload.has("type")) {
                val type = payload.getString("type")

                when (type) {
                    "offer" -> {
                        val sdp = payload.getString("sdp")
                        webRTCManager.setRemoteDescription(sdp, SessionDescription.Type.OFFER) // WebRTCManager handles this
                        val sender = message.getString("from")
                                webRTCManager.createAnswer(sender)
                    }

                    "answer" -> {
                        val sdp = payload.getString("sdp")
                        webRTCManager.setRemoteDescription(sdp, SessionDescription.Type.ANSWER)
                    }

                    "ice" -> {
                        val candidate = payload.getString("candidate")
                        val sdpMLineIndex = payload.getString("sdpMLineIndex")
                        webRTCManager.addIceCandidate(IceCandidate(sdpMLineIndex, 0, candidate))
                    }
                }
            }
            if (payload.has("connections")) {
                Log.i("WebSocket", "Connected and received connections")
                val connections = payload.getJSONArray("connections")
                val peersList = mutableListOf<String>()
                for (i in 0 until connections.length()) {
                    peersList.add(connections.getString(i))
                }

                // Call the method to update UI with the new peers list
                listener?.onPeersUpdated(peersList)
//                webRTCManager.createOffer() // uncomment for the second peer joining
            }
        }
    }

    fun sendOffer(sdp: String) {
        val payload = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp)
        }
        send(payload)
    }

    fun sendAnswer(targetPeerId: String, sdp: String) {
        val payload = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp)
        }
        sendTo(targetPeerId, payload)
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val payload = JSONObject().apply {
            put("type", "ice")
            put("candidate", candidate.sdp)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }
        send(payload)
    }
}
