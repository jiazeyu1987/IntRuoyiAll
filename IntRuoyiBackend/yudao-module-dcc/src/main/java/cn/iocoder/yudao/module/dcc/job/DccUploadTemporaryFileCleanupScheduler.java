package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DccUploadTemporaryFileCleanupScheduler {

    public static final int CLEANUP_BATCH_SIZE = 100;

    @Resource
    private TenantFrameworkService tenantFrameworkService;
    @Resource
    private DccUploadTicketService uploadTicketService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void schedule() {
        for (Long tenantId : tenantFrameworkService.getTenantIds()) {
            TenantUtils.execute(tenantId, () -> cleanupTenant(tenantId));
        }
    }

    private void cleanupTenant(Long tenantId) {
        try {
            uploadTicketService.cleanupExpiredTemporaryFiles(LocalDateTime.now(), CLEANUP_BATCH_SIZE);
        } catch (Exception ex) {
            throw new IllegalStateException("DCC upload temporary cleanup failed for tenantId=" + tenantId, ex);
        }
    }

}
