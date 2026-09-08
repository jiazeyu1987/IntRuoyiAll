package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuntimeTrustedTimeCollectorImpl implements RuntimeTrustedTimeCollector {

    static final String MAX_OFFSET_ENV = "INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS";
    private static final String SCRIPT_PATH = "script/deploy/show-int-ruoyi-trusted-time.ps1";
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(60);
    private static final Map<String, Target> TARGETS = targets();

    private final RuntimeControlProperties properties;
    private final RuntimeControlCommandExecutor commandExecutor;
    private final RuntimeTrustedTimeParser parser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RuntimeTrustedTimeCollectorImpl(RuntimeControlProperties properties,
                                           RuntimeControlCommandExecutor commandExecutor) {
        this(properties, commandExecutor, configuredMaxOffsetMillis());
    }

    RuntimeTrustedTimeCollectorImpl(RuntimeControlProperties properties,
                                    RuntimeControlCommandExecutor commandExecutor,
                                    Double maxOffsetMillis) {
        this.properties = properties;
        this.commandExecutor = commandExecutor;
        this.parser = new RuntimeTrustedTimeParser(maxOffsetMillis);
    }

    @Override
    public List<RuntimeControlInspectionCheckRespVO> collect() {
        List<RuntimeControlInspectionCheckRespVO> checks = new ArrayList<>();
        TARGETS.forEach((environment, target) -> checks.add(collect(environment, target)));
        return checks;
    }

    private RuntimeControlInspectionCheckRespVO collect(String environmentKey, Target target) {
        RuntimeControlProperties.Environment environment = properties.getEnvironments().get(environmentKey);
        if (environment == null || StrUtil.isBlank(environment.getHost())) {
            return parser.blocked(environmentKey, target.nodeName(), target.host(),
                    "缺少 " + environmentKey + " 服务器配置");
        }
        if (!target.host().equals(environment.getHost())) {
            return parser.blocked(environmentKey, target.nodeName(), target.host(),
                    "配置服务器与固定目标不一致：" + environment.getHost());
        }
        try {
            String output = commandExecutor.executeForOutput(buildCommand(environmentKey, environment), COMMAND_TIMEOUT);
            RuntimeTrustedTimeCommandOutput commandOutput = objectMapper.readValue(
                    output, RuntimeTrustedTimeCommandOutput.class);
            return parser.parse(environmentKey, target.nodeName(), target.host(), commandOutput);
        } catch (JsonProcessingException ex) {
            return parser.blocked(environmentKey, target.nodeName(), target.host(),
                    "时间检查命令返回无效 JSON：" + ex.getOriginalMessage());
        } catch (RuntimeException ex) {
            return parser.blocked(environmentKey, target.nodeName(), target.host(),
                    "时间检查命令失败：" + StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private RuntimeControlCommand buildCommand(String environmentKey,
                                                RuntimeControlProperties.Environment environment) {
        List<String> args = new ArrayList<>();
        args.add("-TargetEnvironment");
        args.add(environmentKey);
        args.add("-ServerHost");
        args.add(environment.getHost());
        args.add("-ServerUser");
        args.add(environment.getServerUser());
        args.add("-RemoteAppDir");
        args.add(environment.getRemoteAppDir());
        return new RuntimeControlCommand(environmentKey, "trusted-time", SCRIPT_PATH, args);
    }

    private static Map<String, Target> targets() {
        Map<String, Target> result = new LinkedHashMap<>();
        result.put("prod", new Target("正式服", RuntimeControlProperties.PROD_SERVER_HOST));
        result.put("backup", new Target("审查服", RuntimeControlProperties.BACKUP_SERVER_HOST));
        return result;
    }

    private static Double configuredMaxOffsetMillis() {
        return configuredMaxOffsetMillis(System.getenv(MAX_OFFSET_ENV));
    }

    static Double configuredMaxOffsetMillis(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (!Double.isFinite(value) || value <= 0) {
                throw invalidThreshold(raw);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw invalidThreshold(raw);
        }
    }

    private static IllegalStateException invalidThreshold(String raw) {
        return new IllegalStateException(MAX_OFFSET_ENV + " 必须为空或正数毫秒值，实际值：" + raw);
    }

    private record Target(String nodeName, String host) {
    }
}
