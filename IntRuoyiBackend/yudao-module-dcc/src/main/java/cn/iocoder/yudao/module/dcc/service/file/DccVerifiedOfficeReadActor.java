package cn.iocoder.yudao.module.dcc.service.file;

import java.util.Objects;

/** Internal read subject, created only after the original Office token and saved evidence checks. */
record DccVerifiedOfficeReadActor(Long tenantId,Long userId,Long controlledFileId,String versionNo,
                                  String processInstanceId,String purpose,Long accessEventId,
                                  String accessEventCode,String watermarkTraceCode,
                                  DccControlledFileBodyReadContext bodyReadContext) {
    DccVerifiedOfficeReadActor(Long tenantId, Long userId, Long controlledFileId, String versionNo,
            String processInstanceId, String purpose, Long accessEventId, String accessEventCode, String watermarkTraceCode) {
        this(tenantId, userId, controlledFileId, versionNo, processInstanceId, purpose, accessEventId, accessEventCode, watermarkTraceCode, null);
    }
    DccVerifiedOfficeReadActor {
        if(tenantId==null||tenantId<=0||userId==null||userId<=0||controlledFileId==null||controlledFileId<=0
                ||accessEventId==null||accessEventId<=0||versionNo==null||versionNo.isBlank()
                ||accessEventCode==null||accessEventCode.isBlank() ||watermarkTraceCode==null||watermarkTraceCode.isBlank())
            throw new IllegalArgumentException("VERIFIED_OFFICE_READ_SUBJECT_INVALID");
        if (bodyReadContext == null) {
            if (processInstanceId == null || processInstanceId.isBlank() || !Objects.equals("CONTROLLED_PREVIEW", purpose))
                throw new IllegalArgumentException("VERIFIED_OFFICE_READ_SUBJECT_INVALID");
        } else {
            bodyReadContext.requireFile(controlledFileId, versionNo);
            if (!Objects.equals(bodyReadContext.purpose(), purpose) || !Objects.equals(bodyReadContext.bpmRound(), processInstanceId))
                throw new IllegalArgumentException("VERIFIED_OFFICE_BODY_CONTEXT_INVALID");
        }
    }
    boolean matches(Long actor,cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file){
        return file!=null&&Objects.equals(userId,actor)&&Objects.equals(tenantId,file.getTenantId())
                &&Objects.equals(controlledFileId,file.getId())&&Objects.equals(versionNo,file.getVersionNo())
                &&(bodyReadContext != null || Objects.equals(processInstanceId,file.getProcessInstanceId()));
    }
}
