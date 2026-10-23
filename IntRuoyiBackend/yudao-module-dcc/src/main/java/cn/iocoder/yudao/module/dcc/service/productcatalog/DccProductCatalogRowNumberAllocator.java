package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Internal global source sequence. Deployment must initialize the sidecar before wiring catalog writers. */
@Service
public class DccProductCatalogRowNumberAllocator {
    @Resource private JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public int allocate(String dataSource) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("DCC_PRODUCT_CATALOG_SEQUENCE_TRANSACTION_REQUIRED");
        }
        TenantContextHolder.getRequiredTenantId();
        if (TenantContextHolder.isIgnore() || dataSource == null || dataSource.isBlank()
                || !dataSource.equals(dataSource.trim()) || dataSource.length() > 64) {
            throw new IllegalArgumentException("DCC_PRODUCT_CATALOG_SEQUENCE_SOURCE_INVALID");
        }
        // Scope follows uk(data_source,original_row_no), which spans tenants and logically deleted rows.
        // JDBC is intentional: this global technical table has no tenant column or business authority.
        var rows = jdbc.queryForList("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source=? FOR UPDATE",
                Long.class, dataSource);
        if (rows.size() != 1) {
            throw new IllegalStateException("DCC_PRODUCT_CATALOG_SEQUENCE_NOT_INITIALIZED: " + dataSource);
        }
        Long previous = rows.get(0);
        if (previous == null || previous < 1 || previous >= Integer.MAX_VALUE) {
            throw new IllegalStateException("DCC_PRODUCT_CATALOG_SEQUENCE_RANGE_INVALID: " + dataSource);
        }
        int next = Math.toIntExact(Math.addExact(previous, 1L));
        if (jdbc.update("UPDATE dcc_product_catalog_source_sequence SET last_row_no=? WHERE data_source=? AND last_row_no=?",
                next, dataSource, previous) != 1) {
            throw new IllegalStateException("DCC_PRODUCT_CATALOG_SEQUENCE_WRITE_INCOMPLETE: " + dataSource);
        }
        return next;
    }
}
