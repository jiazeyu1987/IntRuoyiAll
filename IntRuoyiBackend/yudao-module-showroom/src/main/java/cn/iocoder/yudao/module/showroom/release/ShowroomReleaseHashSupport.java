package cn.iocoder.yudao.module.showroom.release;

import cn.hutool.crypto.digest.DigestUtil;

final class ShowroomReleaseHashSupport {

    private ShowroomReleaseHashSupport() {
    }

    static String sha256Hex(byte[] bytes) {
        return DigestUtil.sha256Hex(bytes);
    }

    static String sha256Hex(String value) {
        return DigestUtil.sha256Hex(value.getBytes(ShowroomReleaseConstants.UTF_8));
    }
}
