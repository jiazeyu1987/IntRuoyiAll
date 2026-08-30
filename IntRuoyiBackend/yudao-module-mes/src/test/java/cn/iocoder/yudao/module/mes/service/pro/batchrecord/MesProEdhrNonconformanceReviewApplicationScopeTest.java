package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrNonconformanceReviewApplicationScopeTest {

    @Mock private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;

    private MesProEdhrNonconformanceReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesProEdhrNonconformanceReviewServiceImpl();
        ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchExecutionMapper);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", releaseApplicationMapper);
    }

    @Test
    void pqcReleaseApplicationCanStartReviewBeforeBatchCreation() {
        when(releaseApplicationMapper.selectById(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-001")
                        .setBatchCode("BATCH-001"));
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1001L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_RELEASE")
                        .setSourceId(7001L)
                        .setNonconformanceReason("检验结论需要评审"));

        assertEquals(1001L, result.getId());
        assertEquals(3001L, result.getWorkOrderId());
        assertEquals("WO-001", result.getWorkOrderCode());
        assertNull(result.getBatchExecutionId());
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }
}
