package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Read witness for the Flow8 source precheck. Expected values are only
 * comparison witnesses; the current values always come from persistence.
 */
@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceSourcePrecheckCommand {

    private Long batchExecutionId;
    private Long originLinkId;
    private String expectedTraceLinkHash;
    private String expectedSourceSnapshotHash;
    private Integer expectedSourceVersion;
}
