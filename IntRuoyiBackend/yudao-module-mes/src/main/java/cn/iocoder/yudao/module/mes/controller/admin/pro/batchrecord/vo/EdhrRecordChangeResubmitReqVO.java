package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class EdhrRecordChangeResubmitReqVO extends EdhrRecordChangeRequestReqVO {
    @NotNull private Long changeEventId;
}
