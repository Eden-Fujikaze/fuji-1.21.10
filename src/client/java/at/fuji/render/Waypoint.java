package at.fuji.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

public class Waypoint {

    private static final float R = 1.0f;
    private static final float G = 0.0f;
    private static final float B = 1.0f;
    private static final float A = 1.0f;

    // Proper Yarn custom RenderLayer
    private static final RenderLayer WAYPOINT_LAYER = RenderLayer.of(
            "fuji_waypoint",
            1536,
            false,
            true, // translucent
            RenderPipelines.DEBUG_LINE_STRIP, // <-- this ignores depth
            RenderLayer.MultiPhaseParameters.builder()
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.empty()))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .target(RenderPhase.ITEM_ENTITY_TARGET)
                    .build(false));

    public static void drawWaypoint(MatrixStack matrices,
            Vec3d targetPos,
            Vec3d cameraPos,
            VertexConsumerProvider providers) {

        matrices.push();

        // Move to block position relative to camera
        matrices.translate(
                targetPos.x - cameraPos.x,
                targetPos.y - cameraPos.y,
                targetPos.z - cameraPos.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = providers.getBuffer(WAYPOINT_LAYER);

        // Bottom square
        addLine(buffer, matrix, 0, 0, 0, 1, 0, 0);
        addLine(buffer, matrix, 1, 0, 0, 1, 0, 1);
        addLine(buffer, matrix, 1, 0, 1, 0, 0, 1);
        addLine(buffer, matrix, 0, 0, 1, 0, 0, 0);

        // Top square
        addLine(buffer, matrix, 0, 1, 0, 1, 1, 0);
        addLine(buffer, matrix, 1, 1, 0, 1, 1, 1);
        addLine(buffer, matrix, 1, 1, 1, 0, 1, 1);
        addLine(buffer, matrix, 0, 1, 1, 0, 1, 0);

        // Vertical edges
        addLine(buffer, matrix, 0, 0, 0, 0, 1, 0);
        addLine(buffer, matrix, 1, 0, 0, 1, 1, 0);
        addLine(buffer, matrix, 1, 0, 1, 1, 1, 1);
        addLine(buffer, matrix, 0, 0, 1, 0, 1, 1);

        matrices.pop();
    }

    private static void addLine(VertexConsumer buffer,
            Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2) {

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0)
            len = 1;

        buffer.vertex(matrix, x1, y1, z1)
                .color(R, G, B, A)
                .normal(dx / len, dy / len, dz / len);

        buffer.vertex(matrix, x2, y2, z2)
                .color(R, G, B, A)
                .normal(dx / len, dy / len, dz / len);
    }
}