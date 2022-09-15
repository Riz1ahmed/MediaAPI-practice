package com.learner.mediaapipractice

import android.graphics.SurfaceTexture
import android.media.*
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.FileDescriptor
import java.security.InvalidParameterException

/***
 * Extractor: Extract data from Media (video) file
 * Decoder: Pass extracted/decoded data to OpenGL. It's like Unpacking
 * Encoder: Pass data from OpenGL to Muxer. It's like packing
 * Muxer: Create a final video file from Encoded data
 *
 * Texture: Where draw anything
 */
class VideoRenderingTool() {
    //region Media instance


    //extract Tracks & Frames and infos from Media(here Video) file
    private lateinit var extractor: MediaExtractor

    //use the extractor Decoded from and pass them to EGL as texture.
    private lateinit var decoder: MediaCodec
    private lateinit var outputSurface: Surface

    private lateinit var encoder: MediaCodec // Will help you to create the inputSurface
    private lateinit var inputSurface: Surface //It will receive data/frame from EGL and render.

    private lateinit var eglDisplay: EGLDisplay
    private lateinit var eglSurface: EGLSurface


    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var muxer: MediaMuxer

    private val mediaCodedTimeoutUs = 10000L

    //This size have to be support by android. Link: https://developer.android.com/guide/topics/media/media-formats#video-codecs
    private val vdoReg = Size(320, 180)
    private val mime = "video/avc"
    private val outFormat = MediaFormat.createVideoFormat(mime, vdoReg.width, vdoReg.height).also {
        val colorFormatSurface = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        it.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormatSurface)
        it.setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
        it.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        it.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 15)
        it.setString(MediaFormat.KEY_MIME, mime)
    }
    //endregion


    //region Precessing instances

    // OpenGL transformation applied to UVs of the texture that holds the decoded frame
    private val texMatrix = FloatArray(16)

    // These control the state of video processing
    private var allInputExtracted = false
    private var allInputDecoded = false
    private var allOutputEncoded = false

    // OnFrameAvailable Callback is called from a different thread than
    // our OpenGL rendering thread, so we need some synchronization
    private val lock = Object()

    // Signalizes when a new decoded frame is available as texture
    // for OpenGL rendering
    @Volatile
    private var frameAvailable = false
    //endregion

    fun starProcessing(outPath: String, fd: FileDescriptor) {
        val inFormat = initExtractorAndGetVideFormat(fd)
        initEncoderInputSurface()
        initEGL()
        initDecoderAndOutputSurface(inFormat)
        initMuxer(outPath)

        startMachine()
    }

    private fun startMachine() {
        encoder.start()
        decoder.start()

        //the extracting, decoding, editing, encoding, and muxing
        //var allInputExtracted = false
        var allInputDecoded = false
        var allOutputEncoded = false

        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1

        while (!allOutputEncoded) {
            // Feed input to decoder
            if (!allInputExtracted) feedInputToDecoder()

            var encoderOutputAvailable = true
            var decoderOutputAvailable = !allInputDecoded

            while (encoderOutputAvailable || decoderOutputAvailable) {
                // Drain Encoder & mux to output file first
                val outBufferId = encoder.dequeueOutputBuffer(bufferInfo, mediaCodedTimeoutUs)

                if (outBufferId >= 0) {
                    val encodedBuffer = encoder.getOutputBuffer(outBufferId)
                    muxer.writeSampleData(trackIndex, encodedBuffer!!, bufferInfo)
                    encoder.releaseOutputBuffer(outBufferId, false)

                    // Are we finished here?
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        allOutputEncoded = true
                        break
                    }
                } else if (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    encoderOutputAvailable = false
                } else if (outBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                }

                if (outBufferId != MediaCodec.INFO_TRY_AGAIN_LATER) continue

                // Get output from decoder and feed it to encoder
                if (!allInputDecoded) {
                    decoderOutputAvailable = feedEncoderToDecoder(bufferInfo)
                    /*val outBufferId1 = decoder.dequeueOutputBuffer(bufferInfo, mediaCodedTimeoutUs)
                    if (outBufferId1 >= 0) {
                        val render = bufferInfo.size > 0
                        // Give the decoded frame to SurfaceTexture (onFrameAvailable() callback should
                        // be called soon after this)
                        decoder.releaseOutputBuffer(outBufferId1, render)
                        if (render) {
                            // Wait till new frame available after onFrameAvailable has been called
                            synchronized(lock) {
                                while (!frameAvailable) {
                                    lock.wait(500)
                                    if (!frameAvailable) Log.e(TAG, "Surface frame wait timed out")
                                }
                                frameAvailable = false
                            }

                            surfaceTexture.updateTexImage()
                            surfaceTexture.getTransformMatrix(texMatrix)

                            // Render texture with OpenGL ES
                            // ...

                            EGLExt.eglPresentationTimeANDROID(
                                eglDisplay, eglSurface, bufferInfo.presentationTimeUs * 1000
                            )

                            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                        }

                        // Did we get all output from decoder?
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            allInputDecoded = true
                            encoder.signalEndOfInputStream()
                        }

                    }

                    else if (outBufferId1 == MediaCodec.INFO_TRY_AGAIN_LATER)
                        decoderOutputAvailable = false*/
                }
            }
        }
    }

    private fun feedEncoderToDecoder(bufferInfo: MediaCodec.BufferInfo): Boolean {
        val outBufferId = decoder.dequeueOutputBuffer(bufferInfo, mediaCodedTimeoutUs)
        if (outBufferId >= 0) {
            val render = bufferInfo.size > 0
            // Give the decoded frame to SurfaceTexture (onFrameAvailable() callback should
            // be called soon after this)
            decoder.releaseOutputBuffer(outBufferId, render)
            if (render) {
                // Wait till new frame available after onFrameAvailable has been called
                synchronized(lock) {
                    while (!frameAvailable) {
                        lock.wait(500)
                        if (!frameAvailable) Log.e(TAG, "Surface frame wait timed out")
                    }
                    frameAvailable = false
                }

                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(texMatrix)

                // Render texture with OpenGL ES
                // ...

                EGLExt.eglPresentationTimeANDROID(
                    eglDisplay, eglSurface, bufferInfo.presentationTimeUs * 1000
                )
                EGL14.eglSwapBuffers(eglDisplay, eglSurface)
            }
            // Did we get all output from decoder?
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                allInputDecoded = true
                encoder.signalEndOfInputStream()
            }
        }
        return outBufferId != MediaCodec.INFO_TRY_AGAIN_LATER
    }

    private fun feedInputToDecoder() {
        val inBufferId = decoder.dequeueInputBuffer(mediaCodedTimeoutUs)
        if (inBufferId >= 0) {
            val buffer = decoder.getInputBuffer(inBufferId)
            val sampleSize = extractor.readSampleData(buffer!!, 0)

            if (sampleSize >= 0) {
                decoder.queueInputBuffer(
                    inBufferId, 0, sampleSize, extractor.sampleTime, extractor.sampleFlags
                )
                extractor.advance()
            } else {
                decoder.queueInputBuffer(
                    inBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                allInputExtracted = true
            }
        }
    }

    //region Initialization
    private fun initExtractorAndGetVideFormat(fd: FileDescriptor): MediaFormat {
        // Init extractor
        extractor = MediaExtractor()
        extractor.setDataSource(fd)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)!!.startsWith("video/")) {
                extractor.selectTrack(i)
                return format
            }
        }
        throw InvalidParameterException("File contains no video track")
    }

    /**Init:
     * [encoder]: Create encoder from output mimeType
     * [inputSurface]: create from [encoder] by createInputSurface()
     */
    private fun initEncoderInputSurface() {
        // Init encoder
        encoder = MediaCodec.createEncoderByType(outFormat.getString(MediaFormat.KEY_MIME)!!)
        encoder.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
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

    /** Must call it after [inputSurface] initialize. Because it use [inputSurface]
     * @param inFormat is input Video's MediaFormat
     */
    private fun initDecoderAndOutputSurface(inFormat: MediaFormat) {
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
                val msg =
                    "Frame available before the last frame was process...we dropped some frames"
                if (frameAvailable) Log.d(TAG, msg)

                frameAvailable = true
                lock.notifyAll()
            }
        }, Handler(thread.looper))

        outputSurface = Surface(surfaceTexture)

        //the inFormat is input Video's MediaFormat
        decoder = MediaCodec.createDecoderByType(inFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder = MediaCodec.createDecoderByType(mime)//Currently I used same (out mime)
        decoder.configure(outFormat, outputSurface, null, 0)
    }

    private fun initMuxer(outPath: String) {
        muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    //endregion
    companion object {
        const val TAG = "xyz"
    }
}