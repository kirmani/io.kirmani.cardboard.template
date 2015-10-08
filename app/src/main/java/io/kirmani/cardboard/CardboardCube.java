/*
 * CardboardCube.java
 * Copyright (C) 2015 sean <sean@wireless-10-147-155-193.public.utexas.edu>
 *
 * Distributed under terms of the MIT license.
 */

package io.kirmani.cardboard;

import android.app.Activity;
import android.content.Context;
import android.opengl.Matrix;
import android.opengl.GLES20;
import android.os.Vibrator;
import android.util.Log;

import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

public class CardboardCube extends CardboardObject {
    private static final String TAG = "CardboardCube";

    private FloatBuffer mFoundColors;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    private static final int COORDS_PER_VERTEX = 3;

    private float[] mView;
    private float[] mHeadView;
    private float[] mModelViewProjection;
    private float[] mModelView;

    private int mScore;
    private float objectDistance = 12f;
    private static final float TIME_DELTA = 0.3f;

    private Vibrator mVibrator;
    private CardboardOverlayView mOverlayView;

    public CardboardCube(Activity activity) {
        super(activity);
        setModel(new float[16]);
        mModelViewProjection = new float[16];
        mModelView = new float[16];
        mHeadView = new float[16];
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        mOverlayView = (CardboardOverlayView) activity.findViewById(R.id.overlay);
        mOverlayView.show3DToast("Pull the magnet when you find an object.");
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        ByteBuffer bbVertices = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COORDS.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        setVertices(bbVertices.asFloatBuffer());
        getVertices().put(WorldLayoutData.CUBE_COORDS);
        getVertices().position(0);

        ByteBuffer bbColors = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COLORS.length * 4);
        bbColors.order(ByteOrder.nativeOrder());
        setColors(bbColors.asFloatBuffer());
        getColors().put(WorldLayoutData.CUBE_COLORS);
        getColors().position(0);

        ByteBuffer bbFoundColors = ByteBuffer.allocateDirect(
                WorldLayoutData.CUBE_FOUND_COLORS.length * 4);
        bbFoundColors.order(ByteOrder.nativeOrder());
        mFoundColors = bbFoundColors.asFloatBuffer();
        mFoundColors.put(WorldLayoutData.CUBE_FOUND_COLORS);
        mFoundColors.position(0);

        ByteBuffer bbNormals = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_NORMALS.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        setNormals(bbNormals.asFloatBuffer());
        getNormals().put(WorldLayoutData.CUBE_NORMALS);
        getNormals().position(0);

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        setProgram(GLES20.glCreateProgram());
        GLES20.glAttachShader(getProgram(), vertexShader);
        GLES20.glAttachShader(getProgram(), passthroughShader);
        GLES20.glLinkProgram(getProgram());
        GLES20.glUseProgram(getProgram());

        checkGLError("Cube program");

        setPositionParam(GLES20.glGetAttribLocation(getProgram(), "a_Position"));
        setNormalParam(GLES20.glGetAttribLocation(getProgram(), "a_Normal"));
        setColorParam(GLES20.glGetAttribLocation(getProgram(), "a_Color"));

        setModelParam(GLES20.glGetUniformLocation(getProgram(), "u_Model"));
        setModelViewParam(GLES20.glGetUniformLocation(getProgram(), "u_MVMatrix"));
        setModelViewProjectionParam(GLES20.glGetUniformLocation(getProgram(), "u_MVP"));
        setLightPosParam(GLES20.glGetUniformLocation(getProgram(), "u_LightPos"));

        GLES20.glEnableVertexAttribArray(getPositionParam());
        GLES20.glEnableVertexAttribArray(getNormalParam());
        GLES20.glEnableVertexAttribArray(getColorParam());

        checkGLError("Cube program params");

        Matrix.setIdentityM(getModel(), 0);
        Matrix.translateM(getModel(), 0, 0, 0, -objectDistance);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        Matrix.rotateM(getModel(), 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);
        headTransform.getHeadView(mHeadView, 0);
    }

    public void onDrawEye(Eye eye, float[] view, float[] lightPosInEyeSpace) {
        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(mModelView, 0, view, 0, getModel(), 0);
        Matrix.multiplyMM(mModelViewProjection, 0, perspective, 0, mModelView, 0);
        draw(lightPosInEyeSpace);
    }

    /**
     * Draw the cube.
     *
     * <p>We've set all of our transformation matrices. Now we simply pass them into the shader.
     */
    public void draw(float[] lightPosInEyeSpace) {
        GLES20.glUseProgram(getProgram());

        GLES20.glUniform3fv(getLightPosParam(), 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(getModelParam(), 1, false, getModel(), 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(getModelViewParam(), 1, false, mModelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(getPositionParam(), COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, getVertices());

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(getModelViewProjectionParam(), 1, false, mModelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(getNormalParam(), 3, GLES20.GL_FLOAT, false, 0, getNormals());
        GLES20.glVertexAttribPointer(getColorParam(), 4, GLES20.GL_FLOAT, false, 0,
                isLookingAtObject() ? mFoundColors : getColors());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        checkGLError("Drawing cube");
    }

    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

        if (isLookingAtObject()) {
            mScore++;
            mOverlayView.show3DToast("Found it! Look around for another one.\nScore = " + mScore);
            hide();
        } else {
            mOverlayView.show3DToast("Look around to find the object!");
        }
        mOverlayView.show3DToast("You tapped the screen!");

        // Always give user feedback.
        mVibrator.vibrate(50);
    }

    /**
     * Find a new random position for the object.
     *
     * We'll rotate it around the Y-axis so it's out of sight, and then up or down by a little bit.
     */
    private void hide() {
        float[] rotationMatrix = new float[16];
        float[] posVec = new float[4];

        // First rotate in XZ plane, between 90 and 270 deg away, and scale so that we vary
        // the object's distance from the user.
        float angleXZ = (float) Math.random() * 180 + 90;
        Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);
        float oldObjectDistance = objectDistance;
        objectDistance = (float) Math.random() * 15 + 5;
        float objectScalingFactor = objectDistance / oldObjectDistance;
        Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor,
                objectScalingFactor);
        Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, getModel(), 12);

        // Now get the up or down angle, between -20 and 20 degrees.
        float angleY = (float) Math.random() * 80 - 40; // Angle in Y plane, between -40 and 40.
        angleY = (float) Math.toRadians(angleY);
        float newY = (float) Math.tan(angleY) * objectDistance;

        Matrix.setIdentityM(getModel(), 0);
        Matrix.translateM(getModel(), 0, posVec[0], newY, posVec[2]);
    }

    /**
     * Check if user is looking at object by calculating where the object is in eye-space.
     *
     * @return true if the user is looking at the object.
     */
    private boolean isLookingAtObject() {
        float[] initVec = { 0, 0, 0, 1.0f };
        float[] objPositionVec = new float[4];

        // Convert object space to camera space. Use the headView from onNewFrame.
        Matrix.multiplyMM(mModelView, 0, mHeadView, 0, getModel(), 0);
        Matrix.multiplyMV(objPositionVec, 0, mModelView, 0, initVec, 0);

        float pitch = (float) Math.atan2(objPositionVec[1], -objPositionVec[2]);
        float yaw = (float) Math.atan2(objPositionVec[0], -objPositionVec[2]);

        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }
}
