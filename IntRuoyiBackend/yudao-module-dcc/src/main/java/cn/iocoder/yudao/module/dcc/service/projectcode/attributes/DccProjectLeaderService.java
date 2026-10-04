package cn.iocoder.yudao.module.dcc.service.projectcode.attributes;

import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Objects;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

@Service
public class DccProjectLeaderService {
    @Resource private DccProjectCodeMapper projectCodeMapper;
    @Resource private AdminUserApi adminUserApi;

    public AdminUserRespDTO requireEnabledAccount(Long userId) {
        AdminUserRespDTO user = userId == null ? null : adminUserApi.getUser(userId);
        if (user == null || !Objects.equals(user.getStatus(), 0)) throw fail(LEADER_INVALID);
        return user;
    }
    /** 唯一项目负责人事实，与既有 OWNER/EDIT 授权规则独立，无 admin 旁路。 */
    public void assertProjectLeader(Long userId, Long projectId) {
        var project = projectId == null ? null : projectCodeMapper.selectById(projectId);
        if (project == null || !Objects.equals(project.getTenantId(), TenantContextHolder.getRequiredTenantId()) || !DccProjectCodeStatusConstants.ENABLE.equals(project.getStatus())
                || userId == null || !Objects.equals(userId, project.getProjectLeaderUserId())) {
            throw fail(LEADER_REQUIRED);
        }
        requireEnabledAccount(userId);
    }
}
