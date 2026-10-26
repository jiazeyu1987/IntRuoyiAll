package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.regex.Pattern;

/** Explicit process-wide local development mode; never a formal audit verification result. */
@Component
@Lazy(false)
public class GxpAuditDevelopmentMode {
    public static final String PROPERTY = "yudao.gxp-audit.development-disabled";
    public static final String STATUS = "DEVELOPMENT_DISABLED";
    private static volatile boolean disabled;
    private static final Pattern LOOPBACK_MYSQL = Pattern.compile(
            "^jdbc:mysql://(?:localhost|127\\.0\\.0\\.[12]|\\[::1\\])(?::[0-9]+)?/[^/?]+(?:\\?.*)?$");

    public GxpAuditDevelopmentMode(Environment environment) {
        boolean requested = environment.getProperty(PROPERTY, Boolean.class, false);
        if (requested) {
            if (!Arrays.equals(environment.getActiveProfiles(), new String[]{"local"}))
                throw new IllegalStateException(PROPERTY + " requires only the local Spring profile");
            for (String name : new String[]{"master", "slave"}) {
                String url = environment.getProperty("spring.datasource.dynamic.datasource." + name + ".url");
                if (url == null && name.equals("slave")) continue;
                if (url == null || !(LOOPBACK_MYSQL.matcher(url).matches()
                        || isolatedCheckpointDatasource(environment, url)))
                    throw new IllegalStateException(PROPERTY + " requires local or explicitly bound isolated checkpoint MySQL datasources");
            }
            LoggerFactory.getLogger(getClass()).warn("GXP DEVELOPMENT_DISABLED: audit recording, queries and audit-dependent checks are disabled. Real authentication, signatures and business rules remain enabled. NOT FORMAL AUDIT ACCEPTANCE.");
        }
        disabled = requested;
    }

    public static boolean disabled() { return disabled; }
    private static boolean isolatedCheckpointDatasource(Environment environment, String url) {
        String space = environment.getProperty("INTRUOYI_CHECKPOINT_SPACE");
        if (space == null || !space.matches("checkpoint-[a-z0-9-]{1,40}")
                || !space.equals(environment.getProperty("intruoyi.checkpoint.space-id"))) return false;
        String prefix = "jdbc:mysql://" + space + "-mysql-1:3306/ruoyi-vue-pro";
        return url.equals(prefix) || url.startsWith(prefix + "?");
    }
    public static boolean auditBindingAccepted(String status) {
        return "CONSISTENT".equals(status) || disabled && STATUS.equals(status);
    }
}
