package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/** One actual event; assignment and subsequent signoff never collapse into one node result. */
public record DccApplicationNodeRecord(String recordId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long controlledFileId,
        String bpmRound,String taskId,String stageCode,String nodeName,String actionType,String result,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long actorId,
        String actorUsernameSnapshot,String actorNicknameSnapshot,String actorDeptNameSnapshot,String comment,
        @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime occurredAt,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long signatureId,String signatureEvidenceStatus,
        AssignmentTarget assignmentTarget,List<ConfirmedField> confirmedFields) {
    public DccApplicationNodeRecord { confirmedFields=List.copyOf(confirmedFields); }
    public record AssignmentTarget(@JsonFormat(shape=JsonFormat.Shape.STRING) Long userId,
                                   String nickname,String obligationDepartmentName) {}
    public record ConfirmedField(String field,String label,String value) {}
}
