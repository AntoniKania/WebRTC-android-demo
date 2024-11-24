package com.example.webrtc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.DataChannel.Buffer
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class WebRTCManager(val context: Context, signalingServerUrl: String, token: String) {
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel
    private var signalingChannel: SignalingChannel
    private var receivedImageStream: ByteArrayOutputStream

    interface WebRTCListener {
        fun onImageReceived(imageBitmap: Bitmap)
        fun onJsonRecived(json: String)
    }

    private var listener: WebRTCListener? = null

    init {
        signalingChannel = SignalingChannel(
            peerId = "id-peer-1",
            peerType = "type-emulator",
            signalingServerUrl = signalingServerUrl,
            token = token,
            webRTCManager = this // Pass this instance to the signaling channel
        )

        receivedImageStream = ByteArrayOutputStream()
    }

    fun initialize() {
        // Initialize WebRTC
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val factoryOptions = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .createPeerConnectionFactory()

        // Create peer connection with ICE servers
        val iceServers = listOf(
//            PeerConnection.IceServer.builder("stun:stun.modulus.gr:3478?transport=tcp").createIceServer()
//            PeerConnection.IceServer.builder("stun:stun.voys.nl:3478?transport=tcp").createIceServer()
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:443").createIceServer()
//            PeerConnection.IceServer.builder("stun.pjsip.org:3478").createIceServer(),
//            PeerConnection.IceServer.builder("stun.noc.ams-ix.net:3478").createIceServer(),
//            PeerConnection.IceServer.builder("stun.nonoh.net:3478").createIceServer(),
//            PeerConnection.IceServer.builder("stun.nottingham.ac.uk:3478").createIceServer(),
//            PeerConnection.IceServer.builder("stun.phone.com:3478").createIceServer(),
//            PeerConnection.IceServer.builder("stun.poivy.com:3478").createIceServer()
//            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer()
//            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
//            PeerConnection.IceServer.builder("turn:turn01.hubl.in?transport=udp").createIceServer(),
//            PeerConnection.IceServer.builder("turn:turn02.hubl.in?transport=tcp").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        peerConnection.addIceCandidate(it)
                        signalingChannel.sendIceCandidate(it)
                    }
                }

                override fun onDataChannel(dc: DataChannel?) {
                    dataChannel = dc!!
//                    setupDataChannel()
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {
                    TODO("Not yet implemented")
                }

                override fun onAddStream(mediaStream: MediaStream?) {}
                override fun onRemoveStream(mediaStream: MediaStream?) {}
                override fun onRenegotiationNeeded() {}
                override fun onTrack(rtpTransceiver: RtpTransceiver?) {}
            })!!

        setupDataChannel()
    }

    fun setWebRTCListener(listener: WebRTCListener) {
        this.listener = listener
    }

    fun createOffer() {
        val offerOptions = MediaConstraints()
        offerOptions.mandatory.add(
            MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo",
                "true"
            )
        )
        offerOptions.mandatory.add(
            MediaConstraints.KeyValuePair(
                "offerToReceiveAudio",
                "true"
            )
        )
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        signalingChannel.sendOffer(sessionDescription?.description ?: "")
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sessionDescription)
            }

            override fun onSetSuccess() {
                TODO("Not yet implemented")
            }

            override fun onCreateFailure(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onSetFailure(p0: String?) {
                TODO("Not yet implemented")
            }
        }, offerOptions)
    }

        fun createAnswer(senderPeerId: String) {
            val answerOptions = MediaConstraints()
            answerOptions.mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    "true"
                )
            )
            answerOptions.mandatory.add(
                MediaConstraints.KeyValuePair(
                    "offerToReceiveAudio",
                    "true"
                )
            )
            peerConnection.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                    peerConnection.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            signalingChannel.sendAnswer(senderPeerId, sessionDescription?.description ?: "")
                        }

                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, sessionDescription)
                }

                override fun onSetSuccess() {
                    TODO("Not yet implemented")
                }

                override fun onCreateFailure(p0: String?) {
                        TODO("Not yet implemented")
                }

                override fun onSetFailure(p0: String?) {
                    TODO("Not yet implemented")
                }
            }, answerOptions)
        }

    fun setRemoteDescription(sdp: String, type: SessionDescription.Type) {
        val sessionDescription = SessionDescription(type, sdp)
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sessionDescription)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection.addIceCandidate(candidate) // Add received ICE candidate
    }

    fun setupDataChannel() {
        val init = DataChannel.Init()
        dataChannel = peerConnection.createDataChannel("fileTransfer", init)

        dataChannel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {}
            override fun onStateChange() {
                if (dataChannel.state() == DataChannel.State.OPEN) {
                    // DataChannel is ready to send data
                }
            }

            private val receivedDataBuffer = ByteArrayOutputStream()

            override fun onMessage(buffer: DataChannel.Buffer) {
                Log.i("onMessage", "inOnMessageMethod")
                // Append incoming data chunk to buffer
                val chunk = ByteArray(buffer.data.remaining()).apply { buffer.data.get(this) }
                receivedDataBuffer.write(chunk)

                // Convert buffer to byte array for easier processing
                val data = receivedDataBuffer.toByteArray()

                // Check for JPEG end marker (0xFF, 0xD9)
                if (data.isJpegComplete()) {
                    handleImageData(data, "JPEG")
                    receivedDataBuffer.reset()
                }
                // Check for PNG end marker (IEND chunk)
                else if (data.isPngComplete()) {
                    handleImageData(data, "PNG")
                    receivedDataBuffer.reset()
                }
                // Check for JSON end marker ("<END_JSON>")
                else if (data.endsWithJsonMarker()) {
                    handleJsonData(data)
                    receivedDataBuffer.reset()
                }
            }

            // Handle image data after transmission completes
            private fun handleImageData(data: ByteArray, format: String) {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                notifyImageReceived(bitmap)
            }

            // Handle JSON data after transmission completes
            private fun handleJsonData(data: ByteArray) {
                val completeJsonString = String(data, Charsets.UTF_8).removeSuffix("<END_JSON>")
                val jsonObject = JSONObject(completeJsonString)
                val username = jsonObject.optString("username", "Unknown")
                val bio = jsonObject.optString("bio", "No bio available")

                notifyJsonReceived(completeJsonString)
            }

            // Helper functions for markers
            fun ByteArray.isJpegComplete() = this.size >= 2 && this[this.size - 2] == 0xFF.toByte() && this[this.size - 1] == 0xD9.toByte()
            fun ByteArray.isPngComplete() = this.size >= 8 && this.sliceArray(this.size - 8 until this.size).contentEquals(
                byteArrayOf(0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte())
            )
            fun ByteArray.endsWithJsonMarker() = this.toString(Charsets.UTF_8).endsWith("<END_JSON>")
        })
    }

    fun sendJson(json: String) {
//        val jsonObject = JSONObject(json)
//        jsonObject.put("type", "profile_data")

        val jsonWithEndMarker = "$json<END_JSON>"
        val jsonBytes = jsonWithEndMarker.toByteArray(Charsets.UTF_8)
        val chunkSize = 1024
        for (i in jsonBytes.indices step chunkSize) {
            val chunk = jsonBytes.copyOfRange(i, minOf(i + chunkSize, jsonBytes.size))
            val buffer = ByteBuffer.wrap(chunk)
            dataChannel.send(DataChannel.Buffer(buffer, false))
        }
    }

    fun sendData(data: ByteArray) {
        val buffer = Buffer(ByteBuffer.wrap(data), false)
        dataChannel.send(buffer)
    }

    fun sendImage(imageBytes: ByteArray, format: String) {
        // Optionally send format information first
//        val formatMessage = JSONObject().apply {
//            put("type", "image_format")
//            put("format", format)
//        }.toString()
//        dataChannel.send(DataChannel.Buffer(ByteBuffer.wrap(formatMessage.toByteArray()), true))

        // Proceed to send the image data in chunks
        Log.i("Sending Image", "peerConnectionState: " + peerConnection.connectionState())
        Log.i("Sending Image", "datachannelState: " + dataChannel.state())
        val chunkSize = 1024
        for (i in imageBytes.indices step chunkSize) {
            val chunk = imageBytes.copyOfRange(i, minOf(i + chunkSize, imageBytes.size))
            val buffer = ByteBuffer.wrap(chunk)
            dataChannel.send(DataChannel.Buffer(buffer, false))
        }
    }


    fun sendImage() {
        // Load the image from resources or assets as a Bitmap
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.cat)
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Compress the Bitmap to a byte array (JPEG format in this example)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()

        // Split and send the image in chunks if necessary
        val chunkSize = 1024  // Adjust this based on your needs and the channel's buffering capacity
        for (i in imageBytes.indices step chunkSize) {
            val chunk = imageBytes.copyOfRange(i, minOf(i + chunkSize, imageBytes.size))
            val buffer = ByteBuffer.wrap(chunk)
            dataChannel.send(Buffer(buffer, false))
        }
    }

    private fun notifyImageReceived(imageBitmap: Bitmap) {
        listener?.onImageReceived(imageBitmap)
    }

    private fun notifyJsonReceived(imageBitmap: String) {
        listener?.onJsonRecived(imageBitmap)
    }

    fun sendFile(fileBytes: ByteArray) {
        sendData(fileBytes)
    }

    fun connect() {
        signalingChannel.connect()
    }

    fun disconnect() {
        signalingChannel.disconnect()
    }
}
