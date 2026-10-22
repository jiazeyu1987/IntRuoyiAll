package cn.iocoder.yudao.module.ai.tool;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * IntRuoyi MCP 只读工具。
 *
 * <p>仅暴露经过裁剪的系统汇总和租户摘要，避免把示例 PersonService 的写入工具
 * 暴露给 Codex 或其它 MCP 客户端。</p>
 */
@Service
@Slf4j
public class IntruoyiMcpReadOnlyTools {

    private final TenantService tenantService;
    private final AdminUserService adminUserService;

    public IntruoyiMcpReadOnlyTools(TenantService tenantService, AdminUserService adminUserService) {
        this.tenantService = tenantService;
        this.adminUserService = adminUserService;
    }

    @Tool(
            name = "intruoyi_get_system_summary",
            description = "Read the current IntRuoyi system summary, including tenant and enabled user counts."
    )
    public SystemSummary getSystemSummary() {
        SystemSummary summary = new SystemSummary(
                tenantService.getTenantIdList().size(),
                tenantService.getTenantListByStatus(CommonStatusEnum.ENABLE.getStatus()).size(),
                adminUserService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus()).size()
        );
        log.info("[MCP] intruoyi_get_system_summary completed");
        return summary;
    }

    @Tool(
            name = "intruoyi_get_tenant_summary",
            description = "Read a tenant's safe summary by tenant ID. Contact details and credentials are excluded."
    )
    public TenantSummary getTenantSummary(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        TenantDO tenant = tenantService.getTenant(tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant does not exist: " + tenantId);
        }
        TenantSummary summary = new TenantSummary(
                tenant.getId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getExpireTime()
        );
        log.info("[MCP] intruoyi_get_tenant_summary completed for tenantId={}", tenantId);
        return summary;
    }

    public record SystemSummary(
            int tenantCount,
            int enabledTenantCount,
            int enabledUserCount
    ) {
    }

    public record TenantSummary(
            Long id,
            String name,
            Integer status,
            LocalDateTime expireTime
    ) {
    }

}
