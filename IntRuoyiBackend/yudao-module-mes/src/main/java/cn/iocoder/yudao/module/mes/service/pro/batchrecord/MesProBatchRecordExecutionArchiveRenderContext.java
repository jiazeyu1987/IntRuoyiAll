package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class MesProBatchRecordExecutionArchiveRenderContext {

    private MesProBatchRecordExecutionDO execution;
    private JSONObject executionSnapshot;
    private Object cellValues;
    private List<MesProBatchRecordExecutionSignatureDO> signatures;
    private String executionSnapshotHash;
    private String cellValuesHash;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String signatureHash;
    private List<MesProBatchRecordExecutionAttachmentDO> attachments;
    private String attachmentManifestHeadHash;
    private Long approvalSnapshotId;
    private String approvalSnapshotHash;
    private Map<String, Object> formSlotManifest;
    private Long generatedBy;
    private LocalDateTime generatedAt;
}
