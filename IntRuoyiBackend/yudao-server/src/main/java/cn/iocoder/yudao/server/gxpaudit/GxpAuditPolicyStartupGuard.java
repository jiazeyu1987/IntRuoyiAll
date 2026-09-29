package cn.iocoder.yudao.server.gxpaudit;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundle;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundleLoader;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Checks persisted activation facts before Spring starts lifecycle components or signals readiness. */
@Component
@Lazy(false)
public class GxpAuditPolicyStartupGuard implements SmartInitializingSingleton {

    private final GxpAuditPolicyBundleLoader loader;
    private final GxpAuditPolicyActivationMapper activations;

    public GxpAuditPolicyStartupGuard(GxpAuditPolicyBundleLoader loader,
                                     GxpAuditPolicyActivationMapper activations) {
        this.loader = loader;
        this.activations = activations;
    }

    @Override
    public void afterSingletonsInstantiated() {
        GxpAuditPolicyBundle bundle = loader.load();
        // Activation is the scope authority, including disabled/deleted/orphan system tenants.
        // Do not change table-level tenant rules; restore the caller's context even on rejection.
        TenantUtils.executeIgnore((Runnable) () -> {
            for (GxpAuditPolicyActivationDO activation : activations.selectLatestAcrossTenants()) {
                String hash = activation.getPolicyHash();
                if (hash == null || hash.isBlank() || !hash.equals(bundle.policyHash())) {
                    throw new IllegalStateException("GxP policy startup mismatch for tenant "
                            + activation.getTenantId() + ", activation " + activation.getId());
                }
            }
        });
    }
}
