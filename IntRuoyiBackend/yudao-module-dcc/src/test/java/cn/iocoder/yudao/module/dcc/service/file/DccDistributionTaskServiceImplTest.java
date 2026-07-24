package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccDistributionTaskServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;

    @InjectMocks
    private DccDistributionTaskServiceImpl distributionTaskService;

    @Test
    void getMyDistributionTaskPage_pendingElectronicRecipients_returnsOnlyAcknowledgementTasks() {
        LocalDateTime publishedTime = LocalDateTime.of(2026, 7, 20, 9, 30);
        DccDistributionTaskPageReqVO reqVO = new DccDistributionTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setStatus("READY_TO_ACKNOWLEDGE");
        when(distributionRecipientMapper.selectListByUserId(99L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(501L)
                        .distributionId(301L)
                        .userId(99L)
                        .messageJobId(8101L)
                        .build(),
                DccControlledFileDistributionRecipientDO.builder()
                        .id(502L)
                        .distributionId(302L)
                        .userId(99L)
                        .acknowledgedAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                        .build(),
                DccControlledFileDistributionRecipientDO.builder()
                        .id(503L)
                        .distributionId(303L)
                        .userId(99L)
                        .messageJobId(8102L)
                        .build()));
        when(distributionMapper.selectById(301L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(301L)
                .controlledFileId(900L)
                .departmentId(300L)
                .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                .status(DccControlledFileDistributionStatusEnum.SENT.getCode())
                .build());
        when(distributionMapper.selectById(303L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(303L)
                .controlledFileId(901L)
                .departmentId(301L)
                .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                .status(DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode())
                .build());
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(15L)
                .fileName("发行后分发确认.pdf")
                .title("发行后分发确认")
                .fileNumber("DCC-DIST-001")
                .versionNo("A")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .publishedTime(publishedTime)
                .build());

        PageResult<DccDistributionTaskRespVO> page =
                distributionTaskService.getMyDistributionTaskPage(99L, reqVO);

        assertEquals(1L, page.getTotal());
        DccDistributionTaskRespVO row = page.getList().get(0);
        assertEquals(501L, row.getRecipientId());
        assertEquals(301L, row.getDistributionId());
        assertEquals(900L, row.getControlledFileId());
        assertEquals(15L, row.getCategoryId());
        assertEquals("DCC-DIST-001", row.getFileNumber());
        assertEquals("发行后分发确认.pdf", row.getFileName());
        assertEquals("A", row.getVersionNo());
        assertEquals(99L, row.getUserId());
        assertEquals(300L, row.getDepartmentId());
        assertEquals("READY_TO_ACKNOWLEDGE", row.getStatus());
        assertEquals(publishedTime, row.getPublishedTime());
    }

    @Test
    void getMyDistributionTaskPage_missingDistribution_failsFast() {
        DccDistributionTaskPageReqVO reqVO = new DccDistributionTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(distributionRecipientMapper.selectListByUserId(99L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(501L)
                        .distributionId(301L)
                        .userId(99L)
                        .messageJobId(8101L)
                        .build()));
        when(distributionMapper.selectById(301L)).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> distributionTaskService.getMyDistributionTaskPage(99L, reqVO));
        assertEquals("DCC distribution recipient 501 references missing distribution 301", ex.getMessage());
        verify(distributionMapper).selectById(301L);
    }
}
