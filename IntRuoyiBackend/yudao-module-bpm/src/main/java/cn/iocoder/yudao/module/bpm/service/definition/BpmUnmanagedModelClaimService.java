package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.flowable.engine.RepositoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

/** Explicit orphan-model maintenance; normal manager authorization remains mandatory. */
@Service
public class BpmUnmanagedModelClaimService {
    @Resource private RepositoryService repository;
    @Resource private PermissionApi permissions;
    @Resource private GxpAuditService audit;

    @Transactional(rollbackFor = Exception.class)
    @GxpWriteOperation(operationId="bpm.model.manager.claim")
    public void claim(Long actor, String id, String reason) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var login=SecurityFrameworkUtils.getLoginUser();
        if(TenantContextHolder.isIgnore() || login==null || !Objects.equals(login.getId(),actor)
                || !Objects.equals(login.getTenantId(),tenant) || !Objects.equals(login.getUserType(),2)
                || !permissions.hasAnyRoles(actor,"super_admin") || !permissions.hasAnyPermissions(actor,"bpm:model:update"))
            throw exception(MODEL_CLAIM_FORBIDDEN);
        if(id==null || id.isBlank() || reason==null || reason.isBlank() || reason.length()>500) throw exception(MODEL_CLAIM_REASON_REQUIRED);
        var model=repository.getModel(id);
        if(model==null || !id.equals(model.getId()) || !tenant.toString().equals(model.getTenantId())) throw exception(MODEL_NOT_EXISTS);
        Map<String,Object> metadata=JsonUtils.parseObject(model.getMetaInfo(),Map.class);
        if(metadata==null) throw exception(MODEL_NOT_EXISTS);
        Object oldManagers=metadata.get("managerUserIds");
        if(oldManagers!=null && (!(oldManagers instanceof List<?> ids) || !ids.isEmpty())) throw exception(MODEL_CLAIM_ALREADY_MANAGED);
        metadata=new LinkedHashMap<>(metadata);metadata.put("managerUserIds",List.of(actor));
        model.setMetaInfo(JsonUtils.toJsonString(metadata));
        repository.saveModel(model); // Flowable optimistic revision guards a concurrent claim/update.
        audit.append(GxpAuditCommand.builder().operationId("bpm.model.manager.claim").subjectId(id)
                .subjectVersion(String.valueOf(model.getVersion())).reason(reason.trim())
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT").canonicalJson("{\"managerUserIds\":[]}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").canonicalJson(JsonUtils.toJsonString(Map.of("managerUserIds",List.of(actor)))).build())
                .idempotencyKey("BPM:MODEL:MANAGER_CLAIM:"+tenant+":"+id+":"+actor).source("BpmUnmanagedModelClaimService.claim").build());
    }
}
