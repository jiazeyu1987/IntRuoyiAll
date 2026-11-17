package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProductionQuantityCorrectionFifoTest {
    @ParameterizedTest
    @CsvSource({"501,500", "499,499"})
    void leaderCorrectionKeepsExistingFifoConsumers(String output, String consumed) {
        var lines = mock(MesProcessPoolFifoAllocationLineMapper.class);
        when(lines.selectListBySourceQuantityFragmentIdsForUpdate(List.of(7L)))
                .thenReturn(List.of(MesProcessPoolFifoAllocationLineDO.builder()
                        .allocatedQuantity(new BigDecimal(consumed)).build()));
        var events = mock(MesProProcessPoolEventMapper.class);
        var revisions = mock(MesProProcessPoolEventRevisionMapper.class);
        var diffs = mock(MesProProcessPoolEventRevisionDiffMapper.class);
        var service = new MesProcessPoolEventRevisionServiceImpl(events, revisions, diffs,
                new MesProcessPoolFifoAllocationService(lines), mock(MesProcessPoolSubmissionReviewMapper.class),
                mock(MesProBatchRecordExecutionSignatureService.class));
        ReflectionTestUtils.setField(service, "nonconformanceReviewService",
                mock(MesProEdhrNonconformanceReviewService.class));
        when(events.selectByIdForUpdate(1L)).thenReturn(MesProProcessPoolEventDO.builder()
                .id(1L).eventType("PRODUCTION_SUBMIT").rawPayload("{\"outputQuantity\":500}").build());
        var request = MesProcessPoolEventRevisionUpdateReqBO.builder().eventId(1L)
                .afterPayload("{\"outputQuantity\":" + output + "}").changeReason("组长核对数量")
                .revisionSignatureId(9L).revisionSignatureUserId(10L).modifiedByUserId(10L)
                .revisionSignatureSnapshot("{\"signatureId\":9}")
                .changedFields(List.of(MesProcessPoolEventRevisionFieldChangeBO.builder()
                        .fieldCode("OUTPUT_QUANTITY").fieldName("完成数量").beforeValue("500")
                        .afterValue(output).affectsQuantityFragment(true).sourceQuantityFragmentId(7L)
                        .originalField(MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY).build())).build();
        assertDoesNotThrow(() -> service.updateProductionReportRecord(request));
        verify(events).updateById(any(MesProProcessPoolEventDO.class));
        verify(revisions).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(diffs).insert(any(MesProProcessPoolEventRevisionDiffDO.class));
        verify(lines, never()).supersedeCurrentRows(any(), any());
        verify(lines, never()).insertBatch(anyCollection());
    }

    @Test
    void quantityCannotDropBelowCurrentFifoConsumption() {
        var lines = mock(MesProcessPoolFifoAllocationLineMapper.class);
        when(lines.selectListBySourceQuantityFragmentIdsForUpdate(List.of(7L)))
                .thenReturn(List.of(MesProcessPoolFifoAllocationLineDO.builder()
                        .allocatedQuantity(new BigDecimal("500")).build()));
        var service = new MesProcessPoolFifoAllocationService(lines);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.validateOutputQuantityMutationAllowed(7L, new BigDecimal("499")));
    }

    @Test
    void otherAllocationAffectingFieldsRemainLocked() {
        var lines = mock(MesProcessPoolFifoAllocationLineMapper.class);
        when(lines.selectCountBySourceQuantityFragmentId(7L)).thenReturn(1L);
        var service = new MesProcessPoolFifoAllocationService(lines);
        for (var field : MesProcessPoolFragmentOriginalField.values()) {
            if (field.isAllocationAffecting()) {
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> service.validateOriginalFieldMutationAllowed(7L, field));
            }
        }
    }
}
