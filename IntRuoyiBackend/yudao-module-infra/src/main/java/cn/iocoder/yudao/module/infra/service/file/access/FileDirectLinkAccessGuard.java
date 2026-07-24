package cn.iocoder.yudao.module.infra.service.file.access;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;

public interface FileDirectLinkAccessGuard {

    void assertAllowed(FileDO file, FileDirectLinkAccessContext context);

    final class ControlledFileDirectLinkBlockedException extends RuntimeException {

        private final Long fileId;

        public ControlledFileDirectLinkBlockedException(Long fileId) {
            super("DCC controlled file direct link is blocked: fileId=" + fileId);
            this.fileId = fileId;
        }

        public Long getFileId() {
            return fileId;
        }
    }
}
