package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantAuditDO;
import cn.iocoder.yudao.module.system.service.permission.bo.TemporaryRoleGrantCreateCommand;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface TemporaryRoleGrantService {

    Long createGrant(TemporaryRoleGrantCreateCommand command);

    void approveGrant(Long id, Long approverUserId, String approverUsername);

    void revokeGrant(Long id, Long revokerUserId, String revokerUsername, String reason);

    int expireOverdueGrants(LocalDateTime now, Long operatorUserId, String operatorUsername);

    Set<Long> getActiveRoleIdsByUserId(Long userId, LocalDateTime now);

    void recordPermissionUse(Long userId, String permissionCode);

    PageResult<TemporaryRoleGrantRespVO> getGrantPage(TemporaryRoleGrantPageReqVO reqVO);

    List<TemporaryRoleGrantAuditDO> getAuditList(Long grantId);

}
