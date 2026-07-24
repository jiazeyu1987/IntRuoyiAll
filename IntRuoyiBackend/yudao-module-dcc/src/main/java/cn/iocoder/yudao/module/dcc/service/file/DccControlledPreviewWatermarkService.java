package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;

@Service
@Validated
public class DccControlledPreviewWatermarkService {

    private static final DateTimeFormatter WATERMARK_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String WATERMARK_LABEL = "受控预览";

    @Resource
    private AdminUserService adminUserService;

    public DccControlledPreviewWatermarkRespVO build(Long actorId, String purpose, String fileName) {
        AdminUserDO user = adminUserService.getUser(actorId);
        if (user == null || StrUtil.isBlank(user.getUsername())) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        String actorName = StrUtil.blankToDefault(StrUtil.trim(user.getNickname()), StrUtil.trim(user.getUsername()));
        String actorAccount = StrUtil.trim(user.getUsername());
        String normalizedPurpose = StrUtil.blankToDefault(StrUtil.trim(purpose), "preview");
        String normalizedFileName = StrUtil.blankToDefault(StrUtil.trim(fileName), "-");
        String timestamp = LocalDateTime.now().format(WATERMARK_TIME_FORMATTER);
        return DccControlledPreviewWatermarkRespVO.builder()
                .label(WATERMARK_LABEL)
                .text(String.format("%s | %s | %s | %s | %s",
                        WATERMARK_LABEL, actorName, actorAccount, normalizedFileName, timestamp))
                .actorName(actorName)
                .actorAccount(actorAccount)
                .timestamp(timestamp)
                .purpose(normalizedPurpose)
                .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                        .textColor("#6b7280")
                        .opacity(0.18D)
                        .rotationDeg(-24)
                        .gapX(260)
                        .gapY(180)
                        .fontSize(18)
                        .build())
                .build();
    }
}
