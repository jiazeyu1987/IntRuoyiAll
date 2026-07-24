package cn.iocoder.yudao.module.bpm.businessapproval.model;

import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;

public class BusinessApprovalException extends RuntimeException {

    private final BusinessApprovalErrorCode errorCode;

    public BusinessApprovalException(BusinessApprovalErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessApprovalErrorCode getErrorCode() {
        return errorCode;
    }

}
