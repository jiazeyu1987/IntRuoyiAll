package cn.iocoder.yudao.module.mes.service.pro.frontline;

public interface MesFrontlineSubmitAuthorizationService {

    MesFrontlineSubmitIdentityTrace authorize(MesFrontlineSubmitIdentityCommand command);

}
