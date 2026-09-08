package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantAuditRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantRevokeReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.TemporaryRoleGrantService;
import cn.iocoder.yudao.module.system.service.permission.bo.TemporaryRoleGrantCreateCommand;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 临时角色授权")
@RestController
@RequestMapping("/system/temporary-role-grant")
@Validated
public class TemporaryRoleGrantController {

    @Resource
    private TemporaryRoleGrantService temporaryRoleGrantService;
    @Resource
    private AdminUserService userService;

    @PostMapping("/create")
    @Operation(summary = "创建临时角色授权申请")
    @PreAuthorize("@ss.hasPermission('system:temporary-role-grant:create')")
    public CommonResult<Long> createGrant(@Valid @RequestBody TemporaryRoleGrantCreateReqVO reqVO) {
        Long loginUserId = getLoginUserId();
        String loginUsername = getLoginUsername(loginUserId);
        return success(temporaryRoleGrantService.createGrant(TemporaryRoleGrantCreateCommand.builder()
                .userId(reqVO.getUserId())
                .roleId(reqVO.getRoleId())
                .reason(reqVO.getReason())
                .expireTime(reqVO.getExpireTime())
                .applicantUserId(loginUserId)
                .applicantUsername(loginUsername)
                .build()));
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过临时角色授权")
    @PreAuthorize("@ss.hasPermission('system:temporary-role-grant:approve')")
    public CommonResult<Boolean> approveGrant(@RequestParam("id") Long id) {
        Long loginUserId = getLoginUserId();
        temporaryRoleGrantService.approveGrant(id, loginUserId, getLoginUsername(loginUserId));
        return success(true);
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤销临时角色授权")
    @PreAuthorize("@ss.hasPermission('system:temporary-role-grant:revoke')")
    public CommonResult<Boolean> revokeGrant(@Valid @RequestBody TemporaryRoleGrantRevokeReqVO reqVO) {
        Long loginUserId = getLoginUserId();
        temporaryRoleGrantService.revokeGrant(reqVO.getId(), loginUserId, getLoginUsername(loginUserId), reqVO.getReason());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得临时角色授权分页")
    @PreAuthorize("@ss.hasPermission('system:temporary-role-grant:query')")
    public CommonResult<PageResult<TemporaryRoleGrantRespVO>> getGrantPage(@Valid TemporaryRoleGrantPageReqVO reqVO) {
        return success(temporaryRoleGrantService.getGrantPage(reqVO));
    }

    @GetMapping("/audit-list")
    @Operation(summary = "获得临时角色授权审计记录")
    @Parameter(name = "grantId", description = "授权记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:temporary-role-grant:query')")
    public CommonResult<List<TemporaryRoleGrantAuditRespVO>> getAuditList(@RequestParam("grantId") Long grantId) {
        return success(BeanUtils.toBean(temporaryRoleGrantService.getAuditList(grantId), TemporaryRoleGrantAuditRespVO.class));
    }

    private String getLoginUsername(Long loginUserId) {
        AdminUserDO user = userService.getUser(loginUserId);
        return user.getUsername();
    }

}
