package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportUploadTaskRespVO {

    private String nodeType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchTaskId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long workTaskId;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> candidateUserIds;

    private String status;
}
