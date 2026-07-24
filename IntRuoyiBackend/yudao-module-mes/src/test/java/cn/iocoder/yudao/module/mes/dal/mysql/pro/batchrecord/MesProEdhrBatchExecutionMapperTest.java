package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrBatchExecutionMapperTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;

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
