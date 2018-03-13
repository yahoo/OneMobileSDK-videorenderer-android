/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer.gles;

import android.support.annotation.NonNull;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

public final class FishEyeSphere implements GLESModel {
    private static final int VERTEX_SIZE = 5;
    private static final int TRIANGLES_PER_SLICE = 2;
    private static final int INDICES_PER_TRIANGLE = 3;
    private static final int INDICES_PER_SLICE = TRIANGLES_PER_SLICE * INDICES_PER_TRIANGLE;

    private static final int FLOAT_SIZE = Float.SIZE / Byte.SIZE;
    private static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;

    private static final int X_OFFSET = 0;
    private static final int Y_OFFSET = 1;
    private static final int Z_OFFSET = 2;
    private static final int S_OFFSET = 3;
    private static final int T_OFFSET = 4;

    private static final int STRIDE = VERTEX_SIZE * FLOAT_SIZE;
    private static final int TEXTURE_COORDINATES_OFFSET = S_OFFSET * FLOAT_SIZE;

    @NonNull
    private final FloatBuffer vbo;
    @NonNull
    private final ShortBuffer ibo;

    private final int vboSize;
    private final int indicesCount;
    private final int iboSize;

    public FishEyeSphere(int slices, float radius) {
        if (slices < 5) throw new IllegalArgumentException("Slices must be greater then 5");
        if (slices >= 180) throw new RuntimeException("Slices must be lesser or equals to 180");

        vbo = generateVBO(slices, radius);
        ibo = generateIBO(slices);

        vboSize = vbo.capacity() * FLOAT_SIZE;
        indicesCount = ibo.capacity();
        iboSize = indicesCount * SHORT_SIZE;
    }

    @NonNull
    private FloatBuffer generateVBO(int slices, float radius) {
        float latStepAngle = (float) (Math.PI / slices);
        float lonStepAngle = (float) (2.0f * Math.PI / slices);

        int vertexPerLine = slices + 1;
        int bufferSize = vertexPerLine * vertexPerLine * VERTEX_SIZE * FLOAT_SIZE;
        float[] lineVertexBuffer = new float[vertexPerLine * VERTEX_SIZE];

        FloatBuffer vertices = allocateDirect(bufferSize).order(nativeOrder()).asFloatBuffer();

        for (int latStep = 0; latStep <= slices; latStep++) {
            float latitude = latStepAngle * latStep;

            float koeffZ = (float) (3f * latitude / 2f / Math.PI);
            float sinLat = (float) Math.sin(latitude);
            float cosLat = (float) Math.cos(latitude);

            for (int lonStep = 0; lonStep <= slices; lonStep++) {
                int baseOffset = lonStep * VERTEX_SIZE;
                float longitude = lonStepAngle * lonStep;

                float sinLon = (float) Math.sin(longitude);
                float cosLon = (float) Math.cos(longitude);

                float x = radius * sinLat * sinLon;
                float y = radius * sinLat * cosLon;
                float z = radius * cosLat;
                float s = Math.min(1f, .5f + koeffZ * sinLon / 2);
                float t = Math.min(1f, .5f + koeffZ * cosLon / 2);

                lineVertexBuffer[baseOffset + X_OFFSET] = x;
                lineVertexBuffer[baseOffset + Y_OFFSET] = y;
                lineVertexBuffer[baseOffset + Z_OFFSET] = z;
                lineVertexBuffer[baseOffset + S_OFFSET] = s;
                lineVertexBuffer[baseOffset + T_OFFSET] = t;
            }

            vertices.put(lineVertexBuffer);
        }

        vertices.position(0);

        return vertices;
    }

    @NonNull
    private ShortBuffer generateIBO(int slices) {
        int vertexPerLine = slices + 1;
        int indicesCount = slices * slices * INDICES_PER_SLICE;
        int bufferSize = indicesCount * SHORT_SIZE;
        short[] lineIndexBuffer = new short[slices * INDICES_PER_SLICE];

        ShortBuffer indices = allocateDirect(bufferSize).order(nativeOrder()).asShortBuffer();

        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < slices; j++) {
                int baseOffset = j * INDICES_PER_SLICE;
                int i1 = (i + 1);
                int j1 = (j + 1);

                lineIndexBuffer[baseOffset] = (short) (i * vertexPerLine + j);
                lineIndexBuffer[baseOffset + 1] = (short) (i1 * vertexPerLine + j);
                lineIndexBuffer[baseOffset + 2] = (short) (i1 * vertexPerLine + j1);

                lineIndexBuffer[baseOffset + 3] = (short) (i * vertexPerLine + j);
                lineIndexBuffer[baseOffset + 4] = (short) (i1 * vertexPerLine + j1);
                lineIndexBuffer[baseOffset + 5] = (short) (i * vertexPerLine + j1);
            }

            indices.put(lineIndexBuffer);
        }
        indices.position(0);

        return indices;
    }

    @Override
    @NonNull
    public FloatBuffer getVBO() {
        return vbo;
    }

    @Override
    @NonNull
    public ShortBuffer getIBO() {
        return ibo;
    }

    @Override
    public int getVBOSize() {
        return vboSize;
    }

    @Override
    public int getIBOSize() {
        return iboSize;
    }

    @Override
    public int getIndicesCount() {
        return indicesCount;
    }

    @Override
    public int getVertexStride() {
        return STRIDE;
    }

    @Override
    public int getTextureCoordinatesOffset() {
        return TEXTURE_COORDINATES_OFFSET;
    }
}
