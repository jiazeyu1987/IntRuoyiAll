package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.List;
import java.util.stream.LongStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchIntegrityServiceTest {
    @Mock MesProEdhrBatchExecutionMapper batchMapper;
    @Mock MesProEdhrBatchSearchMapper searchMapper;
    @Mock MesProEdhrBatchVisibilityQueryService visibilityService;
    @Mock MesProEdhrBatchLookupService lookupService;
    @InjectMocks MesProEdhrBatchIntegrityService service;

    @BeforeEach void prepare() {
        when(visibilityService.prepare(any(), eq(7L)))
                .thenReturn(new MesProEdhrBatchVisibilityQuery(1L, 7L, true, List.of()));
        when(batchMapper.buildPageQuery(any())).thenAnswer(call -> new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>());
    }
    private MesProEdhrBatchExecutionDO batch(long id) {
        var batch = new MesProEdhrBatchExecutionDO();
        batch.setId(id); batch.setBatchExecutionCode("B-" + id); batch.setTenantId(1L);
        return batch;
    }
    private EdhrBatchIntegrityChunkReqVO request() {
        var request = new EdhrBatchIntegrityChunkReqVO(); request.setUpperId(1000L); return request;
    }
    @Test void firstChunkPinsVisibleUpperBoundAndBindsCursor() {
        when(searchMapper.selectIntegrityUpperId(eq(1L), any())).thenReturn(200L);
        when(searchMapper.selectIntegrityChunk(eq(1L), any(), eq(0L), eq(200L), eq(128)))
                .thenReturn(List.of(batch(2)));
        var result = service.check(new EdhrBatchIntegrityChunkReqVO(), 7L);
        assertEquals(200L, result.getUpperId()); assertEquals(2L, result.getNextId());
        verify(batchMapper, never()).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test void malformedEvidenceIsReportedAndFollowingBatchStillChecked() {
        var first = batch(1); var second = batch(2);
        when(searchMapper.selectIntegrityChunk(eq(1L), any(), eq(0L), eq(1000L), eq(128))).thenReturn(List.of(first, second));
        doThrow(new ServiceException(400, "SOURCE_MISSING: snapshot")).when(lookupService).validateAllFacts(List.of(first));
        var result = service.check(request(), 7L);
        assertEquals(2, result.getChecked()); assertEquals(2L, result.getNextId());
        assertEquals("COMPLETED", result.getStatus()); assertEquals(1, result.getErrors().size());
        assertEquals(1L, result.getErrors().get(0).batchId());
        verify(lookupService).validateAllFacts(List.of(second));
        var scope = ArgumentCaptor.forClass(EdhrBatchExecutionPageReqVO.class);
        verify(visibilityService).prepare(scope.capture(), eq(7L));
        assertTrue(scope.getValue().getCompletedTraceOnly()); assertEquals(List.of(50), scope.getValue().getExcludeStatuses());
    }
    @Test void connectionFailureAbortsInsteadOfPretendingBatchEvidenceFailed() {
        var first = batch(1);
        when(searchMapper.selectIntegrityChunk(eq(1L), any(), eq(0L), eq(1000L), eq(128))).thenReturn(List.of(first));
        doThrow(new DataAccessResourceFailureException("database offline")).when(lookupService).validateAllFacts(List.of(first));
        assertThrows(DataAccessResourceFailureException.class, () -> service.check(request(), 7L));
    }
    @Test void permissionFailureAborts() {
        var first = batch(1);
        when(searchMapper.selectIntegrityChunk(eq(1L), any(), eq(0L), eq(1000L), eq(128))).thenReturn(List.of(first));
        doThrow(new ServiceException(403, "forbidden")).when(lookupService).validateAllFacts(List.of(first));
        assertThrows(ServiceException.class, () -> service.check(request(), 7L));
    }
    @Test void fullChunkReturnsCursorAndDoesNotCheckBeyond128() {
        var batches = LongStream.rangeClosed(1, 128).mapToObj(this::batch).toList();
        when(searchMapper.selectIntegrityChunk(eq(1L), any(), eq(0L), eq(1000L), eq(128))).thenReturn(batches);
        var result = service.check(request(), 7L);
        assertEquals(128, result.getChecked()); assertEquals(128L, result.getNextId());
        assertEquals("RUNNING", result.getStatus()); verify(lookupService, times(128)).validateAllFacts(any());
    }
}
