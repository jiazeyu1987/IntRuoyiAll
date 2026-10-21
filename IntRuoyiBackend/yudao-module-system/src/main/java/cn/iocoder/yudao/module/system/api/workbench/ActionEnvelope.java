package cn.iocoder.yudao.module.system.api.workbench;

import java.util.Map;

/** Shared query code treats payload as opaque; only the owning module defines its fields. */
public record ActionEnvelope(String actionKind, int payloadVersion, Map<String, String> payload) {
    public ActionEnvelope {
        if (actionKind == null || actionKind.isBlank() || payloadVersion != 1 || payload == null)
            throw new IllegalArgumentException("Invalid workbench action envelope");
        payload = Map.copyOf(payload);
    }
}
