package cn.iocoder.yudao.module.dcc.service.token;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;

/** Signed server-side file identity, not a claim about bytes at an unchanged storage location. */
public record DccPreviewSourceIdentity(Long infraFileId, Long configId, String path,
                                       String name, String contentType, Long size) {
    public DccPreviewSourceIdentity {
        if (infraFileId == null || infraFileId <= 0 || configId == null || configId <= 0
                || path == null || path.isBlank() || size == null || size < 0) {
            throw new IllegalArgumentException("Preview source requires the persisted file identity and storage facts");
        }
        // infra_file.name/type are nullable. Sign their real values, including null, without filling descriptions.
    }

    public static DccPreviewSourceIdentity fromFile(FileDO file) {
        if (file == null || Boolean.TRUE.equals(file.getDeleted()))
            throw new IllegalArgumentException("Preview source file must exist and be retained");
        return new DccPreviewSourceIdentity(file.getId(), file.getConfigId(), file.getPath(),
                file.getName(), file.getType(), file.getSize());
    }
}
