package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

import java.util.Arrays;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;

/** Binds temporary tickets to their validated source-upload business identity. */
final class DccSourceUploadSession {
    private DccSourceUploadSession() { }

    static String newUploadPrefix(Long projectId, Long taxonomyId, String fileName) {
        return "dcc-new:" + DigestUtil.sha256Hex(JsonUtils.toJsonString(
                Arrays.asList(projectId, taxonomyId, StrUtil.trim(fileName)))) + ":";
    }

    static String checkinPrefix(Long versionId) {
        return "dcc-checkin:" + versionId + ":";
    }

    static String externalPrefix() {
        return "dcc-external:";
    }

    static String scope(String prefix, String clientSessionId) {
        if (StrUtil.isBlank(clientSessionId)) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        return prefix + DigestUtil.sha256Hex(clientSessionId.trim()).substring(0, 32);
    }

    static void require(String sessionId, String prefix) {
        if (sessionId == null || !sessionId.startsWith(prefix)
                || !sessionId.substring(prefix.length()).matches("[0-9a-f]{32}")) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
    }
}
