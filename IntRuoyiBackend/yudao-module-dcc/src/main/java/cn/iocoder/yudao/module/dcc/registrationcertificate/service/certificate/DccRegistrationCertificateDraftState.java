package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;

import java.util.List;

public record DccRegistrationCertificateDraftState(
        DccRegistrationCertificateDO certificate,
        DccRegistrationCertificateVersionDO version,
        DccRegistrationCertificateSnapshotDO snapshot,
        List<DccRegistrationCertificateSnapshotEntrustedDO> entrustedProjection) {

    public DccRegistrationCertificateDraftState {
        entrustedProjection = List.copyOf(entrustedProjection);
    }
}
