package at.fuji.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

public class Waypoint {

        private static final RenderLayer WAYPOINT_LAYER = RenderLayer.of(
                        "fuji_waypoint", 1536, false, true,
                        RenderPipelines.DEBUG_LINE_STRIP,
                        RenderLayer.MultiPhaseParameters.builder()
                                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.empty()))
                                        .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                                        .target(RenderPhase.ITEM_ENTITY_TARGET)
                                        .build(false));

        public static void drawWaypoint(MatrixStack matrices, Vec3d targetPos,
                        Vec3d cameraPos, VertexConsumerProvider providers,
                        int argbColor) {
                float a = ((argbColor >> 24) & 0xFF) / 255f;
                float r = ((argbColor >> 16) & 0xFF) / 255f;
                float g = ((argbColor >> 8) & 0xFF) / 255f;
                float b = ((argbColor) & 0xFF) / 255f;

                matrices.push();
                matrices.translate(
                                targetPos.x - cameraPos.x,
                                targetPos.y - cameraPos.y,
                                targetPos.z - cameraPos.z);

                Matrix4f m = matrices.peek().getPositionMatrix();
                VertexConsumer buf = providers.getBuffer(WAYPOINT_LAYER);

                // Bottom
                addLine(buf, m, 0, 0, 0, 1, 0, 0, r, g, b, a);
                addLine(buf, m, 1, 0, 0, 1, 0, 1, r, g, b, a);
                addLine(buf, m, 1, 0, 1, 0, 0, 1, r, g, b, a);
                addLine(buf, m, 0, 0, 1, 0, 0, 0, r, g, b, a);
                // Top
                addLine(buf, m, 0, 1, 0, 1, 1, 0, r, g, b, a);
                addLine(buf, m, 1, 1, 0, 1, 1, 1, r, g, b, a);
                addLine(buf, m, 1, 1, 1, 0, 1, 1, r, g, b, a);
                addLine(buf, m, 0, 1, 1, 0, 1, 0, r, g, b, a);
                // Verticals
                addLine(buf, m, 0, 0, 0, 0, 1, 0, r, g, b, a);
                addLine(buf, m, 1, 0, 0, 1, 1, 0, r, g, b, a);
                addLine(buf, m, 1, 0, 1, 1, 1, 1, r, g, b, a);
                addLine(buf, m, 0, 0, 1, 0, 1, 1, r, g, b, a);

                matrices.pop();
        }

        private static void addLine(VertexConsumer buf, Matrix4f m,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float r, float g, float b, float a) {
                float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len == 0)
                        len = 1;
                buf.vertex(m, x1, y1, z1).color(r, g, b, a).normal(dx / len, dy / len, dz / len);
                buf.vertex(m, x2, y2, z2).color(r, g, b, a).normal(dx / len, dy / len, dz / len);
        }
}