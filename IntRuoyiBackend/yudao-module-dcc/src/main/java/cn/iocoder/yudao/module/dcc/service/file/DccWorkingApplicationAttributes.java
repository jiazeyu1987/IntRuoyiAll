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
        List<String> selectedSignoffDepartmentIds, String changeDescription) {}
