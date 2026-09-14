package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

public interface MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort {

    TargetResolution resolveTarget(MesProRouteFlowProcessBatchRecordDO binding,
                                   List<MesProBatchRecordCellLinkRuleDO> rules,
                                   String expectedDccProjectCode);

    WriteResult write(WriteCommand command);

    @Data
    @Accessors(chain = true)
    class TargetResolution {
        private Long templateVersionId;
        private String templateSnapshotHash;
        private String templateDccProjectCode;
        private Map<Long, String> targetFieldCodes;
        private String blockerType;
        private String blockerMessage;

        public boolean isValid() {
            return blockerType == null && templateVersionId != null && templateSnapshotHash != null
                    && templateDccProjectCode != null
                    && targetFieldCodes != null && !targetFieldCodes.isEmpty();
        }
    }

    @Data
    @Accessors(chain = true)
    class WriteCommand {
        private Long tenantId;
        private Long batchExecutionId;
        private MesProEdhrBatchExecutionTaskDO batchTask;
        private MesProRouteFlowProcessBatchRecordDO binding;
        private TargetResolution target;
        private List<FieldWrite> fields;
        private String sourceSnapshotHash;
        private String evidenceHash;
        private List<MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence> signatureEvidence;
    }

    @Data
    @Accessors(chain = true)
    class FieldWrite {
        private Long ruleId;
        private Long ruleVersion;
        private String sourceCellKey;
        private String sourceFieldCode;
        private String targetFieldCode;
        private Object value;
        private String displayValue;
        private String sourceValueHash;
    }

    @Data
    @Accessors(chain = true)
    class WriteResult {
        private Long formCenterInstanceId;
        private Long fieldAuditSnapshotId;
        private String fieldAuditHeadHash;
        private String effectiveStatus;
    }
}
