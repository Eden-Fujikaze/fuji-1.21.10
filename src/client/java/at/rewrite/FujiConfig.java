package at.rewrite;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class FujiConfig {

    public static final ConfigClassHandler<FujiConfig> HANDLER = ConfigClassHandler
            .createBuilder(FujiConfig.class)
            .id(Identifier.of("fuji", "config"))
            .serializer(config -> GsonConfigSerializerBuilder
                    .create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("fuji.json"))
                    .build()
            )
            .build();

    @SerialEntry
    public String[] blocks = new String[]{};

    @SerialEntry
    public float threshold = 0.1f;

    @SerialEntry
    public float acceleration = 1.8f;

    @SerialEntry
    public float friction = 0.1f;

    @SerialEntry
    public float maxVelocity = 2.0f;

    @SerialEntry
    public float noiseStrength = 1.6f;

    @SerialEntry
    public float noiseFadeDist = 6.0f;

    @SerialEntry
    public float noiseDrift = 0.15f;
}