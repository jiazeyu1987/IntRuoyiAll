package cn.iocoder.yudao.module.showroom.foundation.contract;

public final class ShowroomNotifyPrerequisiteChecker {

    private ShowroomNotifyPrerequisiteChecker() {
    }

    public static void validateBeforeSend(Long recipientUserId, String templateCode, Long persistedNotifyMessageId) {
        if (recipientUserId == null) {
            throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: recipient user is required");
        }
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: notify template code is required");
        }
        if (persistedNotifyMessageId == null) {
            throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: persisted notify message is required");
        }
    }

}
