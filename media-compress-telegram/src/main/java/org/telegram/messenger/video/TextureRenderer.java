/*
 * This is the source code of Telegram for Android v. 6.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2020.
 */

package org.telegram.messenger.video;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.telegram.messenger.MediaController;
import org.telegram.ui.Components.FilterShaders;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import chengdu.ws.mediacompress.telegram.BuildConfig;

public class TextureRenderer {
    private static final String TAG = TextureRenderer.class.getSimpleName();

    private final FloatBuffer verticesBuffer;
    private FloatBuffer textureBuffer;
    private final FloatBuffer renderTextureBuffer;
    private FloatBuffer bitmapVerticesBuffer;

    float[] bitmapData = {
            -1.0f, 1.0f,
            1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
    };

    private int transformedWidth;
    private int transformedHeight;

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";

    private static final String FRAGMENT_EXTERNAL_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision highp float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    private final float[] mMVPMatrix = new float[16];
    private final float[] mSTMatrix = new float[16];
    private float[] mSTMatrixIdentity = new float[16];
    private int mTextureID;
    private final int[] mProgram;
    private final int[] muMVPMatrixHandle;
    private final int[] muSTMatrixHandle;
    private final int[] maPositionHandle;
    private final int[] maTextureHandle;

    private boolean blendEnabled;

    private boolean firstFrame = true;

