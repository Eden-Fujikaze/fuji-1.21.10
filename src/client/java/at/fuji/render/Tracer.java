package at.fuji.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

public class Tracer {

    private static final float R = 1.0f, G = 0.0f, B = 1.0f, A = 1.0f;

    private static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/fuji_tracer_lines")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build());

    private static final RenderType TRACER_RENDER_TYPE = RenderType.create(
            "fuji_tracer_lines",
            1536,
            LINES_NO_DEPTH,
            RenderType.CompositeState.builder()
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false));

    public static void drawTracer(PoseStack poseStack, Vec3 targetPos, Vec3 cameraPos,
            MultiBufferSource bufferSource) {

        poseStack.pushPose();

        Minecraft mc = Minecraft.getInstance();
        float yaw = (float) Math.toRadians(mc.gameRenderer.getMainCamera().getYRot());
        float pitch = (float) Math.toRadians(mc.gameRenderer.getMainCamera().getXRot());

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

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer lines = bufferSource.getBuffer(TRACER_RENDER_TYPE);

        lines.addVertex(matrix, x1, y1, z1).setColor(R, G, B, A).setNormal(dx / len, dy / len, dz / len);
        lines.addVertex(matrix, x2, y2, z2).setColor(R, G, B, A).setNormal(dx / len, dy / len, dz / len);

        poseStack.popPose();
    }
}