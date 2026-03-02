package at.fuji.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

public class Waypoint {

    private static final float R = 1.0f, G = 0.0f, B = 1.0f, A = 1.0f;

    private static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/fuji_waypoint_lines")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build());

    private static final RenderType WAYPOINT_RENDER_TYPE = RenderType.create(
            "fuji_waypoint_lines",
            1536,
            LINES_NO_DEPTH,
            RenderType.CompositeState.builder()
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false));

    public static void drawWaypoint(PoseStack poseStack, Vec3 targetPos, Vec3 cameraPos,
            MultiBufferSource bufferSource) {

        poseStack.pushPose();

        // Translate to the block position (bottom-northwest corner, like a real block)
        poseStack.translate(
                targetPos.x - cameraPos.x,
                targetPos.y - cameraPos.y,
                targetPos.z - cameraPos.z);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer lines = bufferSource.getBuffer(WAYPOINT_RENDER_TYPE);

        // 8 corners of a 1x1x1 cube
        // Bottom face (y=0)
        addLine(lines, matrix, 0, 0, 0, 1, 0, 0); // bottom south
        addLine(lines, matrix, 1, 0, 0, 1, 0, 1); // bottom east
        addLine(lines, matrix, 1, 0, 1, 0, 0, 1); // bottom north
        addLine(lines, matrix, 0, 0, 1, 0, 0, 0); // bottom west

        // Top face (y=1)
        addLine(lines, matrix, 0, 1, 0, 1, 1, 0); // top south
        addLine(lines, matrix, 1, 1, 0, 1, 1, 1); // top east
        addLine(lines, matrix, 1, 1, 1, 0, 1, 1); // top north
        addLine(lines, matrix, 0, 1, 1, 0, 1, 0); // top west

        // Vertical edges
        addLine(lines, matrix, 0, 0, 0, 0, 1, 0); // corner --
        addLine(lines, matrix, 1, 0, 0, 1, 1, 0); // corner +-
        addLine(lines, matrix, 1, 0, 1, 1, 1, 1); // corner ++
        addLine(lines, matrix, 0, 0, 1, 0, 1, 1); // corner -+

        poseStack.popPose();
    }

    private static void addLine(VertexConsumer lines, Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2) {

        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0)
            len = 1;

        lines.addVertex(matrix, x1, y1, z1).setColor(R, G, B, A).setNormal(dx / len, dy / len, dz / len);
        lines.addVertex(matrix, x2, y2, z2).setColor(R, G, B, A).setNormal(dx / len, dy / len, dz / len);
    }
}