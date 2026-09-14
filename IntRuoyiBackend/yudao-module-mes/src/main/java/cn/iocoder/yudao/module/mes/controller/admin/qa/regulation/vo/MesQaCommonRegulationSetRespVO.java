package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES QA 通用检验规程套 Response VO")
@Data
@Builder
public class MesQaCommonRegulationSetRespVO {

    private Long id;
    private String setCode;
    private String setName;
    private String setStatus;
    private Long currentVersionId;
    private String remark;
    private List<Version> versions;

    @Data
    @Builder
    public static class Version {
        private Long id;
        private Long setId;
        private String versionNo;
        private String lifecycleStatus;
        private LocalDate effectiveDate;
        private LocalDateTime publishedAt;
        private LocalDateTime retiredAt;
        private String remark;
        private Boolean currentPublished;
        private List<Member> members;
    }

    @Data
    @Builder
    public static class Member {
        private Long id;
        private Long commonDccProjectCodeId;
        private Long commonRegulationId;
        private Long commonRegulationVersionId;
        private String commonRegulationCode;
        private String commonRegulationName;
        private String versionNo;
        private String lifecycleStatus;
        private LocalDate effectiveDate;
        private LocalDateTime publishedAt;
        private Integer sort;
        private String memberRole;
        private String remark;
        private List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess> processes;
    }
}
