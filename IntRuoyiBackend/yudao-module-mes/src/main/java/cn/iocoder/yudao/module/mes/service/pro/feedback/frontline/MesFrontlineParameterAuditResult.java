package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesFrontlineParameterAuditResult {

    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_UNRESOLVED = "UNRESOLVED";

    private String parameterAuditStatus;
    private Integer totalCount;
    private Integer resolvedCount;
    private Integer unresolvedCount;
    private List<MesFrontlineParameterAuditItem> auditItems;

    public static MesFrontlineParameterAuditResult empty() {
        return new MesFrontlineParameterAuditResult()
                .setParameterAuditStatus(STATUS_RESOLVED)
                .setTotalCount(0)
                .setResolvedCount(0)
                .setUnresolvedCount(0)
                .setAuditItems(List.of());
    }
}
