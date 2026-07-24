package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RuntimeControlCommand {

    private String environment;
    private String component;
    private String scriptPath;
    private List<String> arguments;
}
