package q19.kenes_widget.webrtc

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.StatsReport

/**
 * Peer connection events.
 */
internal interface PeerConnectionEvents {
    /**
     * Callback fired once local SDP is created and set.
     */
    fun onLocalDescription(sdp: SessionDescription)

    /**
     * Callback fired once local Ice candidate is generated.
     */
    fun onIceCandidate(candidate: IceCandidate)

    /**
     * Callback fired once local ICE candidates are removed.
     */
    fun onIceCandidatesRemoved(candidates: Array<IceCandidate>)

    /**
     * Callback fired once connection is established ([PeerConnection.IceConnectionState] is
     * [PeerConnection.IceConnectionState.CONNECTED]).
     */
    fun onIceConnected()

    /**
     * Callback fired once connection is disconnected ([PeerConnection.IceConnectionState] is
     * [PeerConnection.IceConnectionState.DISCONNECTED]).
     */
    fun onIceDisconnected()

    /**
     * Callback fired once DTLS connection is established ([PeerConnection.PeerConnectionState]
     * is [PeerConnection.PeerConnectionState.CONNECTED]).
     */
    fun onConnected()

    /**
     * Callback fired once DTLS connection is disconnected ([PeerConnection.PeerConnectionState]
     * is [PeerConnection.PeerConnectionState.DISCONNECTED]).
     */
    fun onDisconnected()

    /**
     * Callback fired once peer connection is closed.
     */
    fun onPeerConnectionClosed()

    /**
     * Callback fired once peer connection statistics is ready.
     */
    fun onPeerConnectionStatsReady(reports: Array<StatsReport>)

    /**
     * Callback fired once peer connection error happened.
     */
    fun onPeerConnectionError(description: String)
}