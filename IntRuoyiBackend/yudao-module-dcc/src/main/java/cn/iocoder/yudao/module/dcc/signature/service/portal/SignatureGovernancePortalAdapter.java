package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

public interface SignatureGovernancePortalAdapter {

    SignatureGovernanceModuleCode getModuleCode();

    String getModuleName();

    String getModuleDescription();

    String getPrimaryRouteLabel();

    String getPrimaryRoute();

    String getSecondaryRouteLabel();

    String getSecondaryRoute();

    SignatureGovernancePortalMetrics describeMetrics(Long userId);
}
