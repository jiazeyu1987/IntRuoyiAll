package cn.iocoder.yudao.module.signature.api;

import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;

import java.util.Set;

public interface ElectronicSignatureSubjectAdapter {

    String moduleCode();

    Set<SignatureActionDefinition> supportedActions();

    SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command);

}
