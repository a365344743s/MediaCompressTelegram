package org.telegram.ui.Components;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import chengdu.ws.telegram.BuildConfig;

public class FilterShaders {
    private static final String TAG = FilterShaders.class.getSimpleName();

    public FloatBuffer vertexBuffer;
    public FloatBuffer textureBuffer;
    public FloatBuffer vertexInvertBuffer;

    public FilterShaders() {

        float[] squareCoordinates = {
                -1.0f, 1.0f,
                1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f};

        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoordinates.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoordinates);
        vertexBuffer.position(0);

        float[] squareCoordinates2 = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f};

        bb = ByteBuffer.allocateDirect(squareCoordinates2.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexInvertBuffer = bb.asFloatBuffer();
        vertexInvertBuffer.put(squareCoordinates2);
        vertexInvertBuffer.position(0);

        float[] textureCoordinates = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
        };

        bb = ByteBuffer.allocateDirect(textureCoordinates.length * 4);
        bb.order(ByteOrder.nativeOrder());
        textureBuffer = bb.asFloatBuffer();
        textureBuffer.put(textureCoordinates);
        textureBuffer.position(0);
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                Log.e(TAG, "shader code:\n " + shaderCode);
            }
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }
}
