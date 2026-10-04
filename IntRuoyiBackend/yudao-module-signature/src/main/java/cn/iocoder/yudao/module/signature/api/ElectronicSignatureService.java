package cn.iocoder.yudao.module.signature.api;

import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity;

public interface ElectronicSignatureService {

    ElectronicSignatureResult sign(ElectronicSignatureCommand command);

    /** Called only after the business service authorizes the selected signer for this submission.
     * Profile credentials are authenticated by their owning module in the same transaction. */
    ElectronicSignatureResult signAuthorized(ElectronicSignatureCommand command,
                                             AuthorizedSignatureIdentity identity,
                                             Runnable authenticateProfile);

}
