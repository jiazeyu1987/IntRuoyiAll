package cn.iocoder.yudao.module.infra.service.config;

import cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode;

/** Process-wide settings are maintained by deployment, never by tenant parameter CRUD. */
public final class PlatformConfigKeys {
    private PlatformConfigKeys() { }

    public static boolean contains(String key) {
        return GxpAuditMode.CONFIG_KEY.equals(key)
                || (key != null && key.startsWith("runtime-control."));
    }
}
