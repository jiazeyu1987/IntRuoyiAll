package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.flowable.bpmn.model.BpmnModel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;

/** Consumes the sole exact bound workflow identity; never selects a second graph catalog. */
@Service
public class DccLifecycleEffectiveContractResolver {
    @Resource private DccBoundWorkflowContractResolver boundContracts;

    public DccLifecycleEffectiveContract resolve(DccControlledFileDO file) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(file==null || file.getId()==null || file.getId()<=0 || !Objects.equals(file.getTenantId(),tenant)
                || Boolean.TRUE.equals(file.getDeleted()) || file.getProcessInstanceId()==null || file.getProcessInstanceId().isBlank()
                || !file.getProcessInstanceId().equals(file.getProcessInstanceId().strip())
                || !Set.of(DccControlledFileProcessDefinitionKeys.UPLOAD,DccControlledFileProcessDefinitionKeys.REVISION)
                    .contains(file.getProcessDefinitionKey()==null?"":file.getProcessDefinitionKey()))
            throw new IllegalArgumentException("生效契约需要本租户精确文件与真实审批轮次");
        var bound=boundContracts.resolve(file);
        if(bound==null || !Objects.equals(tenant,bound.tenantId()) || !Objects.equals(file.getId(),bound.controlledFileId())
                || !Objects.equals(file.getProcessInstanceId(),bound.processInstanceId())
                || !Objects.equals(file.getProcessDefinitionKey(),bound.processKey())
                || bound.processDefinitionId()==null || bound.processDefinitionId().isBlank()
                || bound.deploymentId()==null || bound.deploymentId().isBlank()
                || bound.definitionDigest()==null || !bound.definitionDigest().matches("[a-f0-9]{64}") || bound.flowRule()==null)
            throw new IllegalStateException("生效契约与正式绑定流程身份不一致");
        DccLifecycleEffectiveContract.Rule rule;
        switch(bound.flowRule()) {
            case LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL -> {
                if(!"LEGACY_V4_ACTION".equals(bound.executionContract()))
                    throw new IllegalStateException("旧生效契约缺少正式历史图证明");
                rule=DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE;
            }
            case PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL -> {
                if(!"PUBLIC_MATRIX_NATIVE_V1".equals(bound.executionContract())
                        || bound.attributeRound()==null || bound.attributeRound()<=0)
                    throw new IllegalStateException("新生效契约缺少正式申请属性轮次");
                rule=DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE;
            }
            case PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM -> {
                if(!"PUBLIC_MATRIX_NATIVE_V2".equals(bound.executionContract()) || bound.attributeRound()==null || bound.attributeRound()<=0)
                    throw new IllegalStateException("延迟确认生效契约缺少正式新图和申请轮次");
                rule=DccLifecycleEffectiveContract.Rule.DELAYED_CONFIRMATION_MODE;
            }
            default -> throw new IllegalStateException("当前正式流程不支持上传或升版生效契约");
        }
        return new DccLifecycleEffectiveContract(tenant,file.getId(),bound.processInstanceId(),bound.processDefinitionId(),
                bound.deploymentId(),bound.processKey(),bound.definitionDigest(),rule);
    }

    public static String definitionDigest(BpmnModel model) {
        if(model==null) throw new IllegalArgumentException("流程图不能为空");
        String xml=BpmnModelUtils.getBpmnXml(model);
        if(xml==null || xml.isBlank()) throw new IllegalStateException("正式部署图序列化失败");
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(xml.replace("\r\n","\n").replace('\r','\n').getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException("SHA-256不可用",impossible);}
    }
}