    public TextureRenderer(MediaController.CropState cropState, int w, int h, int rotation) {

        float[] texData = {
                0.f, 0.f,
                1.f, 0.f,
                0.f, 1.f,
                1.f, 1.f,
        };

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "start textureRenderer w = " + w + " h = " + h + " r = " + rotation);
            if (cropState != null) {
                Log.d(TAG, "cropState px = " + cropState.cropPx + " py = " + cropState.cropPy + " cScale = " + cropState.cropScale +
                        " cropRotate = " + cropState.cropRotate + " pw = " + cropState.cropPw + " ph = " + cropState.cropPh +
                        " tw = " + cropState.transformWidth + " th = " + cropState.transformHeight + " tr = " + cropState.transformRotation +
                        " mirror = " + cropState.mirrored);
            }
        }

        textureBuffer = ByteBuffer.allocateDirect(texData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(texData).position(0);

        bitmapVerticesBuffer = ByteBuffer.allocateDirect(bitmapData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        bitmapVerticesBuffer.put(bitmapData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);
        Matrix.setIdentityM(mSTMatrixIdentity, 0);

        transformedWidth = w;
        transformedHeight = h;

        int count = 1;
        mProgram = new int[count];
        muMVPMatrixHandle = new int[count];
        muSTMatrixHandle = new int[count];
        maPositionHandle = new int[count];
        maTextureHandle = new int[count];

        Matrix.setIdentityM(mMVPMatrix, 0);
        int textureRotation = 0;
        if (cropState != null) {
            float[] verticesData = {
                    0, 0,
                    w, 0,
                    0, h,
                    w, h,
            };
            textureRotation = cropState.transformRotation;

            transformedWidth *= cropState.cropPw;
            transformedHeight *= cropState.cropPh;

            float angle = (float) (-cropState.cropRotate * (Math.PI / 180.0f));
            for (int a = 0; a < 4; a++) {
                float x1 = verticesData[a * 2] - w / 2;
                float y1 = verticesData[a * 2 + 1] - h / 2;
                float x2 = (float) (x1 * Math.cos(angle) - y1 * Math.sin(angle) + cropState.cropPx * w) * cropState.cropScale;
                float y2 = (float) (x1 * Math.sin(angle) + y1 * Math.cos(angle) - cropState.cropPy * h) * cropState.cropScale;
                verticesData[a * 2] = x2 / transformedWidth * 2;
                verticesData[a * 2 + 1] = y2 / transformedHeight * 2;
            }
            verticesBuffer = ByteBuffer.allocateDirect(verticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            verticesBuffer.put(verticesData).position(0);
        } else {
            float[] verticesData = {
                    -1.0f, -1.0f,
                    1.0f, -1.0f,
                    -1.0f, 1.0f,
                    1.0f, 1.0f,
            };
            verticesBuffer = ByteBuffer.allocateDirect(verticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            verticesBuffer.put(verticesData).position(0);
        }
        float[] textureData;

        if (textureRotation == 90) {
            textureData = new float[]{
                    1.f, 0.f,
                    1.f, 1.f,
                    0.f, 0.f,
                    0.f, 1.f
            };
        } else if (textureRotation == 180) {
            textureData = new float[]{
                    1.f, 1.f,
                    0.f, 1.f,
                    1.f, 0.f,
                    0.f, 0.f
            };
        } else if (textureRotation == 270) {
            textureData = new float[]{
                    0.f, 1.f,
                    0.f, 0.f,
                    1.f, 1.f,
                    1.f, 0.f
            };
        } else {
            textureData = new float[]{
                    0.f, 0.f,
                    1.f, 0.f,
                    0.f, 1.f,
                    1.f, 1.f
            };
        }
        if (cropState != null && cropState.mirrored) {
            for (int a = 0; a < 4; a++) {
                if (textureData[a * 2] > 0.5f) {
                    textureData[a * 2] = 0.0f;
                } else {
                    textureData[a * 2] = 1.0f;
                }
            }
        }
        renderTextureBuffer = ByteBuffer.allocateDirect(textureData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        renderTextureBuffer.put(textureData).position(0);
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void drawFrame(SurfaceTexture st) {
        st.getTransformMatrix(mSTMatrix);
        if (BuildConfig.DEBUG && firstFrame) {
            StringBuilder builder = new StringBuilder();
            for (float stMatrix : mSTMatrix) {
                builder.append(stMatrix).append(", ");
            }
            Log.d(TAG, "stMatrix = " + builder);
            firstFrame = false;
        }

        if (blendEnabled) {
            GLES20.glDisable(GLES20.GL_BLEND);
            blendEnabled = false;
        }

        int texture = mTextureID;
        int index = 0;
        int target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

        GLES20.glUseProgram(mProgram[index]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(target, texture);

        GLES20.glVertexAttribPointer(maPositionHandle[index], 2, GLES20.GL_FLOAT, false, 8, verticesBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle[index]);
        GLES20.glVertexAttribPointer(maTextureHandle[index], 2, GLES20.GL_FLOAT, false, 8, renderTextureBuffer);
        GLES20.glEnableVertexAttribArray(maTextureHandle[index]);

        GLES20.glUniformMatrix4fv(muSTMatrixHandle[index], 1, false, mSTMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle[index], 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFinish();
    }

    @SuppressLint("WrongConstant")
    public void surfaceCreated() {
        for (int a = 0; a < mProgram.length; a++) {
            mProgram[a] = createProgram(VERTEX_SHADER, a == 0 ? FRAGMENT_EXTERNAL_SHADER : FRAGMENT_SHADER);
            maPositionHandle[a] = GLES20.glGetAttribLocation(mProgram[a], "aPosition");
            maTextureHandle[a] = GLES20.glGetAttribLocation(mProgram[a], "aTextureCoord");
            muMVPMatrixHandle[a] = GLES20.glGetUniformLocation(mProgram[a], "uMVPMatrix");
            muSTMatrixHandle[a] = GLES20.glGetUniformLocation(mProgram[a], "uSTMatrix");
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = FilterShaders.loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = FilterShaders.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program == 0) {
            return 0;
        }
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, pixelShader);
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public void release() {
    }

    public void changeFragmentShader(String fragmentExternalShader, String fragmentShader) {
        GLES20.glDeleteProgram(mProgram[0]);
        mProgram[0] = createProgram(VERTEX_SHADER, fragmentExternalShader);
        if (mProgram.length > 1) {
            mProgram[1] = createProgram(VERTEX_SHADER, fragmentShader);
        }

    }
}
