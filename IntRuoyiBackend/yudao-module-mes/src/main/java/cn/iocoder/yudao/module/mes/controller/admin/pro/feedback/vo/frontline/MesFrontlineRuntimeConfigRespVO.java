package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 一线员工填报运行态配置 Response VO")
@Data
public class MesFrontlineRuntimeConfigRespVO {

    @Schema(description = "工艺路线编号")
    private Long routeId;
    @Schema(description = "工艺路线工序编号")
    private Long routeProcessId;
    @Schema(description = "工序编号")
    private Long processId;
    @Schema(description = "组长维护的本工序员工")
    private List<Employee> employees;
    @Schema(description = "组长维护的本工序可用设备")
    private List<Device> devices;
    @Schema(description = "组长维护的本工序异常/不良原因")
    private List<DefectReason> defectReasons;

    @Data
    public static class Employee {
        private Long employeeProfileId;
        private Long systemUserId;
        private String employeeCode;
        private String employeeName;
        private String displayName;
        private String employeeType;
    }

    @Data
    public static class Device {
        private Long deviceId;
        private String deviceCode;
        private String deviceName;
        private String deviceStatus;
        private List<DeviceParameter> parameters;
    }

    @Data
    public static class DeviceParameter {
        private String parameterCode;
        private String parameterName;
        private String unit;
        private BigDecimal lowerLimit;
        private BigDecimal upperLimit;
        private BigDecimal defaultValue;
        private String valueType;
    }

    @Data
    public static class DefectReason {
        private Long reasonId;
        private String reasonType;
        private String reasonCode;
        private String reasonName;
    }
}
