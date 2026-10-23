package cn.iocoder.yudao.module.signature.api;

import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;

import java.util.Set;

public interface ElectronicSignatureSubjectAdapter {
    enum ServerTimeResolution { ORIGINAL, MICROS }

    String moduleCode();

    Set<SignatureActionDefinition> supportedActions();

    /** Explicit action policy, consulted only after resolving a registered adapter/action/object tuple. */
    default boolean isReasonRequired(String actionCode, String subjectType) {
        return true;
    }

    SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command);

    /** Registered server adapter policy after an authorized snapshot, never a client-selected timestamp. */
    default ServerTimeResolution serverTimeResolution(SignatureSubjectCommand command, SignatureSubjectSnapshot snapshot) {
        return ServerTimeResolution.ORIGINAL;
    }
    default SignatureSubjectSnapshot bindServerSignedAt(SignatureSubjectCommand command, SignatureSubjectSnapshot authorizedSnapshot, java.time.LocalDateTime signedAt) {
        return authorizedSnapshot;
    }

}
