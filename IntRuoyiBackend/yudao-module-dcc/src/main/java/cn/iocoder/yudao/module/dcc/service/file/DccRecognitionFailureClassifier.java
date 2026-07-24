package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_IN_PROGRESS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_NO_CANDIDATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING;

final class DccRecognitionFailureClassifier {

    static final String STAGE_PRECONDITION = "PRECONDITION";
    static final String STAGE_SOURCE_ACCESS = "SOURCE_ACCESS";
    static final String STAGE_RULE_MATCHING = "RULE_MATCHING";
    static final String STAGE_AI_CLASSIFICATION = "AI_CLASSIFICATION";
    static final String STAGE_RESULT_VALIDATION = "RESULT_VALIDATION";
    static final String STAGE_PERSISTENCE = "PERSISTENCE";
    static final String STAGE_BATCH_ORCHESTRATION = "BATCH_ORCHESTRATION";

    static final String CODE_CONTROLLED_FILE_NOT_FOUND = "CONTROLLED_FILE_NOT_FOUND";
    static final String CODE_SOURCE_FILE_MISSING = "SOURCE_FILE_MISSING";
    static final String CODE_RECOGNITION_CONFIG_MISSING = "RECOGNITION_CONFIG_MISSING";
    static final String CODE_NO_ENABLED_CANDIDATE = "NO_ENABLED_CANDIDATE";
    static final String CODE_RECOGNITION_IN_PROGRESS = "RECOGNITION_IN_PROGRESS";
    static final String CODE_AMBIGUOUS_RESULT = "AMBIGUOUS_RESULT";
    static final String CODE_INVALID_RESULT = "INVALID_RESULT";
    static final String CODE_SOURCE_READ_FAILED = "SOURCE_READ_FAILED";
    static final String CODE_AI_REQUEST_FAILED = "AI_REQUEST_FAILED";
    static final String CODE_PERSISTENCE_FAILED = "PERSISTENCE_FAILED";
    static final String CODE_FILE_CATEGORY_PROJECT_CODE_MISSING = "FILE_CATEGORY_PROJECT_CODE_MISSING";
    static final String CODE_FILE_CATEGORY_RESPONSE_INVALID = "FILE_CATEGORY_RESPONSE_INVALID";
    static final String CODE_BATCH_CANDIDATE_FAILED = "BATCH_CANDIDATE_FAILED";
    static final String CODE_UNEXPECTED_FAILURE = "UNEXPECTED_FAILURE";

    private DccRecognitionFailureClassifier() {
    }

    static FailureMetadata classify(Throwable throwable, String currentStage, String defaultCode) {
        ServiceException serviceException = findServiceException(throwable);
        if (serviceException == null) {
            return new FailureMetadata(currentStage, defaultCode);
        }
        int code = serviceException.getCode();
        if (code == CONTROLLED_FILE_NOT_EXISTS.getCode()) {
            return new FailureMetadata(STAGE_PRECONDITION, CODE_CONTROLLED_FILE_NOT_FOUND);
        }
        if (code == CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING.getCode()) {
            return new FailureMetadata(STAGE_PRECONDITION, CODE_SOURCE_FILE_MISSING);
        }
        if (code == CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING.getCode()) {
            return new FailureMetadata(STAGE_PRECONDITION, CODE_RECOGNITION_CONFIG_MISSING);
        }
        if (code == CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_NO_CANDIDATE.getCode()) {
            return new FailureMetadata(STAGE_PRECONDITION, CODE_NO_ENABLED_CANDIDATE);
        }
        if (code == CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_IN_PROGRESS.getCode()) {
            return new FailureMetadata(STAGE_PRECONDITION, CODE_RECOGNITION_IN_PROGRESS);
        }
        if (code == CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS.getCode()) {
            return new FailureMetadata(STAGE_RULE_MATCHING, CODE_AMBIGUOUS_RESULT);
        }
        if (code == CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE.getCode()) {
            return new FailureMetadata(STAGE_RESULT_VALIDATION, CODE_INVALID_RESULT);
        }
        if (code == CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED.getCode()) {
            return new FailureMetadata(STAGE_PERSISTENCE, CODE_PERSISTENCE_FAILED);
        }
        return new FailureMetadata(currentStage, defaultCode);
    }

    static String defaultCodeForStage(String stage) {
        return switch (stage) {
            case STAGE_SOURCE_ACCESS -> CODE_SOURCE_READ_FAILED;
            case STAGE_AI_CLASSIFICATION -> CODE_AI_REQUEST_FAILED;
            case STAGE_RESULT_VALIDATION -> CODE_INVALID_RESULT;
            case STAGE_PERSISTENCE -> CODE_PERSISTENCE_FAILED;
            case STAGE_BATCH_ORCHESTRATION -> CODE_BATCH_CANDIDATE_FAILED;
            default -> CODE_UNEXPECTED_FAILURE;
        };
    }

    private static ServiceException findServiceException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof ServiceException serviceException) {
                return serviceException;
            }
            current = current.getCause();
        }
        return null;
    }

    record FailureMetadata(String stage, String code) {
    }
}
