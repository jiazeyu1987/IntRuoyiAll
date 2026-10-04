package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Notification;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class DccRelationPlatformNotificationSender implements DccRelationNotificationSender {
    public static final String TEMPLATE_CODE="dcc_relation_remediation";
    private final NotifyMessageSendApi platform;
    private final DccLatestControlledFileResolver files;
    public DccRelationPlatformNotificationSender(NotifyMessageSendApi platform,DccLatestControlledFileResolver files){
        this.platform=platform;this.files=files;
    }
    @Override
    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public Long send(Notification notification){
        if(notification==null || notification.businessKey()==null || notification.businessKey().isBlank()
                || notification.businessKey().length()>255 || notification.recipientUserId()==null || notification.recipientUserId()<=0
                || notification.sourceControlledFileId()==null || notification.sourceControlledFileId()<=0
                || notification.relatedMasterId()==null || notification.relatedMasterId()<=0 || notification.dueAt()==null
                || notification.fileNumberSnapshot()==null || notification.fileNumberSnapshot().isBlank()
                || notification.versionNoSnapshot()==null || notification.versionNoSnapshot().isBlank())
            throw new DccRelationInputFailure("DCC_NOTIFICATION_INPUT_INVALID");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var source=files.resolveSelected(notification.sourceControlledFileId());
        if(source==null || !Objects.equals(tenant,source.tenantId())
                || !Objects.equals(notification.sourceControlledFileId(),source.controlledFileId()))
            throw new DccRelationFailure("DCC_NOTIFICATION_SOURCE_IDENTITY_INVALID");
        var request=new NotifySendSingleToUserIdempotentReqDTO();
        request.setUserId(notification.recipientUserId());request.setTemplateCode(TEMPLATE_CODE);
        request.setBusinessKey(notification.businessKey());
        var params=new LinkedHashMap<String,Object>();
        params.put("fileNumber",notification.fileNumberSnapshot());params.put("versionNo",notification.versionNoSnapshot());
        params.put("dueAt",notification.dueAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("relatedMasterId",String.valueOf(notification.relatedMasterId()));
        params.put("sourceControlledFileId",String.valueOf(notification.sourceControlledFileId()));
        params.put("detailUrl","/dcc/controlled-file/detail/"+notification.sourceControlledFileId()+"?viewer=1&from=notification");
        request.setTemplateParams(params);
        Long messageId=platform.sendSingleMessageIdempotentlyToAdmin(request);
        if(messageId==null || messageId<=0) throw new DccRelationFailure("DCC_PLATFORM_MESSAGE_ID_MISSING");
        return messageId;
    }
}
