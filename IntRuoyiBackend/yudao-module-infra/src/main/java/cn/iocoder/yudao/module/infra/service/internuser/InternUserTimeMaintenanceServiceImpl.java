package cn.iocoder.yudao.module.infra.service.internuser;

import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.controller.admin.internuser.vo.InternUserFileUploadTimeUpdateReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_FILE_UPLOAD_TIME;

@Service
public class InternUserTimeMaintenanceServiceImpl implements InternUserTimeMaintenanceService {

    @Resource
    private FileMapper fileMapper;
    @Resource
    private InternUserTimeMaintenanceAuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFileUploadTime(InternUserFileUploadTimeUpdateReqVO reqVO) {
        Assert.notNull(reqVO, "实习用户文件时间维护请求不能为空");
        Assert.notNull(reqVO.getId(), "文件编号不能为空");
        Assert.notNull(reqVO.getCreateTime(), "上传时间不能为空");
        FileDO file = validateFileExists(reqVO.getId());

        int updatedRows = fileMapper.update(null, new LambdaUpdateWrapper<FileDO>()
                .eq(FileDO::getId, reqVO.getId())
                .set(FileDO::getCreateTime, reqVO.getCreateTime()));
        Assert.isTrue(updatedRows == 1, "文件上传时间更新失败");
        auditService.recordTimeChange(null, TARGET_FILE_UPLOAD_TIME, file.getId(),
                file.getName(), "createTime", file.getCreateTime(), reqVO.getCreateTime(),
                SecurityFrameworkUtils.getLoginUserId());
    }

    private FileDO validateFileExists(Long id) {
        FileDO file = fileMapper.selectById(id);
        if (file == null) {
            throw exception(FILE_NOT_EXISTS);
        }
        return file;
    }

}
