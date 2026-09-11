package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;

@Service
public class DccProjectAccessServiceImpl implements DccProjectAccessService {

    @Resource
    private DccProjectCodeAssignmentMapper assignmentMapper;

    @Override
    public void assertProjectOwner(Long userId, Long projectCodeId) {
        if (userId == null || projectCodeId == null) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        boolean assignedOwner = assignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(userId, LocalDateTime.now())
                .stream()
                .anyMatch(projectCodeId::equals);
        if (!assignedOwner) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
    }

}
