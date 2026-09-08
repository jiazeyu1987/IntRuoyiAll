package cn.iocoder.yudao.module.signature.gxp.api;

public interface GxpAuditTrailService {

    Long append(GxpAuditTrailAppendCommand command);

}
