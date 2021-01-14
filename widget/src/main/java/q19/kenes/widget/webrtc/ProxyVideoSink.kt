package q19.kenes.widget.webrtc

import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import q19.kenes.widget.util.Logger

class ProxyVideoSink(private val tag: String = "ProxyVideoSink") : VideoSink {
    private var target: VideoSink? = null

    @Synchronized
    override fun onFrame(frame: VideoFrame?) {
        if (target == null) {
            Logger.debug(tag, "Dropping frame in proxy because target is null.")
            return
        }
        target?.onFrame(frame)
    }

    @Synchronized
    fun setTarget(target: VideoSink?) {
        this.target = target
    }
}