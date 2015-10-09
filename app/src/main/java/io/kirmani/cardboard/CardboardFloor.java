/*
 * CardboardFloor.java
 * Copyright (C) 2015 sean <sean@wireless-10-147-155-193.public.utexas.edu>
 *
 * Distributed under terms of the MIT license.
 */

package io.kirmani.cardboard;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vrtoolkit.cardboard.Eye;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;

public class CardboardFloor extends CardboardObject {
    private static final String TAG = "CardboardFloor";

    private float mFloorDepth = 20f;

    public CardboardFloor(Context context, CardboardScene scene) {
        super(context, scene);
        setModel(new float[16]);
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        super.onSurfaceCreated(config);
        Log.i(TAG, "onSurfaceCreated");
        // make a floor
        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(FLOOR_COORDS.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        setVertices(bbFloorVertices.asFloatBuffer());
        getVertices().put(FLOOR_COORDS);
        getVertices().position(0);

        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(FLOOR_NORMALS.length * 4);
        bbFloorNormals.order(ByteOrder.nativeOrder());
        setNormals(bbFloorNormals.asFloatBuffer());
        getNormals().put(FLOOR_NORMALS);
        getNormals().position(0);

        ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(FLOOR_COLORS.length * 4);
        bbFloorColors.order(ByteOrder.nativeOrder());
        setColors(bbFloorColors.asFloatBuffer());
        getColors().put(FLOOR_COLORS);
        getColors().position(0);

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        setProgram(GLES20.glCreateProgram());
        GLES20.glAttachShader(getProgram(), vertexShader);
        GLES20.glAttachShader(getProgram(), gridShader);
        GLES20.glLinkProgram(getProgram());
        GLES20.glUseProgram(getProgram());

        checkGLError("Floor program");

        setModelParam(GLES20.glGetUniformLocation(getProgram(), "u_Model"));
        setModelViewParam(GLES20.glGetUniformLocation(getProgram(), "u_MVMatrix"));
        setModelViewProjectionParam(GLES20.glGetUniformLocation(getProgram(), "u_MVP"));
        setLightPosParam(GLES20.glGetUniformLocation(getProgram(), "u_LightPos"));

        setPositionParam(GLES20.glGetAttribLocation(getProgram(), "a_Position"));
        setNormalParam(GLES20.glGetAttribLocation(getProgram(), "a_Normal"));
        setColorParam(GLES20.glGetAttribLocation(getProgram(), "a_Color"));

        GLES20.glEnableVertexAttribArray(getPositionParam());
        GLES20.glEnableVertexAttribArray(getNormalParam());
        GLES20.glEnableVertexAttribArray(getColorParam());

        checkGLError("Floor program params");

        Matrix.setIdentityM(getModel(), 0);
        Matrix.translateM(getModel(), 0, 0, -mFloorDepth, 0); // Floor appears below user.
        checkGLError("onSurfaceCreated");
    }

    public void onDrawEye(Eye eye) {
        super.onDrawEye(eye);
        // Set modelView for the floor, so we draw floor in the correct location
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(getModelView(), 0, getView(), 0, getModel(), 0);
        Matrix.multiplyMM(getModelViewProjection(), 0, perspective, 0,
                getModelView(), 0);
        draw();
    }

    /**
     * Draw the floor.
     *
     * This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void draw() {
        GLES20.glUseProgram(getProgram());

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(getLightPosParam(), 1, getLightPosInEyeSpace(), 0);
        GLES20.glUniformMatrix4fv(getModelParam(), 1, false, getModel(), 0);
        GLES20.glUniformMatrix4fv(getModelViewParam(), 1, false, getModelView(), 0);
        GLES20.glUniformMatrix4fv(getModelViewProjectionParam(), 1, false,
                getModelViewProjection(), 0);
        GLES20.glVertexAttribPointer(getPositionParam(), COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, getVertices());
        GLES20.glVertexAttribPointer(getNormalParam(), 3, GLES20.GL_FLOAT, false, 0,
                getNormals());
        GLES20.glVertexAttribPointer(getColorParam(), 4, GLES20.GL_FLOAT, false, 0, getColors());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        checkGLError("drawing floor");
    }

    public static final float[] FLOOR_COORDS = new float[] {
        200f, 0, -200f,
            -200f, 0, -200f,
            -200f, 0, 200f,
            200f, 0, -200f,
            -200f, 0, 200f,
            200f, 0, 200f,
    };

    public static final float[] FLOOR_NORMALS = new float[] {
        0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
    };

    public static final float[] FLOOR_COLORS = new float[] {
        0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
    };
}

