package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionRestoreService;
import lombok.Data;

import java.util.List;

@Data
public class DccNasPermissionRestorePreviewRespVO {

    private Long taskId;
    private Boolean canRestore;
    private String planHash;
    private String restoreMode;
    private Long directoryCount;
    private Long ruleCount;
    private Boolean runtimeEnforcementReady;
    private String runtimeEnforcementBlocker;
    private List<RestoreBlocker> blockers;
    private List<RestoreRulePreview> sampleRules;

    public static DccNasPermissionRestorePreviewRespVO of(DccNasPermissionRestoreService.PreviewResult result) {
        DccNasPermissionRestorePreviewRespVO respVO = new DccNasPermissionRestorePreviewRespVO();
        respVO.setTaskId(result.taskId());
        respVO.setCanRestore(result.canRestore());
        respVO.setPlanHash(result.planHash());
        respVO.setRestoreMode(result.restoreMode());
        respVO.setDirectoryCount(result.directoryCount());
        respVO.setRuleCount(result.ruleCount());
        respVO.setRuntimeEnforcementReady(result.runtimeEnforcementReady());
        respVO.setRuntimeEnforcementBlocker(result.runtimeEnforcementBlocker());
        respVO.setBlockers(result.blockers().stream().map(RestoreBlocker::of).toList());
        respVO.setSampleRules(result.sampleRules().stream().map(RestoreRulePreview::of).toList());
        return respVO;
    }

    @Data
    public static class RestoreBlocker {

        private String code;
        private String message;
        private Long directorySnapshotId;
        private String nasPath;
        private String trusteeSid;

        private static RestoreBlocker of(DccNasPermissionRestoreService.RestoreBlocker blocker) {
            RestoreBlocker respVO = new RestoreBlocker();
            respVO.setCode(blocker.code());
            respVO.setMessage(blocker.message());
            respVO.setDirectorySnapshotId(blocker.directorySnapshotId());
            respVO.setNasPath(blocker.nasPath());
            respVO.setTrusteeSid(blocker.trusteeSid());
            return respVO;
        }
    }

    @Data
    public static class RestoreRulePreview {

        private Long directoryId;
        private String nasPath;
        private String subjectType;
        private Long subjectId;
        private Boolean canQuery;
        private Boolean canPreview;
        private Boolean canDownload;

        private static RestoreRulePreview of(DccNasPermissionRestoreService.RestoreRulePreview rule) {
            RestoreRulePreview respVO = new RestoreRulePreview();
            respVO.setDirectoryId(rule.directoryId());
            respVO.setNasPath(rule.nasPath());
            respVO.setSubjectType(rule.subjectType());
            respVO.setSubjectId(rule.subjectId());
            respVO.setCanQuery(rule.canQuery());
            respVO.setCanPreview(rule.canPreview());
            respVO.setCanDownload(rule.canDownload());
            return respVO;
        }
    }
}
