package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.Objects;

/** Called only inside the authorized native approve transaction; does not grant owner access rights. */
@Service
public class DccApprovalFileOwnerSelectionService {
    @Resource private AdminUserApi users;
    @Resource private DccControlledFileMapper files;
    @Resource private DccControlledFileSignatureMapper signatures;
    public record Selection(Long userId, String username, String nickname) {}

    public Selection prepare(DccControlledFileDO file, DccControlledFileStageCodeEnum stage, Long userId) {
        boolean applicable = file != null && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey())
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()))
                && stage == DccControlledFileStageCodeEnum.MATRIX_APPROVAL;
        if (!applicable) {
            if (userId != null) throw new IllegalArgumentException("只有上传、升版批准节点可选择文件负责人");
            return null;
        }
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(file.getTenantId(),tenant) || userId==null || userId<=0)
            throw new IllegalArgumentException("批准通过前请选择正式文件负责人");
        var user=users.getUser(userId);
        if (user==null || !Objects.equals(user.getId(),userId) || !Objects.equals(user.getTenantId(),tenant)
                || !Integer.valueOf(0).equals(user.getStatus()) || user.getUsername()==null || user.getUsername().isBlank()
                || user.getNickname()==null || user.getNickname().isBlank())
            throw new IllegalArgumentException("文件负责人必须为本租户正式启用账号");
        return new Selection(userId,user.getUsername(),user.getNickname());
    }

    public String signedReason(String reason, Selection selected) {
        if (selected==null) return reason;
        if (reason==null || reason.isBlank()) throw new IllegalArgumentException("批准意见不能为空");
        String signed=reason.trim()+"；批准文件负责人事实："+cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.List.of(selected.userId(),selected.username(),selected.nickname()));
        if(signed.length()>500)throw new IllegalArgumentException("批准意见与负责人签名事实合计超过500字，请缩短审批意见后重新确认");
        return signed;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void bind(DccControlledFileDO file, String taskId, Long actorId, Long signatureId, Selection selected, String reason) {
        if (selected==null) return;
        if (file==null || file.getId()==null || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || file.getVersionNo()==null || file.getProcessInstanceId()==null || actorId==null || actorId<=0)
            throw new IllegalStateException("文件负责人批准身份、版本和租户不完整");
        if (selected.userId()==null || selected.userId()<=0 || selected.username()==null || selected.username().isBlank()
                || selected.nickname()==null || selected.nickname().isBlank() || reason==null)
            throw new IllegalStateException("文件负责人签名事实不完整");
        String suffix="；批准文件负责人事实："+cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.List.of(selected.userId(),selected.username(),selected.nickname()));
        if (!reason.endsWith(suffix) || reason.length()<=suffix.length()
                || !Objects.equals(reason, signedReason(reason.substring(0,reason.length()-suffix.length()),selected)))
            throw new IllegalStateException("文件负责人选择与正式签名中的负责人事实不一致");
        var signature=signatures.selectActionSignature(file.getId(),taskId,actorId,"APPROVE");
        if (signature==null || signatureId==null || !Objects.equals(signature.getId(),signatureId)
                || !Objects.equals(signature.getControlledFileId(),file.getId()) || !Objects.equals(signature.getTaskId(),taskId)
                || !Objects.equals(signature.getActorId(),actorId) || !Objects.equals(signature.getActionType(),"APPROVE")
                || !Objects.equals(signature.getProcessInstanceId(),file.getProcessInstanceId())
                || !Objects.equals(signature.getVersionNo(),file.getVersionNo()) || !Objects.equals(signature.getComment(),reason)
                || !"MATRIX_APPROVAL_APPROVE".equals(signature.getMeaningCode()) || signature.getEvidenceHash()==null || signature.getEvidenceHash().isBlank()
                || !"VALID".equals(signature.getEvidenceStatus()) || signature.getSignedAt()==null)
            throw new IllegalStateException("文件负责人选择缺少同文件、版本、批准任务和流程的正式签名证据");
        // A fresh authorized task may replace the current projection. Every preceding signature remains immutable.
        // The same task can only replay its exact already-signed projection.
        if (Objects.equals(file.getFileOwnerApprovalTaskId(),taskId)) {
            if (!Objects.equals(file.getFileOwnerSignatureId(),signatureId)
                    || !Objects.equals(file.getFileOwnerUserId(),selected.userId())
                    || !Objects.equals(file.getFileOwnerUsernameSnapshot(),selected.username())
                    || !Objects.equals(file.getFileOwnerNicknameSnapshot(),selected.nickname())
                    || !Objects.equals(file.getFileOwnerProcessInstanceId(),file.getProcessInstanceId())
                    || !Objects.equals(file.getFileOwnerSelectedTime(),signature.getSignedAt()))
                throw new IllegalStateException("同一批准任务的负责人签名事实不可变更");
            return;
        }
        var update=DccControlledFileDO.builder().id(file.getId()).fileOwnerUserId(selected.userId())
                .fileOwnerUsernameSnapshot(selected.username()).fileOwnerNicknameSnapshot(selected.nickname())
                .fileOwnerSignatureId(signatureId).fileOwnerApprovalTaskId(taskId).fileOwnerProcessInstanceId(file.getProcessInstanceId())
                .fileOwnerSelectedTime(signature.getSignedAt()).build();
        if (files.updateById(update)!=1) throw new IllegalStateException("批准文件负责人快照保存失败");
        file.setFileOwnerUserId(selected.userId());file.setFileOwnerUsernameSnapshot(selected.username());file.setFileOwnerNicknameSnapshot(selected.nickname());
        file.setFileOwnerSignatureId(signatureId);file.setFileOwnerApprovalTaskId(taskId);file.setFileOwnerProcessInstanceId(file.getProcessInstanceId());file.setFileOwnerSelectedTime(signature.getSignedAt());
    }
}
