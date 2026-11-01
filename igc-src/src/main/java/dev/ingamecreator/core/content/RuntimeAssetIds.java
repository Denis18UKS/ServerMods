package dev.ingamecreator.core.content;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** Stable numeric IDs used by exported resource-pack CustomModelData overrides. */
public final class RuntimeAssetIds {
    private RuntimeAssetIds() {}

    public static int customModelData(String projectId, String elementId) {
        String value = String.valueOf(projectId) + ":" + String.valueOf(elementId);
        CRC32 crc = new CRC32();
        crc.update(value.getBytes(StandardCharsets.UTF_8));
        int result = (int) (crc.getValue() & 0x7FFFFFFFL);
        return result == 0 ? 1 : result;
    }
}
