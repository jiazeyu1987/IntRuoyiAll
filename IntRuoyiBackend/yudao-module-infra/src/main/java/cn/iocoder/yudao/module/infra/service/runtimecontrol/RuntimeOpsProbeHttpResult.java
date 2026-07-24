package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuntimeOpsProbeHttpResult {

    private Integer statusCode;

    private Long durationMillis;
}
