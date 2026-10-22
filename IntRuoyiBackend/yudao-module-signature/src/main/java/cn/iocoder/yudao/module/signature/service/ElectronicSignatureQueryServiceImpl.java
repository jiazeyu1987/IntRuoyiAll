package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.SignatureCreationVerificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

@Service
public class ElectronicSignatureQueryServiceImpl implements ElectronicSignatureQueryService {
    private static final ObjectMapper FACT_READER = new ObjectMapper();
    @Resource private ElectronicSignatureRecordMapper signatureRecordMapper;
    @Resource private SignatureCreationVerificationService signatureCreationVerificationService;

    @Override
    public ElectronicSignatureEvidenceDTO getById(Long signatureId) {
        positiveId(signatureId);
        var record = signatureRecordMapper.selectById(signatureId);
        return record == null || !Objects.equals(record.getTenantId(), TenantContextHolder.getRequiredTenantId())
                ? null : toEvidence(record);
    }

    @Override
    public List<ElectronicSignatureEvidenceDTO> listBySubject(String moduleCode, String subjectType, String subjectId) {
        if (StrUtil.hasBlank(moduleCode, subjectType, subjectId))
            throw exception(ESIGN_COMMAND_INVALID, "模块、对象类型和对象编号不能为空");
        return signatureRecordMapper.selectList(new LambdaQueryWrapper<ElectronicSignatureRecordDO>()
                .eq(ElectronicSignatureRecordDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ElectronicSignatureRecordDO::getModuleCode, moduleCode)
                .eq(ElectronicSignatureRecordDO::getSubjectType, subjectType)
                .eq(ElectronicSignatureRecordDO::getSubjectId, subjectId)
                .orderByAsc(ElectronicSignatureRecordDO::getSignedAt).orderByAsc(ElectronicSignatureRecordDO::getId))
                .stream().map(this::toEvidence).toList();
    }

    @Override
    public ElectronicSignatureVerificationDTO verifyEvidence(Long signatureId) {
        return new ElectronicSignatureVerifiedFactService(signatureRecordMapper, signatureCreationVerificationService)
                .verifyEvidence(signatureId);
    }

    private static boolean positive(Long id) { return id != null && id > 0; }
    private static void positiveId(Long id) { if (!positive(id)) throw exception(ESIGN_COMMAND_INVALID, "签名编号必须为正整数"); }
    private static String text(JsonNode node, String field) { var value = node.get(field); return value != null && value.isTextual() ? value.textValue() : null; }
    private static JsonNode readFact(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return FACT_READER.readTree(json); }
        catch (JsonProcessingException ex) { return null; }
    }
    private static String frozenName(JsonNode content) {
        var identity = content == null ? null : content.get("signatureIdentity");
        var name = identity == null || !identity.isObject() ? null : text(identity, "displayName");
        return StrUtil.isBlank(name) ? null : name;
    }

    private ElectronicSignatureEvidenceDTO toEvidence(ElectronicSignatureRecordDO r) {
        return new ElectronicSignatureEvidenceDTO(r.getId(), r.getModuleCode(), r.getActionCode(), r.getSubjectType(),
                r.getSubjectId(), r.getSubjectVersion(), r.getActorId(), r.getMeaningCode(), r.getMeaningLabel(), r.getReason(),
                r.getSignedAt(), r.getTimeEvidenceId(), r.getAuthenticationMethod(), r.getContentHash(), r.getEvidenceHash(),
                r.getAlgorithm(), r.getKeyVersion(), r.getPolicyVersion(), r.getVerificationStatus(), r.getProcessInstanceId(),
                r.getTaskId(), r.getNodeCode(), r.getNodeOrder(), r.getCanonicalContentJson(), r.getBeforeContentJson(),
                r.getAfterContentJson(), r.getFieldDiffJson(), frozenName(readFact(r.getCanonicalContentJson())), "Asia/Shanghai");
    }

}
