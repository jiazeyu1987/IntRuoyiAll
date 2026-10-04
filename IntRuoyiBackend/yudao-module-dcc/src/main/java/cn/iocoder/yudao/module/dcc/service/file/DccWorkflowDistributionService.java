package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkflowDistributionReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkflowDistributionRecipientRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Saves the actual per-file recipient selection, then performs the existing reliable delivery action. */
@Service
public class DccWorkflowDistributionService {
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileDistributionMapper distributionMapper;
    @Resource private DccControlledFileDistributionRecipientMapper recipientMapper;
    @Resource private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource private AdminUserApi adminUserApi;
    @Resource private DeptApi deptApi;
    @Resource private DccControlledFileFinalizationService finalizationService;
    @Resource private PermissionApi permissionApi;

    public List<DccWorkflowDistributionRecipientRespVO> recipientOptions(Long userId, Long fileId, Long departmentId) {
        var file=controlledFileMapper.selectById(fileId);
        if(userId==null || !permissionApi.hasAnyRoles(userId,"doc_control")
                || file==null || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || file.getControlledTime()==null || !("ACTIVE".equals(file.getStatus()) || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()))
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.DISTRIBUTE))
            throw new IllegalArgumentException("当前文件或办理资格不允许读取下发接收人");
        if(departmentId==null || departmentId<=0) throw new IllegalArgumentException("请选择有效接收部门");
        deptApi.validateDeptList(List.of(departmentId));
        var users=adminUserApi.getUserListByDeptIds(List.of(departmentId));
        if(users==null) throw new IllegalStateException("下发接收人目录返回缺失");
        if(users.stream().anyMatch(Objects::isNull)) throw new IllegalStateException("下发接收人目录包含空账号");
        if(users.stream().map(cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO::getId).distinct().count()!=users.size())
            throw new IllegalStateException("下发接收人目录包含重复身份");
        return users.stream().filter(user->Integer.valueOf(0).equals(user.getStatus()))
                .map(user -> {
                    if(user.getId()==null || user.getId()<=0 || !Objects.equals(departmentId,user.getDeptId()))
                        throw new IllegalStateException("下发接收人目录账号与部门身份不匹配");
                    String name=user.getNickname();
                    if(name==null || name.isBlank()) name=user.getUsername();
                    if(name==null || name.isBlank()) throw new IllegalStateException("下发接收人姓名未维护");
                    return new DccWorkflowDistributionRecipientRespVO(user.getId(),name);
                }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void distribute(Long userId, Long fileId, DccWorkflowDistributionReqVO request) {
        var file=controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),fileId);
        if(userId==null || !permissionApi.hasAnyRoles(userId,"doc_control") || file==null || file.getControlledTime()==null
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.DISTRIBUTE))
            throw new IllegalArgumentException("文件尚未受控或无下发资格");
        List<Plan> plans=validate(request,false);
        String digest=DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(plans));
        if(file.getDistributedTime()!=null) {
            if(!digest.equals(file.getDistributionPayloadHash())) throw new IllegalArgumentException("已下发内容与重放请求不一致");
            return;
        }
        if (!("ACTIVE".equals(file.getStatus()) || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus())))
            throw new IllegalArgumentException("当前文件状态不允许首次下发");
        validate(request,true);
        if(!distributionMapper.selectListByControlledFileId(fileId).isEmpty())
            throw new IllegalStateException("文件已有下发名单，不能覆盖既有下发记录");
        for(var plan:plans) {
            var distribution=DccControlledFileDistributionDO.builder().controlledFileId(fileId)
                    .departmentId(plan.departmentId()).distributionMedium(plan.medium()).status("PENDING").build();
            if(distributionMapper.insert(distribution)!=1 || distribution.getId()==null)
                throw new IllegalStateException("下发范围保存失败");
            for(Long user:plan.users()) {
                if(recipientMapper.insert(DccControlledFileDistributionRecipientDO.builder()
                        .distributionId(distribution.getId()).userId(user).build())!=1)
                    throw new IllegalStateException("下发接收人保存失败");
            }
        }
        if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(fileId).distributionPayloadHash(digest).build())!=1)
            throw new IllegalStateException("下发请求证据保存失败");
        // The finalization command dispatches the saved electronic recipient rows and stamps distributedTime.
        finalizationService.releaseManualDistribution(userId,fileId);
    }

    private List<Plan> validate(DccWorkflowDistributionReqVO request, boolean checkDirectory) {
        if(request==null || request.getScopes()==null || request.getScopes().isEmpty())
            throw new IllegalArgumentException("请确认下发部门、人员和方式");
        List<Plan> plans=new ArrayList<>();Set<Long> departments=new HashSet<>();
        for(var scope:request.getScopes()) {
            if(scope==null || scope.getDepartmentId()==null || !departments.add(scope.getDepartmentId())
                    || !("PAPER".equals(scope.getDistributionMedium()) || "PUBLIC_FOLDER".equals(scope.getDistributionMedium())))
                throw new IllegalArgumentException("下发部门重复或方式无效");
            if(checkDirectory) deptApi.validateDeptList(List.of(scope.getDepartmentId()));
            List<Long> users=scope.getRecipientUserIds()==null ? List.of():scope.getRecipientUserIds();
            if(users.stream().anyMatch(Objects::isNull) || new HashSet<>(users).size()!=users.size()
                    || "PUBLIC_FOLDER".equals(scope.getDistributionMedium()) && users.isEmpty())
                throw new IllegalArgumentException("电子下发必须选择有效接收人");
            if(checkDirectory && !users.isEmpty()) {
                var directory=adminUserApi.getUserList(users);
                if(directory==null || directory.size()!=users.size() || directory.stream().anyMatch(user ->
                        user==null || !Integer.valueOf(0).equals(user.getStatus())
                                || !Objects.equals(user.getDeptId(),scope.getDepartmentId()) || !users.contains(user.getId())))
                    throw new IllegalArgumentException("下发接收人已失效或不属于所选部门");
                Set<Long> returnedUserIds=directory.stream().map(cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO::getId)
                        .collect(java.util.stream.Collectors.toSet());
                if(returnedUserIds.size()!=directory.size() || !returnedUserIds.equals(new HashSet<>(users)))
                    throw new IllegalArgumentException("接收人目录返回重复或缺失账号，不能保存下发名单");
            }
            plans.add(new Plan(scope.getDepartmentId(),scope.getDistributionMedium(),users.stream().sorted().toList()));
        }
        return plans.stream().sorted(Comparator.comparing(Plan::departmentId)).toList();
    }
    private record Plan(Long departmentId,String medium,List<Long> users) {}
}
