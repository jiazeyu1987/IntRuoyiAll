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
    @Schema(description = "服务端解析的一线生产正式提交上下文")
    private ProductionSubmitContext productionSubmitContext;
    @Schema(description = "服务端全部可选实际填写员工切换快照")
    private List<MesFrontlineSwitchEmployeeRespVO> employeeSwitchSnapshots;
    @Schema(description = "服务端签发的一线运行快照编号")
    private String frontlineSessionSnapshotId;
    @Schema(description = "服务端签发的一线运行快照校验值")
    private String frontlineSessionSnapshotHash;

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
        private String standardText;
        private List<String> optionValues;
        private String defaultText;
        private Integer decimalScale;
    }

    @Data
    public static class DefectReason {
        private Long reasonId;
        private String reasonType;
        private String reasonCode;
        private String reasonName;
    }

    @Data
    public static class ProductionSubmitContext {
        private Long workOrderId;
        private String workOrderCode;
        private String workOrderName;
        private Long taskId;
        private Long routeId;
        private Long routeProcessId;
        private Long processId;
        private Long workstationId;
        private Long itemId;
        private Long approveUserId;
        private Long recordbookId;
        private BigDecimal scheduledQuantity;
        private java.time.LocalDateTime expireDate;
        private Long activeOrderProcessSnapshotId;
        private String parameterSnapshotSha256;
        private String parameterSnapshotState;
    }
}
