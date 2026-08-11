package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureBindingDO;

public record DccControlledFileSignatureBindingVerification(
        boolean valid,
        String reasonCode,
        DccControlledFileSignatureBindingDO binding) {

    public static DccControlledFileSignatureBindingVerification notApplicable() {
        return new DccControlledFileSignatureBindingVerification(true, "", null);
    }

    public static DccControlledFileSignatureBindingVerification bound(
            DccControlledFileSignatureBindingDO binding) {
        return new DccControlledFileSignatureBindingVerification(true, "", binding);
    }

    public static DccControlledFileSignatureBindingVerification invalid(String reasonCode) {
        return new DccControlledFileSignatureBindingVerification(false, reasonCode, null);
    }

    public static DccControlledFileSignatureBindingVerification invalid(
            String reasonCode, DccControlledFileSignatureBindingDO binding) {
        return new DccControlledFileSignatureBindingVerification(false, reasonCode, binding);
    }

}
