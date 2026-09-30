package cn.iocoder.yudao.module.dcc.service.internuser;

import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.internuser.vo.DccInternUserTimeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_DCC_OBSOLETED_TIME;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_DCC_PUBLISHED_TIME;

@Service
public class DccInternUserTimeMaintenanceServiceImpl implements DccInternUserTimeMaintenanceService {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private InternUserTimeMaintenanceAuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePublishedTime(DccInternUserTimeUpdateReqVO reqVO) {
        DccControlledFileDO file = validateControlledFile(reqVO);
        updateTime(file, reqVO.getTargetTime(), "publishedTime", TARGET_DCC_PUBLISHED_TIME,
                file.getPublishedTime(), "published_time");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateObsoletedTime(DccInternUserTimeUpdateReqVO reqVO) {
        DccControlledFileDO file = validateControlledFile(reqVO);
        updateTime(file, reqVO.getTargetTime(), "obsoletedTime", TARGET_DCC_OBSOLETED_TIME,
                file.getObsoletedTime(), "obsoleted_time");
    }

    private DccControlledFileDO validateControlledFile(DccInternUserTimeUpdateReqVO reqVO) {
        Assert.notNull(reqVO, "实习用户 DCC 时间维护请求不能为空");
        Assert.notNull(reqVO.getControlledFileId(), "受控文件编号不能为空");
        Assert.notNull(reqVO.getTargetTime(), "目标时间不能为空");
        DccControlledFileDO file = controlledFileMapper.selectById(reqVO.getControlledFileId());
        Assert.notNull(file, "受控文件不存在");
        Assert.isFalse(Boolean.TRUE.equals(file.getDeleted()), "受控文件已删除");
        return file;
    }

    private void updateTime(DccControlledFileDO file, LocalDateTime newTime, String fieldName, String targetType,
                            LocalDateTime oldTime, String columnName) {
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        int updatedRows = controlledFileMapper.update(null, new UpdateWrapper<DccControlledFileDO>()
                .eq("id", file.getId())
                .eq("deleted", false)
                .set(columnName, newTime)
                .set("updater", actorId == null ? null : String.valueOf(actorId))
                .set("update_time", LocalDateTime.now()));
        Assert.isTrue(updatedRows == 1, "DCC 受控文件时间更新失败");
        auditService.recordTimeChange(file.getTenantId(), targetType, file.getId(), buildTargetName(file),
                fieldName, oldTime, newTime, actorId);
    }

    private String buildTargetName(DccControlledFileDO file) {
        if (file.getTitle() != null && !file.getTitle().isBlank()) {
            return file.getTitle();
        }
        if (file.getFileName() != null && !file.getFileName().isBlank()) {
            return file.getFileName();
        }
        return file.getFileNumber();
    }

}
