package q19.kenes_widget.webrtc

import android.util.Log
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

internal class ProxyVideoSink(private val tag: String = "ProxyVideoSink") : VideoSink {
    private var target: VideoSink? = null

    @Synchronized
    override fun onFrame(frame: VideoFrame?) {
        if (target == null) {
            Log.d(tag, "Dropping frame in proxy because target is null.")
            return
        }
        target?.onFrame(frame)
    }

    @Synchronized
    fun setTarget(target: VideoSink?) {
        this.target = target
    }
}