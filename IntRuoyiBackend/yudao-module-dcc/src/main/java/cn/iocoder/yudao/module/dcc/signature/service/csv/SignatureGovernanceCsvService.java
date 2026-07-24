package cn.iocoder.yudao.module.dcc.signature.service.csv;

public interface SignatureGovernanceCsvService {

    SignatureGovernanceCsvPackageResult evaluatePackage(SignatureGovernanceCsvPackageCommand command);

    SignatureGovernanceCsvReleaseGateResult evaluateReleaseGate(SignatureGovernanceCsvReleaseGateCommand command);
}
