package cn.iocoder.yudao.module.showroom.workflow.model;

import java.util.List;
import java.util.Map;

public record ShowroomApprovalTargetPreview(String targetType, Long targetId, Long liveRevisionId,
                                            Long targetRevisionId, Map<String, String> liveFields,
                                            Map<String, String> targetFields,
                                            List<ShowroomApprovalPreviewRow> rows) {

    public ShowroomApprovalTargetPreview {
        liveFields = Map.copyOf(liveFields);
        targetFields = Map.copyOf(targetFields);
        rows = List.copyOf(rows);
    }

    public ShowroomApprovalTargetPreview(String targetType, Long targetId, Long liveRevisionId,
                                         Long targetRevisionId, Map<String, String> liveFields,
                                         Map<String, String> targetFields) {
        this(targetType, targetId, liveRevisionId, targetRevisionId, liveFields, targetFields, List.of());
    }

}
