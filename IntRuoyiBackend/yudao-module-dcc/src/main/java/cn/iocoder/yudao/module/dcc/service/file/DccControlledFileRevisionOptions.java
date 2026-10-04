package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only revision preparation; storage references and body hashes are never capabilities here. */
public record DccControlledFileRevisionOptions(
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long controlledBaselineId,
        String baselineVersionNo,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long masterId,
        String sourceOriginalFileName,
        Target partialTarget, Target replacementTarget,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long checkedOutBy,
        String checkedOutByName, LocalDateTime checkedOutTime, String lockedReason,
        List<Iteration> iterations) {
    public record Target(String versionNo, String unavailableReason) {}
    public record Iteration(@JsonFormat(shape = JsonFormat.Shape.STRING) Long id, String versionNo,
                            boolean canPartial, boolean canReplacement, boolean canPreview) {}
}
