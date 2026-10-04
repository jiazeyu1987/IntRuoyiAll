package cn.iocoder.yudao.module.dcc.service.file;

/** Source NAS directory and B project folder are separate identities, never interchangeable. */
public record DccControlledFileSelectorQuery(Long projectId, Long sourceDirectoryId, Long projectFolderId,
                                            String keyword, int pageNo, int pageSize) {
    public DccControlledFileSelectorQuery validated() {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200
                || projectId != null && projectId < 1 || sourceDirectoryId != null && sourceDirectoryId < 1
                || projectFolderId != null && projectFolderId < 1) throw new IllegalArgumentException("invalid selector query identity or page");
        if (projectFolderId != null && projectId == null) throw new IllegalArgumentException("project folder requires exact source project");
        return this;
    }
}
