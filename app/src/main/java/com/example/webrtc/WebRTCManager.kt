package com.example.webrtc

import android.content.Context
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer

class WebRTCManager(val context: Context) {
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel

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
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

//        SignalingChannel

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                // Send candidate to remote peer via signaling
            }

            override fun onDataChannel(dc: DataChannel?) {
                dataChannel = dc!!
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
    }

    fun createOffer() {
        val offerOptions = MediaConstraints()
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        // Send offer SDP to remote peer via signaling
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sessionDescription)
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, offerOptions)
    }

    fun setRemoteDescription(sdp: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sessionDescription)
    }

    fun sendData(data: ByteArray) {
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(data), false)
        dataChannel.send(buffer)
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

            override fun onMessage(buffer: DataChannel.Buffer) {
                // Handle incoming file chunks
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                // Process received data (store file chunk, etc.)
            }
        })
    }

    fun sendFile(fileBytes: ByteArray) {
        sendData(fileBytes)
    }
}