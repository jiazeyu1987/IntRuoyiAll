package cn.iocoder.yudao.module.dcc.service.route;

/** A precise unsupported business configuration, retained as original evidence rather than silently rewritten. */
public final class DccApprovalMatrixCapabilityException extends IllegalArgumentException {
    private final String reasonCode;
    public DccApprovalMatrixCapabilityException(String reason,String detail){super(reason+"："+detail);reasonCode=reason;}
    public String getReasonCode(){return reasonCode;}
}
