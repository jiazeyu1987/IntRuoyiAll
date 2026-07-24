package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;

import java.util.List;

public interface DccNasPrincipalMappingService {

    DccNasAclIdentityMappingDO saveMapping(SaveMappingCommand command);

    List<UnmappedPrincipal> listUnmappedPrincipals(Long taskId);

    record SaveMappingCommand(String sourceAuthority,
                              String sourceSid,
                              String sourceName,
                              String accountName,
                              String accountType,
                              String targetSubjectType,
                              Long targetSubjectId,
                              Boolean active,
                              String changeReason,
                              Long operatorUserId) {
    }

    record UnmappedPrincipal(String sourceAuthority,
                             String sourceSid,
                             String sourceName,
                             String sidHash,
                             int aceCount,
                             String firstNasPath) {

        public String sid() {
            return sourceSid;
        }
    }
}
