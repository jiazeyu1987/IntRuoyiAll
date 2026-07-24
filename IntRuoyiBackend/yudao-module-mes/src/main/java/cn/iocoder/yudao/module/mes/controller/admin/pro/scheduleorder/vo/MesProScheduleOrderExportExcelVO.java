package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MesProScheduleOrderExportExcelVO {

    @ExcelProperty("生产工单号")
    private String erpWorkOrderCode;

    @ExcelProperty("产品编号")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("规格型号")
    private String productSpecification;

    @ExcelProperty("数量/进度")
    private String quantityProgress;

    @ExcelProperty("承诺交期")
    private LocalDate promiseDate;

    @ExcelProperty("最晚开工")
    private LocalDateTime latestStartTime;

    @ExcelProperty("计划开工")
    private LocalDateTime plannedStartTime;

    @ExcelProperty("计划完成")
    private LocalDateTime plannedEndTime;

    @ExcelProperty("优先级")
    private Integer priorityNo;

    @ExcelProperty("生产用料清单")
    private String productionMaterialListSummary;

    @ExcelProperty("当前工序")
    private String currentProcessName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    public static MesProScheduleOrderExportExcelVO from(MesProScheduleOrderRespVO row) {
        return MesProScheduleOrderExportExcelVO.builder()
                .erpWorkOrderCode(row.getErpWorkOrderCode())
                .productCode(row.getProductCode())
                .productName(row.getProductName())
                .productSpecification(row.getProductSpecification())
                .quantityProgress(buildQuantityProgress(row))
                .promiseDate(row.getPromiseDate())
                .latestStartTime(row.getLatestStartTime())
                .plannedStartTime(row.getPlannedStartTime())
                .plannedEndTime(row.getPlannedEndTime())
                .priorityNo(row.getPriorityNo())
                .productionMaterialListSummary(resolveMaterialListSummary(row))
                .currentProcessName(buildCurrentProcess(row))
                .createTime(row.getCreateTime())
                .build();
    }

    private static String buildQuantityProgress(MesProScheduleOrderRespVO row) {
        BigDecimal totalQuantity = row.getTotalQuantity() != null ? row.getTotalQuantity() : row.getQuantity();
        BigDecimal progressPercent = row.getProgressPercent() != null ? row.getProgressPercent() : BigDecimal.ZERO;
        return String.format("总量 %s / %s%%", formatDecimal(totalQuantity), formatDecimal(progressPercent));
    }

    private static String buildCurrentProcess(MesProScheduleOrderRespVO row) {
        if (row.getCurrentProcessId() == null) {
            return "";
        }
        String name = row.getCurrentProcessName() != null ? row.getCurrentProcessName() : row.getCurrentProcessCode();
        String displayName = name != null ? name : String.valueOf(row.getCurrentProcessId());
        BigDecimal progressPercent = row.getCurrentProcessProgressPercent() != null
                ? row.getCurrentProcessProgressPercent() : BigDecimal.ZERO;
        return String.format("%s / %s%%", displayName, formatDecimal(progressPercent));
    }

    private static String resolveMaterialListSummary(MesProScheduleOrderRespVO row) {
        if (row.getProductionMaterialListCount() == null || row.getProductionMaterialListCount() <= 0) {
            return "缺失";
        }
        return row.getProductionMaterialListSummary() != null
                ? row.getProductionMaterialListSummary()
                : String.format("共 %s 张", row.getProductionMaterialListCount());
    }

    private static String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

}
