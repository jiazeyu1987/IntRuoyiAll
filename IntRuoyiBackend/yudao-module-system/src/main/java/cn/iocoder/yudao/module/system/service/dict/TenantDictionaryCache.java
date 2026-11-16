package cn.iocoder.yudao.module.system.service.dict;

import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TenantDictionaryCache {
    private TenantDictionaryCache() {}

    public static void invalidateAfterCommit() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { DictFrameworkUtils.clearTenantCache(tenantId); }
            });
        } else {
            DictFrameworkUtils.clearTenantCache(tenantId);
        }
    }
}
