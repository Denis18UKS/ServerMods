package dev.ingamecreator.core.content;

import dev.ingamecreator.core.project.CreatorProject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Exports Creator 2D textures and cuboid models to an ordinary Minecraft 1.20.1 resource pack. */
public final class ResourcePackExporter {
    private static final String NS = "ingamecreator";

    private ResourcePackExporter() {}

    public static ExportReport export(CreatorProject project, Path outputRoot) throws IOException {
        if (project == null) throw new IllegalArgumentException("project is required");
        if (outputRoot == null) throw new IllegalArgumentException("output path is required");
        Files.createDirectories(outputRoot);
        Counter counter = new Counter();
        write(outputRoot.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": 15,
                    "description": "InGame Creator runtime assets"
                  }
                }
                """, counter);

        String projectId = project.id();
        for (TextureAsset texture : project.textures().values()) {
            Path item = outputRoot.resolve("assets/" + NS + "/textures/item/runtime/" + projectId + "/" + texture.id() + ".png");
            Path block = outputRoot.resolve("assets/" + NS + "/textures/block/runtime/" + projectId + "/" + texture.id() + ".png");
            writePng(texture, item, counter);
            writePng(texture, block, counter);
        }

        List<Override> itemOverrides = new ArrayList<>();
        var sortedItems = project.runtimeItems().values().stream().sorted(Comparator.comparing(RuntimeItemDefinition::id)).toList();
        for (RuntimeItemDefinition def : sortedItems) {
            String target = NS + ":item/runtime/" + projectId + "/" + def.id();
            Path modelPath = outputRoot.resolve("assets/" + NS + "/models/item/runtime/" + projectId + "/" + def.id() + ".json");
            write(modelPath, itemModel(project, def), counter);
            itemOverrides.add(new Override(RuntimeAssetIds.customModelData(projectId, def.id()), target));
        }

        List<Override> blockOverrides = new ArrayList<>();
        var sortedBlocks = project.runtimeBlocks().values().stream().sorted(Comparator.comparing(RuntimeBlockDefinition::id)).toList();
        for (RuntimeBlockDefinition def : sortedBlocks) {
            String target = NS + ":block/runtime/" + projectId + "/" + def.id();
            Path modelPath = outputRoot.resolve("assets/" + NS + "/models/block/runtime/" + projectId + "/" + def.id() + ".json");
            write(modelPath, blockModel(project, def), counter);
            blockOverrides.add(new Override(RuntimeAssetIds.customModelData(projectId, def.id()), target));
        }

        write(outputRoot.resolve("assets/" + NS + "/models/item/runtime_item.json"), hostItemModel(itemOverrides, "minecraft:item/paper"), counter);
        write(outputRoot.resolve("assets/" + NS + "/models/item/runtime_block.json"), hostItemModel(blockOverrides, "minecraft:item/barrier"), counter);
        write(outputRoot.resolve("assets/" + NS + "/models/block/runtime_block_placeholder.json"), """
                {"parent":"minecraft:block/cube_all","textures":{"all":"minecraft:block/structure_block"}}
                """, counter);
        write(outputRoot.resolve("assets/" + NS + "/blockstates/runtime_block.json"), """
                {"variants":{"":{"model":"ingamecreator:block/runtime_block_placeholder"}}}
                """, counter);
        return new ExportReport(outputRoot, counter.value, project.textures().size(), project.runtimeItems().size(), project.runtimeBlocks().size());
    }

    private static String itemModel(CreatorProject project, RuntimeItemDefinition definition) {
        CuboidModelAsset model = definition.modelId().isBlank() ? null : project.model(definition.modelId()).orElse(null);
        if (model != null && !model.cuboids().isEmpty()) {
            return cuboidModel(project.id(), model, definition.textureId(), true);
        }
        String texture = definition.textureId().isBlank() ? "minecraft:item/paper"
                : NS + ":item/runtime/" + project.id() + "/" + definition.textureId();
        return "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + json(texture) + "\"}}\n";
    }

    private static String blockModel(CreatorProject project, RuntimeBlockDefinition definition) {
        CuboidModelAsset model = definition.modelId().isBlank() ? null : project.model(definition.modelId()).orElse(null);
        if (model != null && !model.cuboids().isEmpty()) {
            return cuboidModel(project.id(), model, definition.textureId(), false);
        }
        String texture = definition.textureId().isBlank() ? "minecraft:block/structure_block"
                : NS + ":block/runtime/" + project.id() + "/" + definition.textureId();
        return "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + json(texture) + "\"}}\n";
    }

    private static String cuboidModel(String projectId, CuboidModelAsset model, String fallbackTextureId, boolean itemTexture) {
        StringBuilder out = new StringBuilder("{\"textures\":{");
        List<String> textureIds = new ArrayList<>();
        for (CuboidModelAsset.Cuboid cuboid : model.cuboids()) {
            String id = cuboid.textureId().isBlank() ? fallbackTextureId : cuboid.textureId();
            if (id != null && !id.isBlank() && !textureIds.contains(id)) textureIds.add(id);
        }
        if (textureIds.isEmpty()) textureIds.add("missing");
        for (int i = 0; i < textureIds.size(); i++) {
            if (i > 0) out.append(',');
            String id = textureIds.get(i);
            String path = id.equals("missing") ? "minecraft:block/magenta_glazed_terracotta"
                    : NS + ":" + (itemTexture ? "item" : "block") + "/runtime/" + projectId + "/" + id;
            out.append('"').append("t").append(i).append("\":\"").append(json(path)).append('"');
        }
        out.append("},\"elements\":[");
        for (int i = 0; i < model.cuboids().size(); i++) {
            if (i > 0) out.append(',');
            CuboidModelAsset.Cuboid cube = model.cuboids().get(i);
            String id = cube.textureId().isBlank() ? fallbackTextureId : cube.textureId();
            int tex = Math.max(0, textureIds.indexOf(id == null || id.isBlank() ? "missing" : id));
            out.append("{\"name\":\"").append(json(cube.name())).append("\",\"from\":[")
                    .append(num(cube.x())).append(',').append(num(cube.y())).append(',').append(num(cube.z()))
                    .append("],\"to\":[").append(num(cube.x() + cube.width())).append(',')
                    .append(num(cube.y() + cube.height())).append(',').append(num(cube.z() + cube.depth())).append(']');
            String rotation = rotationJson(cube);
            if (!rotation.isEmpty()) out.append(',').append(rotation);
            out.append(",\"faces\":{");
            String[] faces = {"north","south","east","west","up","down"};
            for (int f = 0; f < faces.length; f++) {
                if (f > 0) out.append(',');
                out.append('"').append(faces[f]).append("\":{\"texture\":\"#t").append(tex).append("\"}");
            }
            out.append("}}");
        }
        out.append("]}");
        return out.toString();
    }

    private static String rotationJson(CuboidModelAsset.Cuboid cube) {
        double[] values = {cube.rotX(), cube.rotY(), cube.rotZ()};
        String[] axes = {"x", "y", "z"};
        int chosen = -1;
        for (int i = 0; i < values.length; i++) if (Math.abs(values[i]) > 0.0001) { chosen = i; break; }
        if (chosen < 0) return "";
        double angle = nearestAllowed(values[chosen]);
        if (Math.abs(angle) < 0.0001) return "";
        double ox = cube.x() + cube.width() / 2.0;
        double oy = cube.y() + cube.height() / 2.0;
        double oz = cube.z() + cube.depth() / 2.0;
        return "\"rotation\":{\"origin\":[" + num(ox) + ',' + num(oy) + ',' + num(oz)
                + "],\"axis\":\"" + axes[chosen] + "\",\"angle\":" + num(angle) + ",\"rescale\":false}";
    }

    private static double nearestAllowed(double value) {
        double[] allowed = {-45, -22.5, 0, 22.5, 45};
        double best = 0, distance = Double.MAX_VALUE;
        for (double candidate : allowed) {
            double d = Math.abs(candidate - value);
            if (d < distance) { best = candidate; distance = d; }
        }
        return best;
    }

    private static String hostItemModel(List<Override> overrides, String defaultTexture) {
        StringBuilder out = new StringBuilder("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"")
                .append(defaultTexture).append("\"},\"overrides\":[");
        for (int i = 0; i < overrides.size(); i++) {
            if (i > 0) out.append(',');
            Override override = overrides.get(i);
            out.append("{\"predicate\":{\"custom_model_data\":").append(override.customModelData)
                    .append("},\"model\":\"").append(json(override.model)).append("\"}");
        }
        return out.append("]}\n").toString();
    }

    private static void writePng(TextureAsset texture, Path file, Counter counter) throws IOException {
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(texture.width(), texture.height(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, texture.width(), texture.height(), texture.pixels(), 0, texture.width());
        if (!ImageIO.write(image, "PNG", file.toFile())) throw new IOException("PNG writer unavailable");
        counter.value++;
    }

    private static void write(Path file, String text, Counter counter) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
        counter.value++;
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String num(double value) {
        if (Math.rint(value) == value) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public record ExportReport(Path root, int filesWritten, int textures, int items, int blocks) {}
    private record Override(int customModelData, String model) {}
    private static final class Counter { int value; }
}
