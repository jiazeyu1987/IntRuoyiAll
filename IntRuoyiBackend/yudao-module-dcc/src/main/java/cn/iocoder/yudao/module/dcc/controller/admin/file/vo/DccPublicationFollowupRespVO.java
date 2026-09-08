package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccPublicationFollowupRespVO {
    private String id;
    private String publishedControlledFileId;
    private String fileNumber;
    private String fileName;
    private String versionNo;
    private String status;
    private LocalDateTime publishedAt;
    private List<DccPublicationVisibilityRuleRespVO> visibilityRules;
    private List<DccPublicationNotificationDeliveryRespVO> notificationDeliveries;
    private List<DccPublicationImpactTaskRespVO> impactTasks;
    private List<DccPublicationTimelineEventRespVO> timeline;
}
