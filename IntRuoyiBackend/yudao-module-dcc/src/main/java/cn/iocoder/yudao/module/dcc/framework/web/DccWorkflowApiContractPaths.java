package cn.iocoder.yudao.module.dcc.framework.web;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.web.config.WebProperties;

import java.util.Set;

final class DccWorkflowApiContractPaths {

    private static final Set<String> ACTIONS = Set.of(
            "approve-task", "reject-task", "return-task", "transfer-task", "sign-task",
            "task-action-readiness");

    private DccWorkflowApiContractPaths() {
    }

    static boolean matches(WebProperties webProperties, String requestPath) {
        String prefix = controlledFilesPrefix(webProperties) + "/";
        if (!requestPath.startsWith(prefix)) {
            return false;
        }
        String[] segments = requestPath.substring(prefix.length()).split("/", -1);
        return segments.length == 2 && segments[0].matches("[1-9][0-9]*") && ACTIONS.contains(segments[1]);
    }

    static String registrationPattern(WebProperties webProperties) {
        return controlledFilesPrefix(webProperties) + "/*";
    }

    private static String controlledFilesPrefix(WebProperties webProperties) {
        return StrUtil.removeSuffix(webProperties.getAdminApi().getPrefix(), "/") + "/dcc/controlled-files";
    }
}
