package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrReleasePrecheckReqVO {

    private Long releaseTransactionId;

    private Long batchExecutionId;

    @AssertTrue(message = "放行预检必须指定放行事务或eDHR批次")
    public boolean isTargetPresent() {
        return releaseTransactionId != null || batchExecutionId != null;
    }
}
