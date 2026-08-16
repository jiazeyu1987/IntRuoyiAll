package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseBatchExecutionPortTest {

    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrBatchExecutionService batchExecutionService;

    private MesProductionReleaseBatchExecutionPort port;

    @BeforeEach
    void setUp() {
        port = new MesProductionReleaseBatchExecutionPortImpl(batchExecutionMapper, batchExecutionService);
    }

    @Test
    void createsBatchWithApplicationUniqueContextAndFrozenRouteVersion() {
        when(batchExecutionService.openOrCreateFromProductionRelease(any())).thenReturn(901L);

        assertEquals(901L, port.openOrCreate(command()));
        verify(batchExecutionService).openOrCreateFromProductionRelease(
                org.mockito.ArgumentMatchers.argThat(item ->
                        "PQC_RELEASE:701".equals(item.getActiveContextKey())
                                && Long.valueOf(402L).equals(item.getRouteVersionId())));
    }

    @Test
    void legacyContextCannotBeReused() {
        when(batchExecutionMapper.selectByContext(301L, "BATCH-001", 401L))
                .thenReturn(new MesProEdhrBatchExecutionDO().setId(999L).setActiveContextKey("301|401|BATCH-001"));

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class, () -> port.openOrCreate(command()));

        assertEquals(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(batchExecutionService, never()).openOrCreateFromProductionRelease(any());
    }

    private MesProductionReleaseBatchExecutionCommand command() {
        return new MesProductionReleaseBatchExecutionCommand()
                .setApplicationId(701L)
                .setWorkOrderId(301L)
                .setBatchCode("BATCH-001")
                .setRouteId(401L)
                .setRouteVersionId(402L);
    }
}
