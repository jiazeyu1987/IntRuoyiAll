package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionFailureSummaryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DccControlledFileRecognitionRecordMapperTest extends BaseDbUnitTest {

    @Resource
    private DccControlledFileRecognitionRecordMapper mapper;

    @Test
    void selectFailureSummariesByBatchTaskIdGroupsStructuredFailuresWithoutParsingMessages() {
        mapper.upsert(record(1001L, 9001L, "FAILED", "AI_CLASSIFICATION", "AI_REQUEST_FAILED",
                "AI classification request timed out"));
        mapper.upsert(record(1002L, 9001L, "FAILED", "AI_CLASSIFICATION", "AI_REQUEST_FAILED",
                "AI service temporarily unavailable"));
        mapper.upsert(record(1003L, 9001L, "FAILED", "SOURCE_ACCESS", "SOURCE_FILE_MISSING",
                "source file missing"));
        mapper.upsert(record(1004L, 9001L, "SUCCESS", null, null, null));
        mapper.upsert(record(1005L, 9002L, "FAILED", "AI_CLASSIFICATION", "AI_REQUEST_FAILED",
                "other task failure"));
        mapper.upsert(record(1006L, 9001L, "FAILED", null, null, "legacy failure"));

        List<DccControlledFileRecognitionFailureSummaryDO> summaries =
                mapper.selectFailureSummariesByBatchTaskId(9001L, 3);

        assertEquals(3, summaries.size());
        assertEquals("AI_CLASSIFICATION", summaries.get(0).getFailureStage());
        assertEquals("AI_REQUEST_FAILED", summaries.get(0).getFailureCode());
        assertEquals("AI classification request timed out", summaries.get(0).getFailureMessage());
        assertEquals(2L, summaries.get(0).getFailureCount());
        assertEquals("SOURCE_ACCESS", summaries.get(1).getFailureStage());
        assertEquals("SOURCE_FILE_MISSING", summaries.get(1).getFailureCode());
        assertEquals("source file missing", summaries.get(1).getFailureMessage());
        assertEquals(1L, summaries.get(1).getFailureCount());
        assertEquals("UNCLASSIFIED", summaries.get(2).getFailureStage());
        assertEquals("MISSING_FAILURE_METADATA", summaries.get(2).getFailureCode());
        assertEquals("legacy failure", summaries.get(2).getFailureMessage());
        assertEquals(1L, summaries.get(2).getFailureCount());
    }

    private DccControlledFileRecognitionRecordDO record(Long fileId, Long taskId,
                                                        String status, String failureStage,
                                                        String failureCode, String failureMessage) {
        return DccControlledFileRecognitionRecordDO.builder()
                .tenantId(0L)
                .controlledFileId(fileId)
                .recognitionScope("FILE_CATEGORY")
                .recognitionMethod("BATCH_FILE_CATEGORY")
                .recognitionVersion("file-category-v1")
                .status(status)
                .batchTaskId(taskId)
                .failureStage(failureStage)
                .failureCode(failureCode)
                .failureMessage(failureMessage)
                .recognizedBy(99L)
                .recognizedTime(LocalDateTime.now())
                .build();
    }
}
