package q19.kenes_widget.webrtc

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection.IceServer
import org.webrtc.SessionDescription

/**
 * Struct holding the signaling parameters of an AppRTC room.
 */
internal class SignalingParams(
    val iceServers: List<IceServer>,
    val initiator: Boolean,
    val clientId: String?,
    val wssUrl: String?,
    val wssPostUrl: String?,
    val offerSdp: SessionDescription?,
    val iceCandidates: List<IceCandidate>?
)