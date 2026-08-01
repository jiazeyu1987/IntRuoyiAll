package cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 工序只读目录 Response VO")
@Data
public class MesProMesProcessRespVO {

    @Schema(description = "编号", example = "9003131001")
    private Long id;

    @Schema(description = "行键", example = "二代压力泵:2")
    private String rowKey;

    @Schema(description = "来源文件", example = "压力泵工序.xlsx")
    private String sourceFileName;

    @Schema(description = "来源工作表", example = "二代压力泵")
    private String sourceSheetName;

    @Schema(description = "Excel 源行号", example = "2")
    private Integer sourceRowNo;

    @Schema(description = "列表排序", example = "1")
    private Integer sortNo;

    @Schema(description = "目录编码", example = "PUMP2-MES-0001")
    private String catalogCode;

    @Schema(description = "产品名称", example = "二代压力泵")
    private String productName;

    @Schema(description = "源表设备编码", example = "B09032/G01160")
    private String sourceMachineryCodes;

    @Schema(description = "工序名称", example = "粗洗")
    private String mesProcessName;

    @Schema(description = "源表设备名称", example = "超声波清洗机")
    private String sourceMachineryName;

    @Schema(description = "源表设备数量", example = "1")
    private String sourceMachineryQuantity;

    @Schema(description = "10.5 小时日产能", example = "3500")
    private String dailyCapacity10_5;

    @Schema(description = "日常工序人力", example = "3")
    private String dailyWorkerQuantity;

    @Schema(description = "工序编码", example = "Z1500")
    private String mesProcessCode;

    @Schema(description = "工序单价", example = "0.2224")
    private String processPrice;

    @Schema(description = "工序是否报工", example = "是")
    private String feedbackFlag;

    @Schema(description = "工序是否形成批记录", example = "是（两道合并）")
    private String batchRecordFlag;

    @Schema(description = "批记录工序名称", example = "检测")
    private String batchRecordProcessName;

    @Schema(description = "设备明细列表")
    private List<MesProMesProcessMachineryRespVO> machineryList;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
