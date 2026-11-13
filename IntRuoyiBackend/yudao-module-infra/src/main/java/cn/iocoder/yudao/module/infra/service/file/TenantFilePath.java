package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;

/** Object keys are relative paths below the current tenant's namespace. */
final class TenantFilePath {
    private TenantFilePath() {}

    static String prefix() {
        return "tenants/" + TenantContextHolder.getRequiredTenantId() + "/";
    }

    static String qualify(String path) {
        validateRelative(path);
        if (path.startsWith("tenants/")) {
            requireOwned(path);
            return path;
        }
        return prefix() + path;
    }

    static void requireOwned(String path) {
        validateRelative(path);
        if (!path.startsWith(prefix()) || path.length() == prefix().length()) {
            throw new IllegalArgumentException("文件路径不属于当前租户");
        }
    }

    static void validateRelative(String path) {
        if (path == null || path.isBlank() || path.contains("\\") || path.contains(":")
                || path.contains("%") || path.chars().anyMatch(c -> c < 32)) {
            throw new IllegalArgumentException("文件路径必须是规范的相对路径");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                    || segment.endsWith(".") || segment.endsWith(" ")) {
                throw new IllegalArgumentException("文件路径不能包含空目录、相对目录或不规范名称");
            }
        }
    }
}
