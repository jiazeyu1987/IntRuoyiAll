package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccLegacySourceNameSql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/** Read-only diagnostics of explicitly registered, sealed legacy configuration. No activation API. */
@Service
public class DccLegacySourceNameOccupancyService {
    private final JdbcTemplate jdbc;
    public DccLegacySourceNameOccupancyService(JdbcTemplate jdbc) { this.jdbc=Objects.requireNonNull(jdbc); }
    @Transactional(readOnly=true)
    public long countUnresolved(Long tenantId) {
        if (!Objects.equals(tenantId,TenantContextHolder.getRequiredTenantId())) throw new IllegalArgumentException("wrong tenant identity");
        return Objects.requireNonNull(jdbc.queryForObject(DccLegacySourceNameSql.COUNT_UNRESOLVED.replace("#{tenantId}","?"),Long.class,tenantId));
    }
}
