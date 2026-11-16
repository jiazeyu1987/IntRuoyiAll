package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class EdhrBatchIntegrityChunkReqVO {
    @PositiveOrZero private Long afterId;
    @PositiveOrZero private Long upperId;
}
