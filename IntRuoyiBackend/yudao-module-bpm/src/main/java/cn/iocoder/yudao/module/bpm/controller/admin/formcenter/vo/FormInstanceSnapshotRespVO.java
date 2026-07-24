package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 表单中心实例快照 Response VO")
@Data
public class FormInstanceSnapshotRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "表单实例编号")
    private Long instanceId;

    @Schema(description = "快照类型")
    private String snapshotType;

    @Schema(description = "快照版本")
    private Integer snapshotVersion;

    @Schema(description = "表单数据")
    private Map<String, Object> formData;

    @Schema(description = "业务上下文")
    private BusinessActionContextReqVO context;

    @Schema(description = "附件编号集合")
    private List<String> attachmentIds;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

}
