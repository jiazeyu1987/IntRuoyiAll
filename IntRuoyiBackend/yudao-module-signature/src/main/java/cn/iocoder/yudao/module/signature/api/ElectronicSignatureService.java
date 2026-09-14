package cn.iocoder.yudao.module.signature.api;

import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;

public interface ElectronicSignatureService {

    ElectronicSignatureResult sign(ElectronicSignatureCommand command);

}
