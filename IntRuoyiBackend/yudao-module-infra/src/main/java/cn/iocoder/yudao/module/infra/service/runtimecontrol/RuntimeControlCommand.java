package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeControlCommand {

    private String environment;
    private String component;
    private String scriptPath;
    private List<String> arguments;
    private String workingDirectory;

    public RuntimeControlCommand(String environment, String component, String scriptPath, List<String> arguments) {
        this(environment, component, scriptPath, arguments, null);
    }

    public RuntimeControlCommand(String environment, String component, String scriptPath, List<String> arguments,
                                 String workingDirectory) {
        this.environment = environment;
        this.component = component;
        this.scriptPath = scriptPath;
        this.arguments = arguments;
        this.workingDirectory = workingDirectory;
    }
}
