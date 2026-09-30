package cn.iocoder.yudao.module.dcc.service.internuser;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.internuser.vo.DccInternUserTimeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_DCC_OBSOLETED_TIME;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_DCC_PUBLISHED_TIME;

class DccInternUserTimeMaintenanceServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccInternUserTimeMaintenanceServiceImpl timeMaintenanceService;

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private InternUserTimeMaintenanceAuditService auditService;

    @Test
    void testUpdatePublishedTime_success() {
        LocalDateTime oldTime = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime newTime = LocalDateTime.of(2026, 8, 20, 10, 30);
        DccControlledFileDO file = buildFile();
        file.setPublishedTime(oldTime);
        when(controlledFileMapper.selectById(100L)).thenReturn(file);
        when(controlledFileMapper.update(eq(null), any())).thenReturn(1);

        timeMaintenanceService.updatePublishedTime(new DccInternUserTimeUpdateReqVO()
                .setControlledFileId(100L)
                .setTargetTime(newTime));

        verify(auditService).recordTimeChange(1L, TARGET_DCC_PUBLISHED_TIME, 100L, "作业指导书",
                "publishedTime", oldTime, newTime, null);
    }

    @Test
    void testUpdateObsoletedTime_success() {
        LocalDateTime oldTime = LocalDateTime.of(2026, 9, 2, 10, 0);
        LocalDateTime newTime = LocalDateTime.of(2026, 8, 21, 10, 30);
        DccControlledFileDO file = buildFile();
        file.setObsoletedTime(oldTime);
        when(controlledFileMapper.selectById(100L)).thenReturn(file);
        when(controlledFileMapper.update(eq(null), any())).thenReturn(1);

        timeMaintenanceService.updateObsoletedTime(new DccInternUserTimeUpdateReqVO()
                .setControlledFileId(100L)
                .setTargetTime(newTime));

        verify(auditService).recordTimeChange(1L, TARGET_DCC_OBSOLETED_TIME, 100L, "作业指导书",
                "obsoletedTime", oldTime, newTime, null);
    }

    private DccControlledFileDO buildFile() {
        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(100L);
        file.setTenantId(1L);
        file.setTitle("作业指导书");
        file.setDeleted(false);
        assertEquals(100L, file.getId());
        return file;
    }

}
