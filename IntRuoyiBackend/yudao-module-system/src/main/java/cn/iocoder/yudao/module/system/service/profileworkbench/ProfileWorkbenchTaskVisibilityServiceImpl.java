package cn.iocoder.yudao.module.system.service.profileworkbench;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.profileworkbench.vo.ProfileWorkbenchTaskVisibilitySaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench.ProfileWorkbenchTaskVisibilityDO;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.ProfileWorkbenchTaskVisibilityMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID;

@Service
@Validated
public class ProfileWorkbenchTaskVisibilityServiceImpl implements ProfileWorkbenchTaskVisibilityService {

    private static final int MAX_TASK_KEY_LENGTH = 160;
    private static final int MAX_LABEL_LENGTH = 64;
    private static final int MAX_BUSINESS_ID_LENGTH = 80;
    private static final int MAX_DETAIL_LENGTH = 500;

    @Resource
    private ProfileWorkbenchTaskVisibilityMapper profileWorkbenchTaskVisibilityMapper;

    @Override
    public List<String> getHiddenTaskKeys() {
        Long loginUserId = getLoginUserId();
        return profileWorkbenchTaskVisibilityMapper.selectListByUser(loginUserId).stream()
                .map(ProfileWorkbenchTaskVisibilityDO::getTaskKey)
                .toList();
    }

    @Override
    public void hideTask(ProfileWorkbenchTaskVisibilitySaveReqVO reqVO) {
        validateSaveReqVO(reqVO);
        Long loginUserId = getLoginUserId();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now();
        String taskKey = StrUtil.trim(reqVO.getTaskKey());
        ProfileWorkbenchTaskVisibilityDO existing =
                profileWorkbenchTaskVisibilityMapper.selectByUserAndTaskKey(loginUserId, taskKey);
        if (existing == null) {
            ProfileWorkbenchTaskVisibilityDO createObj = new ProfileWorkbenchTaskVisibilityDO()
                    .setUserId(loginUserId)
                    .setTaskKey(taskKey)
                    .setTaskType(StrUtil.trim(reqVO.getTaskType()))
                    .setSource(StrUtil.trim(reqVO.getSource()))
                    .setBusinessId(trimToNull(reqVO.getBusinessId()))
                    .setDetail(trimToNull(reqVO.getDetail()))
                    .setHiddenAt(now);
            createObj.setTenantId(tenantId);
            profileWorkbenchTaskVisibilityMapper.insert(createObj);
            return;
        }
        existing.setTaskType(StrUtil.trim(reqVO.getTaskType()))
                .setSource(StrUtil.trim(reqVO.getSource()))
                .setBusinessId(trimToNull(reqVO.getBusinessId()))
                .setDetail(trimToNull(reqVO.getDetail()))
                .setHiddenAt(now);
        profileWorkbenchTaskVisibilityMapper.updateById(existing);
    }

    @Override
    public void restoreTask(String taskKey) {
        validateRequired(taskKey, MAX_TASK_KEY_LENGTH);
        profileWorkbenchTaskVisibilityMapper.deletePhysicalByUserAndTaskKey(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), StrUtil.trim(taskKey));
    }

    private void validateSaveReqVO(ProfileWorkbenchTaskVisibilitySaveReqVO reqVO) {
        if (reqVO == null) {
            throw exception(PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID);
        }
        validateRequired(reqVO.getTaskKey(), MAX_TASK_KEY_LENGTH);
        validateRequired(reqVO.getTaskType(), MAX_LABEL_LENGTH);
        validateRequired(reqVO.getSource(), MAX_LABEL_LENGTH);
        validateOptional(reqVO.getBusinessId(), MAX_BUSINESS_ID_LENGTH);
        validateOptional(reqVO.getDetail(), MAX_DETAIL_LENGTH);
    }

    private void validateRequired(String value, int maxLength) {
        String trimmed = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmed) || trimmed.length() > maxLength) {
            throw exception(PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID);
        }
    }

    private void validateOptional(String value, int maxLength) {
        String trimmed = StrUtil.trim(value);
        if (trimmed != null && trimmed.length() > maxLength) {
            throw exception(PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID);
        }
    }

    private String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private Long getLoginUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID);
        }
        return loginUserId;
    }
}
