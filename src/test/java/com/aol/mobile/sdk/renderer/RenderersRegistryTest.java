package com.aol.mobile.sdk.renderer;

import android.content.Context;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RenderersRegistryTest {
    @Test
    public void testDefaultRenderersAvailability() {
        assertThat(RenderersRegistry.listRenderers()).contains(
                RenderersRegistry.FISH_EYE_RENDERER,
                RenderersRegistry.FLAT_RENDERER,
                RenderersRegistry.FISH_EYE_RENDERER);
    }

    @Test
    public void testRenderersAddition() {
        VideoRenderer videoRenderer = mock(VideoRenderer.class);
        Context context = mock(Context.class);
        VideoRenderer.Producer producer = mock(VideoRenderer.Producer.class);
        when(producer.createRenderer(context)).thenReturn(videoRenderer);

        RenderersRegistry.registerRenderer("My.test.renderer@1.1", producer);

        assertThat(RenderersRegistry.listRenderers()).contains("My.test.renderer@1.1");
        assertThat(RenderersRegistry.getRenderer("My.test.renderer@1.1", context))
                .isEqualTo(videoRenderer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRendererIdAddition() throws Exception {
        RenderersRegistry.registerRenderer("My.test.renderer", mock(VideoRenderer.Producer.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyRendererIdAddition() throws Exception {
        RenderersRegistry.registerRenderer("", mock(VideoRenderer.Producer.class));
    }
}