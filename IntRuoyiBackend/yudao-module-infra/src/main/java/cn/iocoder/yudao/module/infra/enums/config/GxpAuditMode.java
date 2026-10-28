package cn.iocoder.yudao.module.infra.enums.config;

import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.regex.Pattern;

/** Process-wide mode, persisted in basic configuration and applied only at startup. */
public enum GxpAuditMode {
    FORMAL, LOCAL_POLICY, DEVELOPMENT_DISABLED;

    public static final String CONFIG_KEY = "system.gxp.audit-mode";
    private static final Pattern LOOPBACK_MYSQL = Pattern.compile(
            "^jdbc:mysql://(?:localhost|127\\.0\\.0\\.[12]|\\[::1\\])(?::[0-9]+)?/[^/?]+(?:\\?.*)?$");

    public static GxpAuditMode parseRequired(String value) {
        if (value == null) throw new IllegalArgumentException("缺少 GxP 模式配置，请先初始化基础配置。");
        for (GxpAuditMode mode : values()) {
            if (mode.name().equals(value)) return mode;
        }
        throw new IllegalArgumentException("GxP 模式必须为 FORMAL、LOCAL_POLICY 或 DEVELOPMENT_DISABLED。");
    }

    public void validateEnvironment(Environment environment) {
        if (this != LOCAL_POLICY) return;
        if (!Arrays.equals(environment.getActiveProfiles(), new String[]{"local"})) {
            throw new IllegalArgumentException("本地策略审计只允许用于单独的 local 环境。");
        }
        for (String name : new String[]{"master", "slave"}) {
            String url = environment.getProperty("spring.datasource.dynamic.datasource." + name + ".url");
            if (url == null && "slave".equals(name)) continue;
            if (url == null || !LOOPBACK_MYSQL.matcher(url).matches()) {
                throw new IllegalArgumentException("当前数据库地址不允许使用所选 GxP 本地模式（" + name + "）。");
            }
        }
    }

}
