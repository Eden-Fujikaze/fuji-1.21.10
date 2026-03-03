package at.fuji.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Tracer {

    private static final float R = 1.0f;
    private static final float G = 0.0f;
    private static final float B = 1.0f;
    private static final float A = 1.0f;

    public static void drawTracer(MatrixStack matrices,
            Vec3d targetPos,
            Vec3d cameraPos,
            VertexConsumerProvider providers) {

        matrices.push();

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();

        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());

        // Forward vector
        float fx = -(float) (Math.cos(pitch) * Math.sin(yaw));
        float fy = -(float) Math.sin(pitch);
        float fz = (float) (Math.cos(pitch) * Math.cos(yaw));

        float startDist = 2.0f;

        float x1 = fx * startDist;
        float y1 = fy * startDist;
        float z1 = fz * startDist;

        float x2 = (float) (targetPos.x - cameraPos.x);
        float y2 = (float) (targetPos.y - cameraPos.y);
        float z2 = (float) (targetPos.z - cameraPos.z);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0)
            len = 1;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = providers.getBuffer(RenderLayer.getLines());

        // First vertex
        buffer.vertex(matrix, x1, y1, z1)
                .color(R, G, B, A)
                .normal(dx / len, dy / len, dz / len);

        // Second vertex
        buffer.vertex(matrix, x2, y2, z2)
                .color(R, G, B, A)
                .normal(dx / len, dy / len, dz / len);

        matrices.pop();
    }
}