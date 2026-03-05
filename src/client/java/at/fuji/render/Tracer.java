package at.fuji.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Tracer {

    public static void drawTracer(MatrixStack matrices, Vec3d targetPos,
            Vec3d cameraPos, VertexConsumerProvider providers,
            int argbColor) {
        float a = ((argbColor >> 24) & 0xFF) / 255f;
        float r = ((argbColor >> 16) & 0xFF) / 255f;
        float g = ((argbColor >> 8) & 0xFF) / 255f;
        float b = ((argbColor) & 0xFF) / 255f;

        matrices.push();
        var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());

        float fx = -(float) (Math.cos(pitch) * Math.sin(yaw));
        float fy = -(float) Math.sin(pitch);
        float fz = (float) (Math.cos(pitch) * Math.cos(yaw));

        float startDist = 2.0f;
        float x1 = fx * startDist, y1 = fy * startDist, z1 = fz * startDist;
        float x2 = (float) (targetPos.x - cameraPos.x);
        float y2 = (float) (targetPos.y - cameraPos.y);
        float z2 = (float) (targetPos.z - cameraPos.z);

        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0)
            len = 1;

        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer buf = providers.getBuffer(RenderLayer.getLines());
        buf.vertex(m, x1, y1, z1).color(r, g, b, a).normal(dx / len, dy / len, dz / len);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a).normal(dx / len, dy / len, dz / len);
        matrices.pop();
    }
}