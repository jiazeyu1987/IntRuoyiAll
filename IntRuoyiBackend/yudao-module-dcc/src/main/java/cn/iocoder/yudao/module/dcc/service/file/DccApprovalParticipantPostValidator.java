package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED;

@Service
public class DccApprovalParticipantPostValidator {

    @Resource
    private AdminUserApi adminUserApi;

    public void requireConfiguredPosts(Collection<Long> userIds) {
        List<Long> normalizedUserIds = userIds == null ? List.of() : userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedUserIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserList(normalizedUserIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, user -> user,
                        (left, right) -> left, LinkedHashMap::new));
        List<String> missingPostUsers = normalizedUserIds.stream()
                .filter(userId -> {
                    AdminUserRespDTO user = userMap.get(userId);
                    return user == null || CollUtil.isEmpty(user.getPostIds());
                })
                .map(userId -> describeUser(userId, userMap.get(userId)))
                .toList();
        if (!missingPostUsers.isEmpty()) {
            throw exception(CONTROLLED_FILE_APPROVER_POST_REQUIRED);
        }
    }

    private String describeUser(Long userId, AdminUserRespDTO user) {
        if (user != null) {
            String displayName = StrUtil.blankToDefault(StrUtil.trim(user.getNickname()), user.getUsername());
            return StrUtil.isBlank(displayName) ? "用户#" + user.getId() : displayName + "(ID:" + user.getId() + ")";
        }
        return "用户#" + userId;
    }
}
