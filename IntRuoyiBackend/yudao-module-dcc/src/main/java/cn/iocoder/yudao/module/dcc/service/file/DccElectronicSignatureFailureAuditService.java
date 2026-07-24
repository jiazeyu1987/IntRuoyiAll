package cn.iocoder.yudao.module.dcc.service.file;

public interface DccElectronicSignatureFailureAuditService {

    boolean recordPasswordFailure(DccElectronicSignatureFailureAuditCommand command);
}
