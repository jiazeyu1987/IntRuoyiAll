package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

@Service
public class ElectronicSignatureQueryServiceImpl implements ElectronicSignatureQueryService {

    private static final String VERIFICATION_STATUS_VALID = "VALID";
    private static final String VERIFICATION_STATUS_MISMATCH = "MISMATCH";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String AUTHENTICATION_METHOD = "SESSION_PLUS_PASSWORD";

    @Resource
    private ElectronicSignatureRecordMapper signatureRecordMapper;
    @Resource
    private List<ElectronicSignatureSubjectAdapter> subjectAdapters;

    @Override
    public ElectronicSignatureEvidenceDTO getById(Long signatureId) {
        if (signatureId == null || signatureId <= 0) {
            throw exception(ESIGN_COMMAND_INVALID, "签名编号必须为正整数");
        }
        ElectronicSignatureRecordDO record = signatureRecordMapper.selectById(signatureId);
        if (record == null || !Objects.equals(record.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            return null;
        }
        return toEvidence(record);
    }

    @Override
    public List<ElectronicSignatureEvidenceDTO> listBySubject(String moduleCode, String subjectType, String subjectId) {
        if (StrUtil.hasBlank(moduleCode, subjectType, subjectId)) {
            throw exception(ESIGN_COMMAND_INVALID, "模块、对象类型和对象编号不能为空");
        }
        return signatureRecordMapper.selectList(new LambdaQueryWrapper<ElectronicSignatureRecordDO>()
                        .eq(ElectronicSignatureRecordDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                        .eq(ElectronicSignatureRecordDO::getModuleCode, moduleCode)
                        .eq(ElectronicSignatureRecordDO::getSubjectType, subjectType)
                        .eq(ElectronicSignatureRecordDO::getSubjectId, subjectId)
                        .orderByAsc(ElectronicSignatureRecordDO::getSignedAt)
                        .orderByAsc(ElectronicSignatureRecordDO::getId))
                .stream()
                .map(this::toEvidence)
                .toList();
    }

    @Override
    public ElectronicSignatureVerificationDTO verifyEvidence(Long signatureId) {
        if (signatureId == null) {
            throw exception(ESIGN_COMMAND_INVALID, "签名编号不能为空");
        }
        ElectronicSignatureRecordDO record = signatureRecordMapper.selectById(signatureId);
        if (record == null || !Objects.equals(record.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw exception(ESIGN_COMMAND_INVALID, "签名记录不存在");
        }
        String calculatedContentHash = resolveContentHash(record);
        String calculatedEvidenceHash = hash(evidencePayload(record, calculatedContentHash));
        String status = Objects.equals(record.getContentHash(), calculatedContentHash)
                && Objects.equals(record.getEvidenceHash(), calculatedEvidenceHash)
                ? VERIFICATION_STATUS_VALID : VERIFICATION_STATUS_MISMATCH;
        return new ElectronicSignatureVerificationDTO(record.getId(), status, record.getContentHash(),
                calculatedContentHash, record.getEvidenceHash(), calculatedEvidenceHash, record.getAlgorithm(),
                record.getKeyVersion());
    }

    private String resolveContentHash(ElectronicSignatureRecordDO record) {
        String directHash = hash(ElectronicSignatureJsonCanonicalizer.canonicalize(record.getCanonicalContentJson()));
        if (Objects.equals(record.getContentHash(), directHash)) {
            return directHash;
        }
        Optional<SignatureSubjectSnapshot> snapshot = replaySubjectSnapshot(record);
        if (snapshot.isEmpty() || !jsonSemanticallyEquals(record.getCanonicalContentJson(),
                snapshot.get().canonicalContentJson())) {
            return directHash;
        }
        String replayedHash = hash(ElectronicSignatureJsonCanonicalizer.canonicalize(
                snapshot.get().canonicalContentJson()));
        return Objects.equals(record.getContentHash(), replayedHash) ? replayedHash : directHash;
    }

    private Optional<SignatureSubjectSnapshot> replaySubjectSnapshot(ElectronicSignatureRecordDO record) {
        if (subjectAdapters == null || subjectAdapters.isEmpty()) {
            return Optional.empty();
        }
        return subjectAdapters.stream()
                .filter(adapter -> Objects.equals(adapter.moduleCode(), record.getModuleCode()))
                .findFirst()
                .map(adapter -> adapter.loadAndAuthorize(new SignatureSubjectCommand(
                        record.getActorId(), record.getModuleCode(), record.getActionCode(),
                        record.getSubjectType(), record.getSubjectId(), record.getSubjectVersion(),
                        record.getReason())));
    }

    private boolean jsonSemanticallyEquals(String left, String right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        if (StrUtil.hasBlank(left, right)) {
            return false;
        }
        try {
            return Objects.equals(JSON.parse(left), JSON.parse(right));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private ElectronicSignatureEvidenceDTO toEvidence(ElectronicSignatureRecordDO record) {
        return new ElectronicSignatureEvidenceDTO(record.getId(), record.getModuleCode(), record.getActionCode(),
                record.getSubjectType(), record.getSubjectId(), record.getSubjectVersion(), record.getActorId(),
                record.getMeaningCode(), record.getMeaningLabel(), record.getReason(), record.getSignedAt(),
                record.getTimeEvidenceId(), record.getAuthenticationMethod(), record.getContentHash(),
                record.getEvidenceHash(), record.getAlgorithm(), record.getKeyVersion(), record.getPolicyVersion(),
                record.getVerificationStatus(), record.getProcessInstanceId(), record.getTaskId(),
                record.getNodeCode(), record.getNodeOrder(), record.getCanonicalContentJson(),
                record.getBeforeContentJson(), record.getAfterContentJson(), record.getFieldDiffJson());
    }

    private String evidencePayload(ElectronicSignatureRecordDO record, String contentHash) {
        return String.join("|", String.valueOf(record.getTenantId()), String.valueOf(record.getActorId()),
                record.getModuleCode(), record.getActionCode(), record.getSubjectType(), record.getSubjectId(),
                record.getSubjectVersion(), record.getMeaningCode(), record.getMeaningLabel(), record.getReason(),
                originalSignedAt(record), record.getTimeEvidenceId(), AUTHENTICATION_METHOD, contentHash,
                StrUtil.nullToEmpty(record.getBeforeContentHash()), StrUtil.nullToEmpty(record.getAfterContentHash()),
                StrUtil.nullToEmpty(ElectronicSignatureJsonCanonicalizer.canonicalize(record.getBeforeContentJson())),
                StrUtil.nullToEmpty(ElectronicSignatureJsonCanonicalizer.canonicalize(record.getAfterContentJson())),
                StrUtil.nullToEmpty(ElectronicSignatureJsonCanonicalizer.canonicalize(record.getFieldDiffJson())),
                HASH_ALGORITHM, record.getKeyVersion(),
                record.getPolicyVersion(), record.getVerificationStatus());
    }

    private String originalSignedAt(ElectronicSignatureRecordDO record) {
        String timeEvidenceId = record.getTimeEvidenceId();
        if (StrUtil.startWith(timeEvidenceId, "SERVER_CLOCK:")) {
            return StrUtil.removePrefix(timeEvidenceId, "SERVER_CLOCK:");
        }
        return record.getSignedAt().toString();
    }

    private String hash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] bytes = digest.digest(StrUtil.nullToEmpty(payload).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

}
