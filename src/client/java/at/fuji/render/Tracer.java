package at.fuji.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import org.joml.Matrix4f;

import java.util.OptionalDouble;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class Tracer {

    private static final float R = 1.0f, G = 0.0f, B = 1.0f, A = 1.0f;

    private static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation("pipeline/fuji_tracer_lines")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build());

    private static final RenderLayer TRACER_RENDER_TYPE = RenderLayer.of(
            "fuji_tracer_lines",
            1536,
            LINES_NO_DEPTH,
            RenderLayer.MultiPhaseParameters.builder()
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.empty()))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .target(RenderPhase.ITEM_ENTITY_TARGET)
                    .build(false));

    public static void drawTracer(MatrixStack poseStack, Vec3d targetPos, Vec3d cameraPos,
            VertexConsumerProvider bufferSource) {

        poseStack.push();

        MinecraftClient mc = MinecraftClient.getInstance();
        float yaw = (float) Math.toRadians(mc.gameRenderer.getCamera().getYaw());
        float pitch = (float) Math.toRadians(mc.gameRenderer.getCamera().getPitch());

        float fx = -(float) (Math.cos(pitch) * Math.sin(yaw));
        float fy = -(float) Math.sin(pitch);
        float fz = (float) (Math.cos(pitch) * Math.cos(yaw));

        float tracerStart = 2.0f;

        float x1 = fx * tracerStart;
        float y1 = fy * tracerStart;
        float z1 = fz * tracerStart;

        float x2 = (float) (targetPos.x - cameraPos.x);
        float y2 = (float) (targetPos.y - cameraPos.y);
        float z2 = (float) (targetPos.z - cameraPos.z);

        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0)
            len = 1;

        Matrix4f matrix = poseStack.peek().getPositionMatrix();
        VertexConsumer lines = bufferSource.getBuffer(TRACER_RENDER_TYPE);

        lines.vertex(matrix, x1, y1, z1).color(R, G, B, A).normal(dx / len, dy / len, dz / len);
        lines.vertex(matrix, x2, y2, z2).color(R, G, B, A).normal(dx / len, dy / len, dz / len);

        poseStack.pop();
    }
}