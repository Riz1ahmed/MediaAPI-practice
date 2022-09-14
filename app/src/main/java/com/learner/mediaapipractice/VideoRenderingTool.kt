package com.learner.mediaapipractice

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface

class VideoRenderingTool() {
    private lateinit var encoder: MediaCodec // Also will help you to create the inputSurface
    private lateinit var inputSurface: Surface //It will receive data/frame from EGL and render.

    private lateinit var eglDisplay: EGLDisplay
    private lateinit var eglSurface: EGLSurface

    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var outputSurface: Surface

    // OnFrameAvailable Callback is called from a different thread than
    // our OpenGL rendering thread, so we need some synchronization
    private val lock = Object()

    // Signalizes when a new decoded frame is available as texture
    // for OpenGL rendering
    @Volatile
    private var frameAvailable = false


    fun initEds() {
        initEncoder()
        initEGL()
        initDecoderAndOutputSurface()


        // Finish decoder configuration with our outputSurface
        /*decoder = MediaCodec.createDecoderByType(inFormat.getString(MediaFormat.KEY_MIME))
        decoder.configure(inputFormat, outputSurface, null, 0)*/
    }


    private fun initEncoder() {
        /** Configure video output format - here you can adjust values according to input Format
        that you've got from MediaExtractor in previous section */
        val mime = "video/avc"
        //This size must be support by android. Link: https://developer.android.com/guide/topics/media/media-formats#video-codecs
        val width = 320;
        val height = 180
        val outVFormat = MediaFormat.createVideoFormat(mime, width, height)
        val colorFormatSurface = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface

        outVFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormatSurface)
        outVFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
        outVFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        outVFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 15)
        outVFormat.setString(MediaFormat.KEY_MIME, mime)

        // Init encoder
        encoder = MediaCodec.createEncoderByType(outVFormat.getString(MediaFormat.KEY_MIME)!!)
        encoder.configure(outVFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()//Create a input surface
    }

    private fun initEGL() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY)
            throw RuntimeException(
                "eglDisplay == EGL14.EGL_NO_DISPLAY: " + GLUtils.getEGLErrorString(EGL14.eglGetError())
            )

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1))
            throw RuntimeException("eglInitialize(): " + GLUtils.getEGLErrorString(EGL14.eglGetError()))

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGLExt.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val nConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs,
                0, configs.size, nConfigs, 0
            )
        )
            throw RuntimeException(GLUtils.getEGLErrorString(EGL14.eglGetError()))

        var err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        val ctxAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        val eglContext =
            EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)

        err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)


        eglSurface =
            EGL14.eglCreateWindowSurface(eglDisplay, configs[0], inputSurface, surfaceAttribs, 0)

        err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext))
            throw RuntimeException("eglMakeCurrent(): " + GLUtils.getEGLErrorString(EGL14.eglGetError()))
    }

    /**Must call it after inputSurface initialize. Because it use inputSurface*/
    private fun initDecoderAndOutputSurface() {
        // Prepare a texture handle for SurfaceTexture
        val textureHandles = IntArray(1)
        GLES20.glGenTextures(1, textureHandles, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureHandles[0])

        surfaceTexture = SurfaceTexture(textureHandles[0])

        // The onFrameAvailable() callback will be called from our HandlerThread
        val thread = HandlerThread("FrameHandlerThread")
        thread.start()

        surfaceTexture.setOnFrameAvailableListener({
            synchronized(lock) {
                // New frame available before the last frame was process...we dropped some frames
                if (frameAvailable) Log.d(
                    TAG,
                    "Frame available before the last frame was process...we dropped some frames"
                )

                frameAvailable = true
                lock.notifyAll()
            }
        }, Handler(thread.looper))

        outputSurface = Surface(surfaceTexture)
    }

    companion object {
        const val TAG = "xyz"
    }
}