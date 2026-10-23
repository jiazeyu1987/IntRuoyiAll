package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccDistributionTaskFactsTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccControlledFileDistributionMapper distributionMapper;
    @Mock DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @InjectMocks DccDistributionTaskServiceImpl service;
    DccControlledFileDO file;
    DccControlledFileDistributionDO distribution;
    DccDistributionTaskPageReqVO request;
    @BeforeEach void fixture() {
        request=new DccDistributionTaskPageReqVO();request.setPageNo(1);request.setPageSize(10);
        var now=LocalDateTime.of(2026,10,11,9,0);
        file=DccControlledFileDO.builder().id(42L).versionNo("B/1").status("ACTIVE")
                .controlledTime(now.minusDays(1)).activatedTime(now).distributedTime(now).build();
        distribution=DccControlledFileDistributionDO.builder().id(51L).controlledFileId(42L)
                .distributionMedium("PUBLIC_FOLDER").status("PENDING").build();
        when(distributionRecipientMapper.selectListByUserId(99L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder().id(71L).distributionId(51L).userId(99L).messageJobId(81L).build()));
        when(distributionMapper.selectById(51L)).thenAnswer(ignored->distribution);
        lenient().when(controlledFileMapper.selectById(42L)).thenAnswer(ignored->file);
    }
    @Test void pendingEffectiveHasNoAcknowledgementTask() {
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");file.setActivatedTime(null);
        assertEquals(0L,service.getMyDistributionTaskPage(99L,request).getTotal());
    }
    @Test void notDispatchedOrNotControlledHasNoTask() {
        file.setDistributedTime(null);
        assertEquals(0L,service.getMyDistributionTaskPage(99L,request).getTotal());
        file.setDistributedTime(LocalDateTime.now());file.setControlledTime(null);
        assertEquals(0L,service.getMyDistributionTaskPage(99L,request).getTotal());
    }
    @Test void recoveredAudienceHasNoTask() {
        distribution.setStatus("RECOVERED");
        assertEquals(0L,service.getMyDistributionTaskPage(99L,request).getTotal());
    }
    @Test void staleAudienceDoesNotBreakOtherPersonalTasks() {
        distribution=null;
        assertEquals(0L,service.getMyDistributionTaskPage(99L,request).getTotal());
    }
    @Test void exactHistoricalDispatchedVersionRemainsTheTaskIdentity() {
        file.setStatus("OBSOLETE");
        var rows=service.getMyDistributionTaskPage(99L,request).getList();
        assertEquals(1,rows.size());assertEquals(42L,rows.get(0).getControlledFileId());assertEquals("B/1",rows.get(0).getVersionNo());
    }
}
