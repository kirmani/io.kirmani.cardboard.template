/*
 * CardboardScene.java
 * Copyright (C) 2015 sean <sean@wireless-10-147-155-193.public.utexas.edu>
 *
 * Distributed under terms of the MIT license.
 */

package io.kirmani.cardboard;

public class CardboardScene {
    private float[] mView;
    private float[] mModelView;
    private float[] mModelViewProjection;

    public CardboardScene() {
        mView = new float[16];
        mModelView = new float[16];
        mModelViewProjection = new float[16];
    }

    public float[] getView() {
        return mView;
    }

    public float[] getModelView() {
        return mModelView;
    }

    public float[] getModelViewProjection() {
        return mModelViewProjection;
    }
}
