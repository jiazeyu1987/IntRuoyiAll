package cn.iocoder.yudao.module.dcc.service.file;

import java.util.List;

public record DccProjectCodeRecognitionCommand(Long controlledFileId,
                                               Long sourceFileId,
                                               String sourceFileName,
                                               String contentType,
                                               byte[] sourceContent,
                                               List<Candidate> candidates) {

    public record Candidate(Long id,
                            String projectName,
                            String projectCode,
                            String category,
                            String priority) {
    }
}
