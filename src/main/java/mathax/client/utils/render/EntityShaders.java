package mathax.client.utils.render;


import mathax.client.systems.modules.Modules;
import mathax.client.mixin.WorldRendererAccessor;
import mathax.client.renderer.GL;
import mathax.client.renderer.PostProcessRenderer;
import mathax.client.renderer.Shader;
import mathax.client.systems.modules.render.Chams;
import mathax.client.systems.modules.render.ESP;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;

import static mathax.client.MatHax.mc;

public class EntityShaders {
    // Overlay
    public static Framebuffer overlayFramebuffer;
    public static OutlineVertexConsumerProvider overlayVertexConsumerProvider;
    private static Shader overlayShader;

    private static Chams chams;
    public static float timer;

    // Outline
    public static Framebuffer outlinesFramebuffer;
    public static OutlineVertexConsumerProvider outlinesVertexConsumerProvider;
    private static Shader outlinesShader;

    private static ESP esp;
    public static boolean renderingOutlines;

    // Overlay

    public static void initOverlay(String shaderName) {
        overlayShader = new Shader("outline.vert", shaderName + ".frag");
        overlayFramebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false, false);
        overlayVertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
        timer = 0;
    }

    public static boolean shouldDrawOverlay(Entity entity) {
        if (chams == null) chams = Modules.get().get(Chams.class);
        return chams.isShader() && chams.entities.get().getBoolean(entity.getType()) && (entity != mc.player || !chams.ignoreSelfDepth.get());
    }

    // Outlines

    public static void initOutlines() {
        outlinesShader = new Shader("outline.vert", "outline.frag");
        outlinesFramebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false, false);
        outlinesVertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
    }

    public static boolean shouldDrawOutline(Entity entity) {
        if (esp == null) esp = Modules.get().get(ESP.class);
        return esp.isShader() && esp.getOutlineColor(entity) != null && (entity != mc.player || !esp.ignoreSelf.get());
    }

    // Main

    public static void beginRender() {
        // Overlay
        if (chams == null) chams = Modules.get().get(Chams.class);
        if (chams.isShader()) overlayFramebuffer.clear(false);

        // Outline
        if (esp == null) esp = Modules.get().get(ESP.class);
        if (esp.isShader()) outlinesFramebuffer.clear(false);

        mc.getFramebuffer().beginWrite(false);
    }

    public static void endRender() {
        WorldRenderer worldRenderer = mc.worldRenderer;
        WorldRendererAccessor wra = (WorldRendererAccessor) worldRenderer;
        Framebuffer fbo = worldRenderer.getEntityOutlinesFramebuffer();

        // Overlay
        if (chams != null && chams.isShader()) {
            wra.setEntityOutlinesFramebuffer(overlayFramebuffer);
            overlayVertexConsumerProvider.draw();
            wra.setEntityOutlinesFramebuffer(fbo);

            mc.getFramebuffer().beginWrite(false);

            GL.bindTexture(overlayFramebuffer.getColorAttachment());

            overlayShader.bind();
            overlayShader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
            overlayShader.set("u_Texture", 0);
            overlayShader.set("u_Time", timer++ / 20.0);
            PostProcessRenderer.render();
        }

        // Outline
        if (esp != null && esp.isShader()) {
            wra.setEntityOutlinesFramebuffer(outlinesFramebuffer);
            outlinesVertexConsumerProvider.draw();
            wra.setEntityOutlinesFramebuffer(fbo);

            mc.getFramebuffer().beginWrite(false);

            GL.bindTexture(outlinesFramebuffer.getColorAttachment());

            outlinesShader.bind();
            outlinesShader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
            outlinesShader.set("u_Texture", 0);
            outlinesShader.set("u_Width", esp.outlineWidth.get());
            outlinesShader.set("u_FillOpacity", esp.fillOpacity.get().floatValue() / 255.0);
            outlinesShader.set("u_ShapeMode", esp.shapeMode.get().ordinal());
            PostProcessRenderer.render();
        }
    }

    public static void onResized(int width, int height) {
        if (overlayFramebuffer != null) overlayFramebuffer.resize(width, height, false);
        if (outlinesFramebuffer != null) outlinesFramebuffer.resize(width, height, false);
    }
}
