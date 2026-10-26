package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Immutable for the life of this process. Saved configuration is consumed on the next startup. */
@Component
@Lazy(false)
public class GxpAuditDevelopmentMode {
    public static final String STATUS = "DEVELOPMENT_DISABLED";
    private static volatile GxpAuditMode currentMode = GxpAuditMode.FORMAL;

    @Autowired
    public GxpAuditDevelopmentMode(Environment environment, ConfigMapper configs) {
        this(environment, readRequired(configs));
    }

    @VisibleForTesting
    public GxpAuditDevelopmentMode(Environment environment, GxpAuditMode mode) {
        try {
            mode.validateEnvironment(environment);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
        currentMode = mode;
        LoggerFactory.getLogger(getClass()).warn("GXP mode applied at startup: {}. Configuration changes require a restart.", mode);
    }

    private static GxpAuditMode readRequired(ConfigMapper configs) {
        ConfigDO config = configs.selectByKey(GxpAuditMode.CONFIG_KEY);
        if (config == null || !Boolean.TRUE.equals(config.getVisible()) || !Integer.valueOf(1).equals(config.getType())) {
            throw new IllegalStateException("缺少有效的 GxP 模式配置，请先执行 20261026_gxp_audit_mode_config.sql。");
        }
        return GxpAuditMode.parseRequired(config.getValue());
    }

    public static GxpAuditMode currentMode() { return currentMode; }
    public static boolean disabled() { return currentMode == GxpAuditMode.DEVELOPMENT_DISABLED; }
    public boolean localPolicyEnabled() { return currentMode == GxpAuditMode.LOCAL_POLICY; }
    public static boolean auditBindingAccepted(String status) {
        return "CONSISTENT".equals(status) || disabled() && STATUS.equals(status);
    }
}
