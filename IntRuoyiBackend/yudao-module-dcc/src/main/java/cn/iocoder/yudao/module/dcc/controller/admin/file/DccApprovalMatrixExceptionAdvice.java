package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.service.file.DccApprovalMatrixChangeException;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO;

/** Only the existing file application boundary exposes a formal zero-write matrix comparison. */
public class DccApprovalMatrixExceptionAdvice {
    public record Comparison(String reasonCode,DccApprovalMatrixBaselineVO matrixBefore,
            DccApprovalMatrixBaselineVO matrixAfter,String approvalMatrixBaselineToken,
            String approvalMatrixConfirmationToken) {}

    public CommonResult<Comparison> changed(DccApprovalMatrixChangeException change) {
        int code=switch(change.getReasonCode()) {
            case "APPROVAL_MATRIX_EXPECTATION_STALE" -> 1_080_000_363;
            case "APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED" -> 1_080_000_364;
            default -> throw new IllegalStateException("未登记的矩阵比较错误分类",change);
        };
        var result=CommonResult.<Comparison>error(code,change.getReasonCode()+"：审批配置已变化，请刷新正式预检并确认本次配置");
        result.setData(new Comparison(change.getReasonCode(),change.getMatrixBefore(),change.getMatrixAfter(),
                change.getApprovalMatrixBaselineToken(),change.getApprovalMatrixConfirmationToken()));
        return result;
    }
}
