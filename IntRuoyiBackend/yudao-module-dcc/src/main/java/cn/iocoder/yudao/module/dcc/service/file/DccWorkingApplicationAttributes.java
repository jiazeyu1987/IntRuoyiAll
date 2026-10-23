package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

/** Existing unsent draft only. Null departments mean no department choice has been persisted. */
public record DccWorkingApplicationAttributes(
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long controlledFileId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId,
        String applicationType, DccProjectAttributes defaultSource, DccProjectAttributes actual,
        boolean canSubmit, String unavailableReason, LocalDate effectiveDate, Boolean needTraining,
        List<String> selectedSignoffDepartmentIds, String changeDescription,
        String effectiveMode, List<String> selectedDistributionDepartmentIds,
        cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO draftApprovalMatrix,
        boolean newActionBaselineRequired,boolean newActionBaselinePreparationAllowed,
        String executionContract,String preparedProcessDefinitionId,String preparedDefinitionDigest,
        String approvalSourceMode,DccApplicationCustomMatrix applicationCustomMatrix,String applicationContextKey,
        Integer attributeRound,String applicationCustomMatrixDigest) {
    public DccWorkingApplicationAttributes(Long file,Long project,String type,DccProjectAttributes defaults,DccProjectAttributes actual,
            boolean canSubmit,String reason,LocalDate date,Boolean training,List<String> signoff,String description,String mode,List<String> distribution,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO matrix,boolean required,boolean allowed,
            String execution,String definition,String digest) {
        this(file,project,type,defaults,actual,canSubmit,reason,date,training,signoff,description,mode,distribution,matrix,required,allowed,execution,definition,digest,null,null,null,null,null);
    }
    public DccWorkingApplicationAttributes(Long file,Long project,String type,DccProjectAttributes defaults,DccProjectAttributes actual,
            boolean canSubmit,String reason,LocalDate date,Boolean training,List<String> signoff,String description,String mode,List<String> distribution,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO matrix,boolean required,boolean allowed) {
        this(file,project,type,defaults,actual,canSubmit,reason,date,training,signoff,description,mode,distribution,matrix,required,allowed,null,null,null);
    }
    /** Existing explicit projections have no new-action preparation grant. Real GET computes it. */
    public DccWorkingApplicationAttributes(Long file,Long project,String type,DccProjectAttributes defaults,DccProjectAttributes actual,
            boolean canSubmit,String reason,LocalDate date,Boolean training,List<String> signoff,String description,String mode,List<String> distribution,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO matrix) {
        this(file,project,type,defaults,actual,canSubmit,reason,date,training,signoff,description,mode,distribution,matrix,false,false,null,null,null);
    }
    /** Explicit historical/partial projection: absent baseline remains absent, never current-config fallback. */
    public DccWorkingApplicationAttributes(Long file,Long project,String type,DccProjectAttributes defaults,DccProjectAttributes actual,
            boolean canSubmit,String reason,LocalDate date,Boolean training,List<String> signoff,String description,String mode,List<String> distribution) {
        this(file,project,type,defaults,actual,canSubmit,reason,date,training,signoff,description,mode,distribution,null,false,false,null,null,null);
    }
}
