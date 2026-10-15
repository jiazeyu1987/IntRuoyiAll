package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO;

/** Authoritative zero-write comparison for the existing submit Controller to project as a business refusal. */
public final class DccApprovalMatrixChangeException extends IllegalStateException {
    private final String reasonCode;
    private final DccApprovalMatrixBaselineVO matrixBefore,matrixAfter;
    private final String approvalMatrixBaselineToken,approvalMatrixConfirmationToken;
    public DccApprovalMatrixChangeException(String code,DccApprovalMatrixBaselineVO before,DccApprovalMatrixBaselineVO after,String baseline,String confirmation){
        super(code);reasonCode=code;matrixBefore=before;matrixAfter=after;approvalMatrixBaselineToken=baseline;approvalMatrixConfirmationToken=confirmation;
    }
    public String getReasonCode(){return reasonCode;}
    public DccApprovalMatrixBaselineVO getMatrixBefore(){return matrixBefore;}
    public DccApprovalMatrixBaselineVO getMatrixAfter(){return matrixAfter;}
    public String getApprovalMatrixBaselineToken(){return approvalMatrixBaselineToken;}
    public String getApprovalMatrixConfirmationToken(){return approvalMatrixConfirmationToken;}
}
