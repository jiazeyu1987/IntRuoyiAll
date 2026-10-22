package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrBatchExecutionMapperTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private DataSource dataSource;

    @Test
    void selectPage_excludeReleasedFiltersReleasedTransactionWithoutSqlError() {
        MesProEdhrBatchExecutionDO activeBatch = insertBatchExecution("EDHR-ACTIVE", 30);
        MesProEdhrBatchExecutionDO releasedBatch = insertBatchExecution("EDHR-RELEASED", 30);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setReleaseCode("REL-001")
                .setBatchExecutionId(releasedBatch.getId())
                .setBatchExecutionCode(releasedBatch.getBatchExecutionCode())
                .setBatchCode(releasedBatch.getBatchCode())
                .setReleaseStatus("RELEASED"));

        EdhrBatchExecutionPageReqVO reqVO = new EdhrBatchExecutionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setExcludeReleased(true);

        PageResult<MesProEdhrBatchExecutionDO> result = batchExecutionMapper.selectPage(reqVO);

        assertEquals(1, result.getTotal());
        assertEquals(activeBatch.getId(), result.getList().get(0).getId());
    }

    @Test
    void selectDeviationOptionsPage_scopesTenantAndKeepsBeforePqcBatchButExcludesVoidedAndReleased() {
        MesProEdhrBatchExecutionDO beforePqcBatch = insertBatchExecution("EDHR-DEVIATION-BEFORE-PQC", 30);
        MesProEdhrBatchExecutionDO voidedBatch = insertBatchExecution("EDHR-DEVIATION-VOIDED", 60);
        MesProEdhrBatchExecutionDO releasedBatch = insertBatchExecution("EDHR-DEVIATION-RELEASED", 30);
        MesProEdhrBatchExecutionDO otherTenantBatch = insertBatchExecution("EDHR-DEVIATION-OTHER-TENANT", 30);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setReleaseCode("REL-DEVIATION-RELEASED")
                .setBatchExecutionId(releasedBatch.getId())
                .setBatchExecutionCode(releasedBatch.getBatchExecutionCode())
                .setBatchCode(releasedBatch.getBatchCode())
                .setReleaseStatus("RELEASED"));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("UPDATE mes_pro_edhr_batch_execution SET tenant_id=1 WHERE id IN (?, ?, ?)",
                beforePqcBatch.getId(), voidedBatch.getId(), releasedBatch.getId());
        jdbcTemplate.update("UPDATE mes_pro_edhr_batch_execution SET tenant_id=2 WHERE id=?",
                otherTenantBatch.getId());
        jdbcTemplate.update("UPDATE mes_pro_edhr_release_transaction SET tenant_id=1 WHERE batch_execution_id=?",
                releasedBatch.getId());
        for (MesProEdhrBatchExecutionDO batch : java.util.List.of(beforePqcBatch, voidedBatch, releasedBatch, otherTenantBatch)) {
            long tenant = batch == otherTenantBatch ? 2L : 1L;
            jdbcTemplate.update("INSERT INTO mes_pro_edhr_batch_execution_origin "
                    + "(tenant_id,batch_execution_id,entry_type,origin_key,active_order_id,"
                    + "source_snapshot_hash,batch_provision_receipt_id,batch_provision_status,"
                    + "source_bundle_hash,idempotency_key,relation_status,captured_at) "
                    + "VALUES (?,?,'ACTIVE_ORDER_COMPLETION',?,?,'source',1,'SUCCESS','bundle',?,'CURRENT',CURRENT_TIMESTAMP)",
                    tenant, batch.getId(), "option-" + batch.getId(), batch.getId(), "option-" + batch.getId());
        }
        MesProEdhrBatchExecutionDO noOrigin = insertBatchExecution("EDHR-NO-ORIGIN", 30);
        jdbcTemplate.update("UPDATE mes_pro_edhr_batch_execution SET tenant_id=1 WHERE id=?", noOrigin.getId());
        TenantContextHolder.setTenantId(1L);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mes_pro_edhr_batch_execution WHERE tenant_id=1 AND id=? AND status=30",
                Integer.class, beforePqcBatch.getId()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mes_pro_edhr_release_transaction WHERE tenant_id=1 AND batch_execution_id=? AND release_status='RELEASED'",
                Integer.class, releasedBatch.getId()));
        MesProEdhrDeviationBatchOptionPageReqVO reqVO = new MesProEdhrDeviationBatchOptionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<MesProEdhrBatchExecutionDO> result =
                batchExecutionMapper.selectDeviationOptionsPage(reqVO, 1L, null);

        assertEquals(1L, result.getTotal());
        assertEquals(beforePqcBatch.getId(), result.getList().get(0).getId());
    }

    private MesProEdhrBatchExecutionDO insertBatchExecution(String code, int status) {
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode(code)
                .setWorkOrderId(1001L)
                .setWorkOrderCode("WO-" + code)
                .setBatchCode("BATCH-" + code)
                .setAttemptNo(1)
                .setProductId(2001L)
                .setProductCode("PROD-" + code)
                .setProductName("产品-" + code)
                .setRouteId(3001L)
                .setRouteCode("ROUTE-" + code)
                .setRouteName("路线-" + code)
                .setStatus(status)
                .setTaskTotal(0)
                .setTaskApprovedCount(0)
                .setBlockedCount(0);
        batchExecutionMapper.insert(batch);
        return batch;
    }
}
