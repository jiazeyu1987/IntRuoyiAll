package cn.iocoder.yudao.module.infra.service.internuser;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.internuser.vo.InternUserFileUploadTimeUpdateReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.internuser.InternUserTimeMaintenanceAuditDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.dal.mysql.internuser.InternUserTimeMaintenanceAuditMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_FILE_UPLOAD_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import({InternUserTimeMaintenanceServiceImpl.class, InternUserTimeMaintenanceAuditServiceImpl.class})
public class InternUserTimeMaintenanceServiceImplTest extends BaseDbUnitTest {

    @Resource
    private InternUserTimeMaintenanceServiceImpl timeMaintenanceService;

    @Resource
    private FileMapper fileMapper;
    @Resource
    private InternUserTimeMaintenanceAuditMapper auditMapper;

    @Test
    public void testUpdateFileUploadTime_success() {
        LocalDateTime oldUploadTime = buildTime(2026, 9, 1);
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setCreateTime(oldUploadTime));
        fileMapper.insert(dbFile);
        LocalDateTime newUploadTime = buildTime(2026, 8, 20);
        InternUserFileUploadTimeUpdateReqVO reqVO = new InternUserFileUploadTimeUpdateReqVO()
                .setId(dbFile.getId())
                .setCreateTime(newUploadTime);

        timeMaintenanceService.updateFileUploadTime(reqVO);

        FileDO updated = fileMapper.selectById(dbFile.getId());
        assertEquals(newUploadTime, updated.getCreateTime());
        assertEquals(dbFile.getName(), updated.getName());
        assertEquals(dbFile.getPath(), updated.getPath());

        InternUserTimeMaintenanceAuditDO audit = auditMapper
                .selectListByTarget(TARGET_FILE_UPLOAD_TIME, dbFile.getId())
                .get(0);
        assertNull(audit.getTenantId());
        assertEquals(dbFile.getId(), audit.getTargetId());
        assertEquals(dbFile.getName(), audit.getTargetName());
        assertEquals("createTime", audit.getFieldName());
        assertEquals(oldUploadTime, audit.getOldTime());
        assertEquals(newUploadTime, audit.getNewTime());
    }

    @Test
    public void testUpdateFileUploadTime_notExists() {
        InternUserFileUploadTimeUpdateReqVO reqVO = new InternUserFileUploadTimeUpdateReqVO()
                .setId(randomLongId())
                .setCreateTime(buildTime(2026, 8, 20));

        assertServiceException(() -> timeMaintenanceService.updateFileUploadTime(reqVO), FILE_NOT_EXISTS);
    }

}
