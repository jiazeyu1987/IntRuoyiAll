package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesDeviationTransferEligibilityTest {
    private final MesProEdhrDeviationMapper deviations = mock(MesProEdhrDeviationMapper.class);
    private final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
    private final MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
    private MesProEdhrDeviationServiceImpl service;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(71L);
        service = new MesProEdhrDeviationServiceImpl(deviations, null, batches, null, null, null, null);
        ReflectionTestUtils.setField(service, "nonconformanceReviewMapper", reviews);
        when(deviations.selectByTenantAndId(71L, 1L)).thenReturn(new MesProEdhrDeviationDO()
                .setId(1L).setBatchExecutionId(2L).setLevel("CRITICAL").setStatus("OPEN"));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @ParameterizedTest @ValueSource(ints = {15,30,40,50,60})
    void blockedBatchExplainsNormalInvestigationPath(int status) {
        when(batches.selectByTenantIdAndId(71L,2L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(2L).setStatus(status));
        var response = service.get(1L);
        assertEquals(false, ReflectionTestUtils.getField(response,"canTransferToNcr"));
        assertTrue(((String)ReflectionTestUtils.getField(response,"transferToNcrBlockedReason")).contains("常规"));
        assertEquals("OPEN", response.getStatus());
    }
    @Test void restoredBatchRefreshesEligibility() {
        var batch = new MesProEdhrBatchExecutionDO().setId(2L).setStatus(15);
        when(batches.selectByTenantIdAndId(71L,2L)).thenReturn(batch);
        assertEquals(false, ReflectionTestUtils.getField(service.get(1L),"canTransferToNcr"));
        batch.setStatus(20);
        var response = service.get(1L);
        assertEquals(true, ReflectionTestUtils.getField(response,"canTransferToNcr"));
        assertNull(ReflectionTestUtils.getField(response,"transferToNcrBlockedReason"));
    }
    @Test void pendingReviewBlocksOtherwiseActiveBatch() {
        when(batches.selectByTenantIdAndId(71L,2L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(2L).setStatus(20));
        when(reviews.selectPendingByBatchExecutionId(2L)).thenReturn(new MesProEdhrNonconformanceReviewDO().setId(3L));
        assertEquals(false, ReflectionTestUtils.getField(service.get(1L),"canTransferToNcr"));
    }
    @Test void pageExposesSameEligibilityForBulkTransfer() {
        var row = new MesProEdhrDeviationDO().setId(1L).setBatchExecutionId(2L).setLevel("CRITICAL").setStatus("OPEN");
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<MesProEdhrDeviationDO>();
        page.setRecords(java.util.List.of(row)); page.setTotal(1L);
        doReturn(page).when(deviations).selectPageByTenant(any(), eq(71L), any(), any(), any(), any(), any(), any(), any(), any());
        when(batches.selectByTenantIdAndId(71L,2L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(2L).setStatus(50));
        var response = service.getPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO()).getList().get(0);
        assertEquals(false, response.getCanTransferToNcr());
        assertTrue(response.getTransferToNcrBlockedReason().contains("常规"));
    }
}
