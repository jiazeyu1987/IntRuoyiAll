package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import java.time.Duration;

public interface RuntimeOpsProbeHttpClient {

    RuntimeOpsProbeHttpResult probe(String url, Duration timeout);
}
