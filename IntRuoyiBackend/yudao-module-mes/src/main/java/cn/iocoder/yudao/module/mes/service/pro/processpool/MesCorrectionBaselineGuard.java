package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MesCorrectionBaselineGuard {
    private final MesProProcessPoolEventRevisionMapper revisions;
    public static String payloadHash(String payload) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(payload));
    }
    public static String challenge(Long eventId, String before, String after, String reason) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256("CORRECTION_V2|" + eventId + "|" + payloadHash(before)
                + "|" + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(after) + "|" + reason.trim());
    }
    public void requireCurrent(MesProProcessPoolEventDO event, Long expectedRevisionId, String expectedPayloadHash) {
        var rows = revisions.selectListByEventIdForUpdate(event.getId());
        Long current = rows.isEmpty() ? 0L : rows.get(0).getId();
        if (expectedRevisionId == null || expectedRevisionId < 0 || expectedPayloadHash == null
                || !Objects.equals(current, expectedRevisionId)
                || !payloadHash(event.getRawPayload()).equals(expectedPayloadHash)) {
            throw ServiceExceptionUtil.invalidParamException("记录已更新，请重新加载并核对后补正；本次输入尚未提交。");
        }
    }
}
