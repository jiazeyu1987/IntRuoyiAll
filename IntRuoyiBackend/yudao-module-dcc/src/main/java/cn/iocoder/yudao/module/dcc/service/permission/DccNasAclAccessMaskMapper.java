package cn.iocoder.yudao.module.dcc.service.permission;

final class DccNasAclAccessMaskMapper {

    private static final long FILE_READ_DATA_OR_LIST_DIRECTORY = 0x0001L;
    private static final long FILE_READ_EA = 0x0008L;
    private static final long FILE_EXECUTE_OR_TRAVERSE = 0x0020L;
    private static final long FILE_READ_ATTRIBUTES = 0x0080L;

    private DccNasAclAccessMaskMapper() {
    }

    static DccPermissions toDccPermissions(Long accessMask) {
        if (accessMask == null) {
            return null;
        }
        boolean canReadData = has(accessMask, FILE_READ_DATA_OR_LIST_DIRECTORY);
        boolean canQuery = canReadData
                || hasAny(accessMask, FILE_READ_EA, FILE_EXECUTE_OR_TRAVERSE, FILE_READ_ATTRIBUTES);
        boolean canPreview = canReadData;
        boolean canDownload = canReadData;
        if (!canQuery && !canPreview && !canDownload) {
            return null;
        }
        return new DccPermissions(canQuery, canPreview, canDownload);
    }

    private static boolean hasAny(long accessMask, long... bits) {
        for (long bit : bits) {
            if (has(accessMask, bit)) {
                return true;
            }
        }
        return false;
    }

    private static boolean has(long accessMask, long bit) {
        return (accessMask & bit) == bit;
    }

    record DccPermissions(boolean canQuery, boolean canPreview, boolean canDownload) {
    }
}
