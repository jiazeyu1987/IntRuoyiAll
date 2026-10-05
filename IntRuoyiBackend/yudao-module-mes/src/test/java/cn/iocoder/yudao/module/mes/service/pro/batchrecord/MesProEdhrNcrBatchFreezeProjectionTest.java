package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesProEdhrNcrBatchFreezeProjectionTest {

    @Test
    void batchDetailFrozenProjectionIncludesActiveOrderReviewWithoutDirectBatchLink() {
        var service = new MesProEdhrNonconformanceReviewServiceImpl();
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        var reviewMapper = mock(MesProEdhrNonconformanceReviewMapper.class);
        var originMapper = mock(MesProEdhrBatchExecutionOriginMapper.class);
        var batchMapper = mock(MesProEdhrBatchExecutionMapper.class);
        var review = MesProEdhrNonconformanceReviewDO.builder().id(55L)
                .reviewCode("BHGSP-202609-00000000")
                .sourceType("ACTIVE_ORDER").sourceId(33L).activeOrderId(33L).workOrderId(44L)
                .reviewStatus(MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW)
                .nonconformanceReason("active order review").build();
        ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", originMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchMapper);
        when(originMapper.selectListByBatchExecutionId(22L)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setActiveOrderId(33L)));
        when(reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(33L)).thenReturn(review);
        when(batchMapper.selectById(22L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(22L).setWorkOrderId(44L));

        assertTrue(service.isBatchFrozen(22L));
    }

    @Test
    void ensureBatchNotFrozenBlocksIndirectActiveOrderReview() {
        var service = new MesProEdhrNonconformanceReviewServiceImpl();
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        var reviewMapper = mock(MesProEdhrNonconformanceReviewMapper.class);
        var originMapper = mock(MesProEdhrBatchExecutionOriginMapper.class);
        var batchMapper = mock(MesProEdhrBatchExecutionMapper.class);
        var review = MesProEdhrNonconformanceReviewDO.builder().id(55L)
                .reviewCode("BHGSP-202609-00000000")
                .sourceType("ACTIVE_ORDER").sourceId(33L).activeOrderId(33L).workOrderId(44L)
                .reviewStatus(MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW)
                .nonconformanceReason("active order review").build();
        ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", originMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchMapper);
        when(originMapper.selectListByBatchExecutionId(22L)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setActiveOrderId(33L)));
        when(reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(33L)).thenReturn(review);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.ensureBatchNotFrozen(22L, "PQC放行"));

        assertTrue(error.getMessage().contains("BHGSP-202609-00000000"));
    }
}
