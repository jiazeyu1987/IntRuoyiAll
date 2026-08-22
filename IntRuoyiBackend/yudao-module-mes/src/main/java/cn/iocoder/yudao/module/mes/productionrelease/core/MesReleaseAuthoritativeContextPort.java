package cn.iocoder.yudao.module.mes.productionrelease.core;

public interface MesReleaseAuthoritativeContextPort {

    MesReleaseFinalizationEvidence require(MesReleaseFinalizationCommand command);
}
