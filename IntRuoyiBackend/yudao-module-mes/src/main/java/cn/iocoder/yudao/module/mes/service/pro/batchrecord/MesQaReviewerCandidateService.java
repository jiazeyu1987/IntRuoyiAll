package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesQaReviewerCandidateRespVO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
public class MesQaReviewerCandidateService {
    @Resource private AdminUserMapper users;
    @Resource private PermissionApi permissions;

    public List<MesQaReviewerCandidateRespVO> list() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return users.selectListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .filter(user -> Objects.equals(tenantId, user.getTenantId())
                        && CommonStatusEnum.isEnable(user.getStatus()))
                .filter(user -> permissions.hasAnyPermissions(user.getId(),
                        "mes:pro-edhr-nonconformance-review:dispose"))
                .map(user -> {
                    var result = new MesQaReviewerCandidateRespVO();
                    result.setId(user.getId());
                    result.setUsername(user.getUsername());
                    result.setNickname(user.getNickname());
                    result.setDeptId(user.getDeptId());
                    result.setStatus(user.getStatus());
                    return result;
                }).toList();
    }
}
