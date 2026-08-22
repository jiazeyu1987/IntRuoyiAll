package cn.iocoder.yudao.module.mes.service.pro.productionrelease;

public interface MesReleaseUpstreamStatePort {

    MesReleaseUpstreamClosureResult closeAfterRelease(MesReleaseUpstreamClosureCommand command);
}
