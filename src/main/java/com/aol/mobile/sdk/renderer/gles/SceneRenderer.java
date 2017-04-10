/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer.gles;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDeleteBuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public final class SceneRenderer implements GLSurfaceView.Renderer {
    private static final int MSG_TEXTURE_CONSTRUCTED = 0x01;
    private static final int MSG_TEXTURE_RESIZED = 0x02;
    private static final int MSG_TEXTURE_DESTROYED = 0x03;

    @NonNull
    private final Handler handler;
    @NonNull
    private final VideoSurfaceListener textureListener;
    @NonNull
    private final Camera camera;
    @NonNull
    private final GLESModel model;

    // OpenGL ES stuff
    private final int[] vbo = new int[1];
    private final int[] ibo = new int[1];
    private final int[] extTexture = new int[1];
    private final float[] videoTextureMatrix = new float[16];
    @Nullable
    private ShadersProgram program;
    @Nullable
    private SurfaceTexture surfaceTexture;
    @Nullable
    private Surface surface;

    public SceneRenderer(@NonNull GLESModel model, @NonNull VideoSurfaceListener videoSurfaceListener) {
        this.model = model;
        textureListener = videoSurfaceListener;
        camera = new Camera(60f, 1f, 1000f);
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_TEXTURE_CONSTRUCTED:
                        textureListener.onVideoSurfaceAvailable((Surface) msg.obj);
                        break;

                    case MSG_TEXTURE_RESIZED:
                        textureListener.onVideoSurfaceResized(msg.arg1, msg.arg2);
                        break;

                    case MSG_TEXTURE_DESTROYED:
                        textureListener.onVideoSurfaceReleased((Surface) msg.obj);
                        break;
                }

                return false;
            }
        });
    }

    @Override
    @WorkerThread
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        glDisable(GL_DEPTH_TEST);
        glClearColor(.0f, .0f, 1.0f, .0f);

        program = new ShadersProgram();

        // Init external texture
        glGenTextures(extTexture.length, extTexture, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, extTexture[0]);

        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Init VBO and IBO
        glGenBuffers(vbo.length, vbo, 0);
        glGenBuffers(ibo.length, ibo, 0);

        glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        glBufferData(GL_ARRAY_BUFFER, model.getVBOSize(), model.getVBO(), GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, model.getIBOSize(), model.getIBO(), GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        surfaceTexture = new SurfaceTexture(extTexture[0]);
        surface = new Surface(surfaceTexture);
        handler.sendMessage(Message.obtain(handler, MSG_TEXTURE_CONSTRUCTED, surface));
    }

    @Override
    @WorkerThread
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        glViewport(0, 0, width, height);
        camera.updateViewportSize(width, height);

        handler.sendMessage(Message.obtain(handler, MSG_TEXTURE_RESIZED, width, height));
    }

    @Override
    @WorkerThread
    public void onDrawFrame(GL10 unused) {
        if (surfaceTexture == null || program == null) return;
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(videoTextureMatrix);

        float[] mvpMatrix = camera.getMvpMatrix();

        int attributePosition = program.getAttributePosition();
        int attributeTextureCoord = program.getAttributeTextureCoord();
        int uniformTextureMatrix = program.getUniformTextureMatrix();
        int uniformMVPMatrix = program.getUniformMVPMatrix();
        int vertexStride = model.getVertexStride();
        int texCoordOffset = model.getTextureCoordinatesOffset();

        glClear(GL_COLOR_BUFFER_BIT);

        glUniformMatrix4fv(uniformMVPMatrix, 1, false, mvpMatrix, 0);

        glBindTexture(GL_TEXTURE_EXTERNAL_OES, extTexture[0]);
        glUniformMatrix4fv(uniformTextureMatrix, 1, false, videoTextureMatrix, 0);

        glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo[0]);

        glEnableVertexAttribArray(attributePosition);
        glEnableVertexAttribArray(attributeTextureCoord);

        glVertexAttribPointer(attributePosition, 3, GL_FLOAT, false, vertexStride, 0);
        glVertexAttribPointer(attributeTextureCoord, 2, GL_FLOAT, false, vertexStride, texCoordOffset);

        glDrawElements(GL_TRIANGLES, model.getIndicesCount(), GL_UNSIGNED_SHORT, 0);

        glDisableVertexAttribArray(attributePosition);
        glDisableVertexAttribArray(attributeTextureCoord);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @WorkerThread
    public void dispose() {
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }

        if (program != null) {
            program.release();
            program = null;
        }

        glDeleteTextures(extTexture.length, extTexture, 0);
        glDeleteBuffers(vbo.length, vbo, 0);
        glDeleteBuffers(ibo.length, ibo, 0);

        if (surface != null) {
            surface.release();
            handler.sendMessage(Message.obtain(handler, MSG_TEXTURE_DESTROYED, surface));
            surface = null;
        }
    }

    @WorkerThread
    public void setCameraOrientation(double longitude, double latitude) {
        camera.lookAt(longitude, latitude);
    }
}
