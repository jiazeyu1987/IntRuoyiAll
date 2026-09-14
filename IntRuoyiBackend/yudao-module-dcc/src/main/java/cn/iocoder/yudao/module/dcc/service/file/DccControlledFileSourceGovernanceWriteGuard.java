package cn.iocoder.yudao.module.dcc.service.file;

final class DccControlledFileSourceGovernanceWriteGuard {

    private DccControlledFileSourceGovernanceWriteGuard() {
    }

    static void requireExactlyOne(int affectedRows, String operation) {
        if (affectedRows != 1) {
            throw new IllegalStateException(operation + " affected " + affectedRows + " rows; expected exactly 1");
        }
    }
}
