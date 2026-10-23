package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductIdentityClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID;

/** 每个正式业务事务记录实际保存行；不为失败资产补成功事件。 */
@Service
public class DccProjectProductAuditService {
    public record Snapshot(String state, String json) {}
    @Resource private DccProjectProductCreateRequestMapper requests;
    @Resource private DccProjectProductIdentityClaimMapper claims;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProductCatalogMapper products;
    @Resource private DccProjectProductRelationMapper relations;
    @Resource private DccProjectFolderMapper folders;
    @Resource private DccProjectAccessRuleMapper projectAccessRules;
    @Resource private DccFolderTemplateMapper templates;
    @Resource private DccProjectConfigurationAuditService reasons;
    @Resource private GxpAuditService ledger;

    public void validateReason(String reason) { reasons.validateReason(reason); }
    public Snapshot beforeCreate(Long templateId) {
        var template=templateId==null?null:templates.selectByIdForUpdate(templateId);
        if(template==null || !Objects.equals(template.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        return new Snapshot("ABSENT",JsonUtils.toJsonString(Map.of("sourceTemplate",template)));
    }
    public Snapshot snapshot(Long requestId) {
        var request=requests.selectById(requestId);Long tenant=TenantContextHolder.getRequiredTenantId();
        if(request==null || !Objects.equals(request.getTenantId(),tenant)) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("request",request);
        payload.put("identityClaims",claims.selectList(new LambdaQueryWrapperX<DccProjectProductIdentityClaimDO>()
                .eq(DccProjectProductIdentityClaimDO::getTenantId,tenant).eq(DccProjectProductIdentityClaimDO::getRequestId,requestId)
                .orderByAsc(DccProjectProductIdentityClaimDO::getIdentityType)));
        if(request.getPreviousRequestId()!=null) payload.put("rejectedPredecessor",requests.selectById(request.getPreviousRequestId()));
        if("COMPLETED".equals(request.getStatus())) {
            var project=projects.selectById(request.getGeneratedProjectCodeId());
            var product=products.selectById(request.getGeneratedProductCatalogId());
            var relation=relations.selectById(request.getRelationId());
            if(project==null || product==null || relation==null || !Objects.equals(project.getTenantId(),tenant)
                    || !Objects.equals(relation.getTenantId(),tenant) || !Objects.equals(relation.getRequestId(),requestId)
                    || !Objects.equals(relation.getProjectCodeId(),project.getId()) || !Objects.equals(relation.getProductCatalogId(),product.getId()))
                throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
            payload.put("generatedProject",project);payload.put("generatedProduct",product);payload.put("relation",relation);
            payload.put("projectFolders",folders.listByProject(project.getId()));
            payload.put("projectAccessRules", projectAccessRules.selectListByProjectCodeId(project.getId()).stream()
                    .filter(rule -> Objects.equals(rule.getTenantId(), tenant)).toList());
        }
        return new Snapshot(request.getStatus(),JsonUtils.toJsonString(payload));
    }
    public String attemptVersion(Integer attempt) {
        if(attempt==null || attempt<=0) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        return String.valueOf(attempt);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void append(String operation,Long requestId,String version,String reason,Snapshot before) {
        reasons.validateReason(reason);
        var after=snapshot(requestId);
        ledger.append(GxpAuditCommand.builder().operationId(operation)
                .subjectId("DCC_PROJECT_PRODUCT_REQUEST:"+requestId).subjectVersion(version).reason(reason.trim())
                .idempotencyKey("DCC:PROJECT_PRODUCT:"+requestId+":"+operation.substring("dcc.project-product.".length())+":"+version)
                .source("DccProjectProductAuditService.append")
                .beforeState(GxpAuditStateEnvelope.builder().state(before.state()).objectVersion(version).canonicalJson(before.json()).build())
                .afterState(GxpAuditStateEnvelope.builder().state(after.state()).objectVersion(version).canonicalJson(after.json()).build()).build());
    }
}
