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
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "deviation-page");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, MesProEdhrBatchExecutionDO.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, MesProEdhrNonconformanceReviewDO.class);
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
    @ParameterizedTest @ValueSource(ints = {15, 30, 40, 50, 60})
    void pageExposesSameEligibilityForBulkTransfer(int status) {
        var row = new MesProEdhrDeviationDO().setId(1L).setBatchExecutionId(2L).setLevel("CRITICAL").setStatus("OPEN");
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<MesProEdhrDeviationDO>();
        page.setRecords(java.util.List.of(row)); page.setTotal(1L);
        doReturn(page).when(deviations).selectPageByTenant(any(), eq(71L), any(), any(), any(), any(), any(), any(), any(), any());
        when(batches.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of(new MesProEdhrBatchExecutionDO().setId(2L).setStatus(status)));
        var response = service.getPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO()).getList().get(0);
        assertEquals(false, response.getCanTransferToNcr());
        assertTrue(response.getTransferToNcrBlockedReason().contains("常规"));
        verifyNoInteractions(reviews);
    }

    @Test void pageBatchesMixedEligibilityAndLinkedReviewsWithoutPerRowQueries() {
        var rows = java.util.List.of(
                row(1L, 2L, "OPEN", "CRITICAL"),
                row(2L, 2L, "OPEN", "CRITICAL"),
                row(3L, 3L, "OPEN", "CRITICAL"),
                row(4L, 4L, "OPEN", "CRITICAL"),
                row(5L, 5L, "CLOSED", "CRITICAL").setNonconformanceReviewId(9L),
                row(6L, 6L, "OPEN", "NORMAL"),
                row(7L, null, "OPEN", "CRITICAL"));
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<MesProEdhrDeviationDO>();
        page.setRecords(rows); page.setTotal(107L);
        doReturn(page).when(deviations).selectPageByTenant(any(), eq(71L), any(), any(), any(), any(), any(), any(), any(), any());
        when(batches.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(java.util.List.of(
                new MesProEdhrBatchExecutionDO().setId(2L).setStatus(20),
                new MesProEdhrBatchExecutionDO().setId(3L).setStatus(20)));
        when(reviews.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of(new MesProEdhrNonconformanceReviewDO().setBatchExecutionId(3L)))
                .thenReturn(java.util.List.of(new MesProEdhrNonconformanceReviewDO().setId(9L)
                        .setReviewCode("NCR-9").setReviewStatus("closed").setDisposition("rework")));
        var result = service.getPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO());
        assertEquals(107L, result.getTotal());
        assertEquals(java.util.List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L), result.getList().stream().map(r -> r.getId()).toList());
        assertTrue(result.getList().get(0).getCanTransferToNcr());
        assertTrue(result.getList().get(1).getCanTransferToNcr());
        assertTrue(result.getList().get(2).getTransferToNcrBlockedReason().contains("已有待处置"));
        assertTrue(result.getList().get(3).getTransferToNcrBlockedReason().contains("状态缺失"));
        assertTrue(result.getList().get(4).getTransferToNcrBlockedReason().contains("仅未关闭"));
        assertEquals("NCR-9", result.getList().get(4).getNonconformanceReviewCode());
        assertEquals("rework", result.getList().get(4).getNonconformanceDisposition());
        assertTrue(result.getList().get(5).getTransferToNcrBlockedReason().contains("仅未关闭"));
        assertTrue(result.getList().get(6).getTransferToNcrBlockedReason().contains("状态缺失"));
        var batchQuery = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class);
        verify(batches, times(1)).selectList(batchQuery.capture());
        assertTrue(batchQuery.getValue().getSqlSegment().contains("tenant_id ="));
        assertTrue(batchQuery.getValue().getParamNameValuePairs().containsValue(71L));
        assertEquals(4, batchQuery.getValue().getParamNameValuePairs().size()); // tenant + distinct candidate batch IDs
        var reviewQueries = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class);
        verify(reviews, times(2)).selectList(reviewQueries.capture());
        for (var query : reviewQueries.getAllValues()) {
            assertTrue(query.getSqlSegment().contains("tenant_id ="));
            assertTrue(query.getParamNameValuePairs().containsValue(71L));
        }
        verify(reviews, times(2)).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(batches, never()).selectByTenantIdAndId(any(), any());
        verify(reviews, never()).selectPendingByBatchExecutionId(any());
        verify(reviews, never()).selectById(any(java.io.Serializable.class));
    }

    @Test void emptyPageDoesNotQueryAssociations() {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<MesProEdhrDeviationDO>();
        page.setRecords(java.util.List.of()); page.setTotal(0L);
        doReturn(page).when(deviations).selectPageByTenant(any(), eq(71L), any(), any(), any(), any(), any(), any(), any(), any());
        assertTrue(service.getPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO()).getList().isEmpty());
        verifyNoInteractions(batches, reviews);
    }

    @Test void multiplePendingReviewsStillFailInsteadOfBeingSilentlyDeduplicated() {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<MesProEdhrDeviationDO>();
        page.setRecords(java.util.List.of(row(1L, 2L, "OPEN", "CRITICAL"))); page.setTotal(1L);
        doReturn(page).when(deviations).selectPageByTenant(any(), eq(71L), any(), any(), any(), any(), any(), any(), any(), any());
        when(batches.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of(new MesProEdhrBatchExecutionDO().setId(2L).setStatus(20)));
        when(reviews.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of(
                        new MesProEdhrNonconformanceReviewDO().setId(10L).setBatchExecutionId(2L),
                        new MesProEdhrNonconformanceReviewDO().setId(11L).setBatchExecutionId(2L)));
        var error = assertThrows(org.apache.ibatis.exceptions.TooManyResultsException.class,
                () -> service.getPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO()));
        assertEquals("Expected one result (or null) to be returned by selectOne(), but found: 2", error.getMessage());
    }

    private MesProEdhrDeviationDO row(Long id, Long batchId, String status, String level) {
        return new MesProEdhrDeviationDO().setId(id).setBatchExecutionId(batchId).setStatus(status).setLevel(level);
    }
}
