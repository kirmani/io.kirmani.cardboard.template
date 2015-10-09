/*
 * CardboardObject.java
 * Copyright (C) 2015 sean <sean@wireless-10-147-155-193.public.utexas.edu>
 *
 * Distributed under terms of the MIT license.
 */

package io.kirmani.cardboard;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

public class CardboardObject {
    private static final String TAG = "CardboardObject";

    protected static final float Z_NEAR = 0.1f;
    protected static final float Z_FAR = 100.0f;
    protected static final int COORDS_PER_VERTEX = 3;

    private final float[] mLightPosInEyeSpace = new float[4];

    private CardboardScene mScene;

    private Context mContext;

    private FloatBuffer mVertices;
    private FloatBuffer mColors;
    private FloatBuffer mNormals;

    private int mProgram;

    private int mPositionParam;
    private int mNormalParam;
    private int mColorParam;
    private int mModelParam;
    private int mModelViewParam;
    private int mModelViewProjectionParam;
    private int mLightPosParam;

    private float[] mModel;

    public CardboardObject(Context context, CardboardScene scene) {
        mContext = context;
        mScene = scene;
    }

    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    /**
     * Creates the buffers we use to store information about the 3D world.
     *
     * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    public void onSurfaceCreated(EGLConfig config) {
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    public void onNewFrame(HeadTransform headTransform) {
        checkGLError("onReadyToDraw");
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    public void onDrawEye(Eye eye) {}

    /**
     * Called when the Cardboard trigger is pulled.
     */
    public void onCardboardTrigger() {
    }

    protected Context getContext() {
        return mContext;
    }

    protected void setModel(float[] model) {
        mModel = model;
    }

    protected float[] getModel() {
        return mModel;
    }

    protected float[] getModelView() {
        return mScene.getModelView();
    }

    protected float[] getModelViewProjection() {
        return mScene.getModelViewProjection();
    }

    protected float[] getView() {
        return mScene.getView();
    }

    protected void setVertices(FloatBuffer vertices) {
        mVertices = vertices;
    }

    protected FloatBuffer getVertices() {
        return mVertices;
    }

    protected void setColors(FloatBuffer colors) {
        mColors = colors;
    }

    protected FloatBuffer getColors() {
        return mColors;
    }

    protected void setNormals(FloatBuffer normals) {
        mNormals = normals;
    }

    protected FloatBuffer getNormals() {
        return mNormals;
    }

    protected void setProgram(int program) {
        mProgram = program;
    }

    protected int getProgram() {
        return mProgram;
    }

    protected void setPositionParam(int positionParam) {
        mPositionParam = positionParam;
    }

    protected int getPositionParam() {
        return mPositionParam;
    }

    protected void setNormalParam(int normalParam) {
        mNormalParam = normalParam;
    }

    protected int getNormalParam() {
        return mNormalParam;
    }

    protected void setColorParam(int colorParam) {
        mColorParam = colorParam;
    }

    protected int getColorParam() {
        return mColorParam;
    }

    protected void setModelParam(int modelParam) {
        mModelParam = modelParam;
    }

    protected int getModelParam() {
        return mModelParam;
    }

    protected void setModelViewParam(int modelViewParam) {
        mModelViewParam = modelViewParam;
    }

    protected int getModelViewParam() {
        return mModelViewParam;
    }

    protected void setModelViewProjectionParam(int modelViewProjectionParam) {
        mModelViewProjectionParam = modelViewProjectionParam;
    }

    protected int getModelViewProjectionParam() {
        return mModelViewProjectionParam;
    }

    protected void setLightPosParam(int lightPosParam) {
        mLightPosParam = lightPosParam;
    }

    protected int getLightPosParam() {
        return mLightPosParam;
    }

    protected float[] getLightPosInEyeSpace() {
        return mLightPosInEyeSpace;
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    protected static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    protected int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private String readRawTextFile(int resId) {
        InputStream inputStream = mContext.getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

