package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 一线 PQC 切换实际填写员工 Response VO")
@Data
public class MesFrontlinePqcSwitchEmployeeRespVO {

    private Long loginUserId;
    private Long actualEmployeeId;
    private Long routeId;
    private Long dccProjectCodeId;
    private Long regulationVersionId;
    private Long qaProcessId;
    private Boolean extraVerificationRequired;
    private PqcTemplate template;

    @Data
    public static class PqcTemplate {
        private String templateNo;
        private String templateType;
        private Long qaProcessId;
        private Long actualEmployeeId;
    }
}
