package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrProcessFormPermissionRuleRespVO {

    private Long routeProcessId;

    private String batchRecordReportId;

    private String fillRuleStatus;

    private String signatureRuleStatus;

    private Long permissionScopeId;

    private CandidateRule fillRule;

    private List<FillAssignment> fillAssignments;

    private List<SignatureRule> signatureRules;

    private LocalDateTime updateTime;

    private Integer affectedRouteBindingCount;

    @Data
    @Accessors(chain = true)
    public static class CandidateRule {

        private String candidateSourceType;

        private List<Long> candidateSourceIds;

        private String completionPolicy;

        private Integer dueMinutes;

        private Boolean enabled;

        private String remark;

        private List<CandidateUser> candidateUsers;
    }

    @Data
    @Accessors(chain = true)
    public static class FillAssignment {

        private String scopeKey;

        private String candidateSourceType;

        private List<Long> candidateSourceIds;

        private String completionPolicy;

        private Integer dueMinutes;

        private Boolean enabled;

        private String remark;

        private List<CandidateUser> candidateUsers;
    }

    @Data
    @Accessors(chain = true)
    public static class SignatureRule {

        private String signatureCellKey;

        private String signatureRole;

        private CandidateRule rule;
    }

    @Data
    @Accessors(chain = true)
    public static class CandidateUser {

        private Long userId;

        private String displayName;
    }
}
