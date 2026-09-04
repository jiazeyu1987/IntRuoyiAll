package cn.iocoder.yudao.module.dcc.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * DCC error codes.
 */
public interface ErrorCodeConstants {

    ErrorCode FILE_DIRECTORY_NOT_EXISTS = new ErrorCode(1_080_000_000, "Controlled file directory does not exist");
    ErrorCode FILE_CATEGORY_NOT_EXISTS = new ErrorCode(1_080_000_001, "Controlled file category does not exist");
    ErrorCode FILE_DIRECTORY_PARENT_NOT_EXISTS = new ErrorCode(1_080_000_002, "Controlled file parent directory does not exist");
    ErrorCode APPROVAL_POSITION_NOT_EXISTS = new ErrorCode(1_080_000_003, "Approval position does not exist");
    ErrorCode APPROVAL_ROUTE_NOT_EXISTS = new ErrorCode(1_080_000_004, "Approval route does not exist");
    ErrorCode APPROVAL_ROUTE_NODE_EMPTY = new ErrorCode(1_080_000_005, "Approval route nodes cannot be empty");
    ErrorCode ROUTE_PREVIEW_APPROVER_NOT_FOUND = new ErrorCode(1_080_000_006, "Route preview failed because a stage has no resolved approver");
    ErrorCode FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS = new ErrorCode(1_080_000_007, "File category is not bound to a directory");
    ErrorCode FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS = new ErrorCode(1_080_000_196, "Unclassified upload directory does not exist");
    ErrorCode CONTROLLED_FILE_ROUTE_NOT_CONFIGURED = new ErrorCode(1_080_000_008, "Controlled file route is not configured");
    ErrorCode CONTROLLED_FILE_NOT_EXISTS = new ErrorCode(1_080_000_009, "Controlled file does not exist");
    ErrorCode CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED = new ErrorCode(1_080_000_010, "Current controlled file cannot be withdrawn");
    ErrorCode CONTROLLED_FILE_CATEGORY_DISABLED = new ErrorCode(1_080_000_011, "Controlled file category is disabled");
    ErrorCode CONTROLLED_FILE_ACCESS_DENIED = new ErrorCode(1_080_000_012, "Current user cannot access this controlled file");
    ErrorCode CONTROLLED_FILE_STAMP_TYPE_UNSUPPORTED = new ErrorCode(1_080_000_013, "Controlled file type does not support finalization");
    ErrorCode CONTROLLED_FILE_STAMP_GENERATION_FAILED = new ErrorCode(1_080_000_014, "Controlled file finalization failed");
    ErrorCode CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED = new ErrorCode(1_080_000_015, "Current controlled file cannot retry finalization");
    ErrorCode CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED = new ErrorCode(1_080_000_016, "Upload preview requires exactly one file");
    ErrorCode CONTROLLED_FILE_UPLOAD_PREVIEW_PDF_ONLY = new ErrorCode(1_080_000_017, "Upload preview accepts exactly one file");
    ErrorCode CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING = new ErrorCode(1_080_000_018, "Submit request is missing required metadata");
    ErrorCode CONTROLLED_FILE_VERSION_INVALID = new ErrorCode(1_080_000_019, "Controlled file version format is invalid");
    ErrorCode CONTROLLED_FILE_VERSION_NOT_GREATER = new ErrorCode(1_080_000_020, "Controlled file version must be greater than the current chain version");
    ErrorCode CONTROLLED_FILE_FILE_NUMBER_CONFLICT = new ErrorCode(1_080_000_021, "Controlled file number conflicts with the existing logical document chain");
    ErrorCode CONTROLLED_FILE_TASK_PASSWORD_INVALID = new ErrorCode(1_080_000_022, "Controlled file task password verification failed");
    ErrorCode CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED = new ErrorCode(1_080_000_023, "Controlled file signature evidence persistence failed");
    ErrorCode CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED = new ErrorCode(1_080_000_024, "Current user cannot act on this controlled file task");
    ErrorCode CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED = new ErrorCode(1_080_000_025, "Controlled file task stage is not aligned with the fixed DCC workflow");
    ErrorCode CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED = new ErrorCode(1_080_000_026, "Controlled file obsolete reason is required");
    ErrorCode CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED = new ErrorCode(1_080_000_027, "Current controlled file cannot be obsoleted");
    ErrorCode CONTROLLED_FILE_ACTION_LOCKED = new ErrorCode(1_080_000_180, "Controlled file has pending approval action: {}");
    ErrorCode CONTROLLED_FILE_PUBLISH_NOT_ALLOWED = new ErrorCode(1_080_000_181, "Current controlled file cannot be published");
    ErrorCode CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED = new ErrorCode(1_080_000_028, "Current user cannot acknowledge this controlled file training");
    ErrorCode CONTROLLED_FILE_MANUAL_RELEASE_NOT_ALLOWED = new ErrorCode(1_080_000_054, "Current controlled file cannot be manually released");
    ErrorCode CONTROLLED_FILE_MESSAGE_JOB_REPLAY_REQUEST_INVALID = new ErrorCode(1_080_000_055, "Controlled file message replay request is invalid");
    ErrorCode CONTROLLED_FILE_MESSAGE_JOB_NOT_EXISTS = new ErrorCode(1_080_000_056, "Controlled file message job does not exist");
    ErrorCode CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED = new ErrorCode(1_080_000_057, "Current controlled file message job cannot be replayed");
    ErrorCode CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID = new ErrorCode(1_080_000_058, "Controlled file message job context is invalid");
    ErrorCode APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED = new ErrorCode(1_080_000_059, "Uploader-derived approval position cannot use manual assignments");
    ErrorCode APPROVAL_POSITION_UPLOADER_CONTEXT_REQUIRED = new ErrorCode(1_080_000_060, "Approval position runtime context is required: {}");
    ErrorCode APPROVAL_POSITION_UPLOADER_MAPPING_INVALID = new ErrorCode(1_080_000_061, "Approval position runtime mapping failed: {}");
    ErrorCode INTAUTH_ORG_SOURCE_INVALID = new ErrorCode(1_080_000_062, "IntAuth org source is invalid: {}");
    ErrorCode CONTROLLED_FILE_ONLYOFFICE_PREVIEW_CONFIG_MISSING = new ErrorCode(1_080_000_063, "OnlyOffice preview config is missing: {}");
    ErrorCode CONTROLLED_FILE_DRAWING_PDF_REQUIRED = new ErrorCode(1_080_000_064, "Drawing source files require a paired PDF upload");
    ErrorCode CONTROLLED_FILE_PRODUCT_CODE_INVALID = new ErrorCode(1_080_000_065, "Product code must be exactly 14 alphanumeric characters");
    ErrorCode CONTROLLED_FILE_DOWNLOAD_WARNING_UNCONFIRMED = new ErrorCode(1_080_000_066, "Non-controlled download warning must be confirmed before download");
    ErrorCode CONTROLLED_FILE_STAMPED_PDF_REQUIRED = new ErrorCode(1_080_000_067, "Doc control approval requires a stamped PDF file");
    ErrorCode CONTROLLED_FILE_TRAINING_RECORD_REQUIRED = new ErrorCode(1_080_000_068, "Training record file is required before doc control approval");
    ErrorCode CONTROLLED_FILE_TASK_TARGET_INVALID = new ErrorCode(1_080_000_069, "Controlled file task target stage is invalid");
    ErrorCode CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED = new ErrorCode(1_080_000_070, "Current user cannot acknowledge this electronic distribution receipt");
    ErrorCode CONTROLLED_FILE_PROCESS_TYPE_INVALID = new ErrorCode(1_080_000_071, "Controlled file process type is invalid");
    ErrorCode CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED = new ErrorCode(1_080_000_176, "Doc control approval requires at least one distribution department");
    ErrorCode CONTROLLED_FILE_WORKFLOW_IN_PROGRESS = new ErrorCode(1_080_000_177, "Controlled file number already has an unfinished workflow");
    ErrorCode CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING = new ErrorCode(1_080_000_178, "Controlled file PDF conversion config is missing: {}");
    ErrorCode CONTROLLED_FILE_PDF_CONVERSION_FAILED = new ErrorCode(1_080_000_179, "Controlled file PDF conversion failed: {}");
    ErrorCode DCC_NAS_PRINCIPAL_SID_REQUIRED = new ErrorCode(1_080_000_072, "DCC NAS principal SID is required");
    ErrorCode DCC_NAS_PRINCIPAL_TARGET_TYPE_INVALID = new ErrorCode(1_080_000_073, "DCC NAS mapping targetSubjectType is invalid: {}");
    ErrorCode DCC_NAS_PRINCIPAL_TARGET_ID_REQUIRED = new ErrorCode(1_080_000_074, "DCC NAS mapping targetSubjectId is required");
    ErrorCode DCC_NAS_PRINCIPAL_MAPPING_CONFLICT = new ErrorCode(1_080_000_075, "DCC NAS principal mapping conflict: {}");
    ErrorCode DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY = new ErrorCode(1_080_000_076, "DCC NAS permission snapshot is not ready for task: {}");
    ErrorCode DCC_NAS_PERMISSION_RESTORE_BLOCKED = new ErrorCode(1_080_000_077, "DCC NAS permission restore is blocked: {}");
    ErrorCode DCC_NAS_PERMISSION_RESTORE_PLAN_STALE = new ErrorCode(1_080_000_078, "DCC NAS permission restore plan is stale");
    ErrorCode DCC_NAS_PERMISSION_RESTORE_MODE_UNSUPPORTED = new ErrorCode(1_080_000_079, "DCC NAS permission restore mode is unsupported: {}");
    ErrorCode DCC_NAS_PERMISSION_RESTORE_IDEMPOTENCY_CONFLICT = new ErrorCode(1_080_000_080, "DCC NAS permission restore idempotency conflict: {}");
    ErrorCode CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED = new ErrorCode(1_080_000_049, "Current user cannot acknowledge this paper distribution");
    ErrorCode CONTROLLED_FILE_TRAINING_VIEW_SECONDS_NOT_ENOUGH = new ErrorCode(1_080_000_047, "Current user has not completed the required training view time");
    ErrorCode CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID = new ErrorCode(1_080_000_048, "Controlled file distribution medium is invalid");
    ErrorCode CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID = new ErrorCode(1_080_000_072,
            "Controlled source file must be one of: doc、docx、xls、xlsx、dwg、sldprt、sldasm、slddrw");
    ErrorCode CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID = new ErrorCode(1_080_000_073, "Drawing paired file must be a real PDF");
    ErrorCode CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID = new ErrorCode(1_080_000_074, "Controlled file upload purpose is invalid");
    ErrorCode CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED = new ErrorCode(1_080_000_075,
            "Only the requester can delete or resubmit a withdrawn controlled file workflow");
    ErrorCode EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_MISSING = new ErrorCode(1_080_000_076,
            "External file review BPM process definition is missing");
    ErrorCode EXTERNAL_FILE_REVIEW_REQUIRED_METADATA_MISSING = new ErrorCode(1_080_000_077,
            "External file review request is missing required metadata");
    ErrorCode EXTERNAL_FILE_REVIEW_OUTPUT_FILE_REQUIRED = new ErrorCode(1_080_000_078,
            "External file review conclusion requires a real output file");
    ErrorCode EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED = new ErrorCode(1_080_000_079,
            "External file review must be submitted through the external review endpoint");
    ErrorCode APPROVAL_PRINT_TEMPLATE_NOT_CONFIGURED = new ErrorCode(1_080_000_080,
            "DCC approval print template is not configured");
    ErrorCode APPROVAL_PRINT_TEMPLATE_FILE_NOT_EXISTS = new ErrorCode(1_080_000_081,
            "DCC approval print template file does not exist");
    ErrorCode APPROVAL_PRINT_TEMPLATE_FILE_INVALID = new ErrorCode(1_080_000_082,
            "DCC approval print template must be a readable .docx file");
    ErrorCode APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_MISSING = new ErrorCode(1_080_000_083,
            "DCC approval print template is missing required placeholders");
    ErrorCode APPROVAL_PRINT_TEMPLATE_RENDER_FAILED = new ErrorCode(1_080_000_084,
            "DCC approval print template render failed");
    ErrorCode APPROVAL_PRINT_TEMPLATE_PLACEHOLDER_UNSUPPORTED = new ErrorCode(1_080_000_085,
            "DCC approval print template contains unsupported placeholders");
    ErrorCode CONTROLLED_FILE_SIGNATURE_CONFIG_MISSING = new ErrorCode(1_080_000_086, "DCC electronic signature evidence configuration is missing");
    ErrorCode CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING = new ErrorCode(1_080_000_087, "DCC electronic signature evidence prerequisite is missing");
    ErrorCode CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID = new ErrorCode(1_080_000_088, "DCC electronic signature evidence is invalid");
    ErrorCode CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED = new ErrorCode(1_080_000_089, "DCC electronic signature authorization change reason is required");
    ErrorCode CONTROLLED_FILE_SIGNATURE_LOCKED = new ErrorCode(1_080_000_090, "DCC electronic signature authorization is locked");
    ErrorCode CONTROLLED_FILE_SIGNATURE_POLICY_MISSING = new ErrorCode(1_080_000_091, "DCC electronic signature lock policy is missing");
    ErrorCode CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED = new ErrorCode(1_080_000_092, "DCC electronic signature export is blocked by invalid evidence");
    ErrorCode CONTROLLED_FILE_SIGNATURE_DISABLED = new ErrorCode(1_080_000_093, "DCC electronic signature authorization is disabled");
    ErrorCode SIGNATURE_GOVERNANCE_OWNER_MISSING = new ErrorCode(1_080_000_094, "Signature governance owner is missing");
    ErrorCode SIGNATURE_GOVERNANCE_POLICY_SOURCE_MISSING = new ErrorCode(1_080_000_095, "Signature governance policy source is missing");
    ErrorCode SIGNATURE_GOVERNANCE_RETENTION_PRECHECK_FAILED = new ErrorCode(1_080_000_096, "Signature governance retention precheck failed");
    ErrorCode SIGNATURE_GOVERNANCE_RECOVERY_HASH_MISMATCH = new ErrorCode(1_080_000_097, "Signature governance recovery hash mismatch");
    ErrorCode SIGNATURE_GOVERNANCE_OPEN_REMEDIATION_EXISTS = new ErrorCode(1_080_000_098, "Signature governance open remediation exists");
    ErrorCode SIGNATURE_GOVERNANCE_CSV_APPROVAL_MISSING = new ErrorCode(1_080_000_099, "Signature governance CSV approval is missing");
    ErrorCode SIGNATURE_GOVERNANCE_MODULE_ADAPTER_MISSING = new ErrorCode(1_080_000_100, "Signature governance module adapter is missing");
    ErrorCode SIGNATURE_GOVERNANCE_AUDIT_PERSIST_FAILED = new ErrorCode(1_080_000_101, "Signature governance audit persist failed");
    ErrorCode SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION = new ErrorCode(1_080_000_102, "Signature governance blocked precondition");
    ErrorCode SIGNATURE_GOVERNANCE_ACTION_UNDEFINED = new ErrorCode(1_080_000_103, "Signature governance action is undefined");
    ErrorCode FILE_DIRECTORY_DELETE_CONFIRM_TEXT_INVALID = new ErrorCode(1_080_000_104,
            "Controlled file directory deletion requires exact PROD confirmation");
    ErrorCode FILE_DIRECTORY_DELETE_MASTER_OUT_OF_SCOPE = new ErrorCode(1_080_000_105,
            "Controlled file directory deletion blocked because a version chain crosses the target directory subtree");
    ErrorCode FILE_DIRECTORY_DELETE_INFRA_FILE_MISSING = new ErrorCode(1_080_000_106,
            "Controlled file directory deletion blocked because referenced upload file is missing");
    ErrorCode FILE_DIRECTORY_DELETE_INFRA_FILE_REFERENCED = new ErrorCode(1_080_000_107,
            "Controlled file directory deletion blocked because upload file is referenced outside the target subtree");
    ErrorCode CATEGORY_VIEW_MATRIX_EFFECTIVE_ACCESS_BLOCKED = new ErrorCode(1_080_000_108,
            "DCC view matrix effective access is blocked: {}");
    ErrorCode DCC_UPLOAD_SIZE_POLICY_MISSING = new ErrorCode(1_080_000_108, "DCC upload size policy is missing or invalid");
    ErrorCode DCC_UPLOAD_SIZE_EXCEEDED = new ErrorCode(1_080_000_109, "DCC upload size exceeds policy limit: fileSize={}, maxBytes={}");
    ErrorCode DCC_UPLOAD_SIZE_POLICY_INVALID = new ErrorCode(1_080_000_110, "DCC upload size policy is invalid: {}");
    ErrorCode CONTROLLED_FILE_VIEWER_TOKEN_CONFIG_MISSING = new ErrorCode(1_080_000_111, "DCC viewer token config is missing: {}");
    ErrorCode CONTROLLED_FILE_VIEWER_TOKEN_INVALID = new ErrorCode(1_080_000_112, "DCC viewer token is invalid");
    ErrorCode CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED = new ErrorCode(1_080_000_113, "DCC viewer token expired");
    ErrorCode CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH = new ErrorCode(1_080_000_114, "DCC viewer token context mismatch");
    ErrorCode CONTROLLED_FILE_UPLOAD_TICKET_INVALID = new ErrorCode(1_080_000_115, "DCC upload ticket is invalid");
    ErrorCode CONTROLLED_FILE_UPLOAD_SESSION_INVALID = new ErrorCode(1_080_000_116, "DCC upload session is invalid");
    ErrorCode CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT = new ErrorCode(1_080_000_198,
            "DCC upload slot already contains different content");
    ErrorCode DCC_DOWNLOAD_ENCRYPTION_CONTRACT_MISSING = new ErrorCode(1_080_000_117, "DCC download encryption contract is missing");
    ErrorCode DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID = new ErrorCode(1_080_000_118, "DCC download encryption evidence is invalid");
    ErrorCode DCC_DOWNLOAD_REQUEST_ID_REQUIRED = new ErrorCode(1_080_000_119, "DCC download request id is required");
    ErrorCode DCC_DOWNLOAD_REQUEST_ID_REUSED = new ErrorCode(1_080_000_120, "DCC download request id has already been used");
    ErrorCode DCC_DOWNLOAD_AUDIT_RECORD_FAILED = new ErrorCode(1_080_000_121, "DCC download audit record failed");
    ErrorCode DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING = new ErrorCode(1_080_000_122, "DCC download encryption config is missing or invalid: {}");
    ErrorCode FILE_DIRECTORY_DELETE_NAS_TRANSFER_ACTIVE = new ErrorCode(1_080_000_123,
            "Controlled file directory deletion blocked because NAS transfer task is still active");
    ErrorCode CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED = new ErrorCode(1_080_000_124,
            "Only doc control can update controlled file metadata");
    ErrorCode CONTROLLED_FILE_PRODUCT_NAME_RECOGNITION_CONFIG_MISSING = new ErrorCode(1_080_000_125,
            "DCC product name recognition Codex CLI config is missing: {}");
    ErrorCode CONTROLLED_FILE_PRODUCT_NAME_RECOGNITION_SOURCE_MISSING = new ErrorCode(1_080_000_126,
            "DCC product name recognition source file is missing");
    ErrorCode CONTROLLED_FILE_PRODUCT_NAME_RECOGNITION_FAILED = new ErrorCode(1_080_000_127,
            "DCC product name recognition failed: {}");
    ErrorCode CONTROLLED_FILE_PRODUCT_NAME_RECOGNITION_EMPTY = new ErrorCode(1_080_000_128,
            "DCC product name recognition returned empty product name");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING = new ErrorCode(1_080_000_130,
            "DCC project-code recognition Codex CLI config is missing: {}");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING = new ErrorCode(1_080_000_131,
            "DCC project-code recognition source file is missing");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED = new ErrorCode(1_080_000_132,
            "DCC project-code recognition failed: {}");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_EMPTY = new ErrorCode(1_080_000_133,
            "DCC project-code recognition returned no DCC basic-data match");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_NO_CANDIDATE = new ErrorCode(1_080_000_134,
            "DCC project-code recognition has no enabled DCC basic-data candidates");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE = new ErrorCode(1_080_000_135,
            "DCC project-code recognition returned an invalid DCC basic-data candidate");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS = new ErrorCode(1_080_000_136,
            "DCC project-code recognition is ambiguous: {}");
    ErrorCode CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_IN_PROGRESS = new ErrorCode(1_080_000_137,
            "DCC project-code recognition is already running for controlled file: {}");
    ErrorCode PROJECT_CODE_NOT_EXISTS = new ErrorCode(1_080_000_139, "DCC project code does not exist");
    ErrorCode PROJECT_CODE_DUPLICATE = new ErrorCode(1_080_000_140, "DCC project code already exists");
    ErrorCode PROJECT_CODE_DELETE_REFERENCED = new ErrorCode(1_080_000_141,
            "DCC project code cannot be deleted because DCC files still reference it");
    ErrorCode PROJECT_CODE_STATUS_INVALID = new ErrorCode(1_080_000_142, "DCC project code status is invalid");
    ErrorCode PRODUCT_ONBOARDING_NOT_EXISTS = new ErrorCode(1_080_000_191,
            "DCC product onboarding request does not exist");
    ErrorCode PRODUCT_ONBOARDING_DUPLICATE_PROJECT_CODE = new ErrorCode(1_080_000_192,
            "DCC product onboarding project code already exists or is pending");
    ErrorCode PRODUCT_ONBOARDING_STATUS_INVALID = new ErrorCode(1_080_000_193,
            "DCC product onboarding request status is invalid");
    ErrorCode PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID = new ErrorCode(1_080_000_194,
            "DCC product onboarding MDM product is invalid: {}");
    ErrorCode PRODUCT_ONBOARDING_REQUIRED_FIELD_MISSING = new ErrorCode(1_080_000_195,
            "DCC product onboarding required field is missing: {}");
    ErrorCode DCC_PRODUCT_CATALOG_ROW_KEY_INVALID = new ErrorCode(1_080_000_148,
            "DCC product catalog row key is invalid: {}");
    ErrorCode DCC_PRODUCT_CATALOG_DATA_SOURCE_INVALID = new ErrorCode(1_080_000_149,
            "DCC product catalog data source is invalid: {}");
    ErrorCode DCC_DATA_RELATION_TARGET_INVALID = new ErrorCode(1_080_000_150,
            "DCC data relation target is missing or identity mismatched");
    ErrorCode DCC_DATA_RELATION_CONFLICT = new ErrorCode(1_080_000_151,
            "DCC data relation already exists");
    ErrorCode FILE_CATEGORY_LIFECYCLE_STAGE_INVALID = new ErrorCode(1_080_000_147,
            "Controlled file category lifecycle stage is invalid: {}");
    ErrorCode FILE_CATEGORY_DELETE_CHILD_EXISTS = new ErrorCode(1_080_000_050, "Controlled file category cannot be deleted because child categories still exist");
    ErrorCode FILE_CATEGORY_DELETE_REFERENCED = new ErrorCode(1_080_000_051, "Controlled file category cannot be deleted because DCC files still reference it");
    ErrorCode FILE_CATEGORY_DELETE_RELATION_EXISTS = new ErrorCode(1_080_000_158,
            "文件类别存在审阅矩阵、查看矩阵、目录授权、分发、培训或上传策略等关联关系，请先清理关联关系后再删除");
    ErrorCode CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID = new ErrorCode(1_080_000_052, "Submitted controlled file directory is invalid");
    ErrorCode CONTROLLED_FILE_SUBMIT_DIRECTORY_NOT_LEAF = new ErrorCode(1_080_000_053, "Submitted controlled file directory must be a leaf directory");
    ErrorCode INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING = new ErrorCode(1_080_000_029, "IntAuth file category sync config is missing");
    ErrorCode INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID = new ErrorCode(1_080_000_030, "IntAuth file category sync response is invalid");
    ErrorCode INTAUTH_FILE_CATEGORY_SYNC_REQUEST_FAILED = new ErrorCode(1_080_000_031, "IntAuth file category sync request failed");
    ErrorCode INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS = new ErrorCode(1_080_000_032, "IntAuth file category sync found ambiguous local categories");
    ErrorCode INTAUTH_POSITION_SYNC_CONFIG_MISSING = new ErrorCode(1_080_000_033, "IntAuth position sync config is missing");
    ErrorCode INTAUTH_POSITION_SYNC_RESPONSE_INVALID = new ErrorCode(1_080_000_034, "IntAuth position sync response is invalid");
    ErrorCode INTAUTH_POSITION_SYNC_REQUEST_FAILED = new ErrorCode(1_080_000_035, "IntAuth position sync request failed");
    ErrorCode INTAUTH_POSITION_SYNC_AMBIGUOUS = new ErrorCode(1_080_000_036, "IntAuth position sync found ambiguous local positions");
    ErrorCode INTAUTH_DIRECTORY_IMPORT_CONFIG_MISSING = new ErrorCode(1_080_000_037, "IntAuth directory import config is missing");
    ErrorCode INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID = new ErrorCode(1_080_000_038, "IntAuth directory import source is invalid");
    ErrorCode INTAUTH_DIRECTORY_IMPORT_NOT_ALLOWED = new ErrorCode(1_080_000_039, "IntAuth directory import is not allowed after local directories exist");
    ErrorCode INTAUTH_POSITION_CREATE_REQUEST_FAILED = new ErrorCode(1_080_000_040, "IntAuth position create request failed");
    ErrorCode INTAUTH_POSITION_CREATE_RESPONSE_INVALID = new ErrorCode(1_080_000_041, "IntAuth position create response is invalid");
    ErrorCode CATEGORY_APPROVAL_MATRIX_SIGNOFF_EMPTY = new ErrorCode(1_080_000_042, "Category approval matrix requires at least one signoff position");
    ErrorCode CATEGORY_APPROVAL_MATRIX_APPROVAL_COUNT_INVALID = new ErrorCode(1_080_000_043, "Category approval matrix requires exactly two approval positions");
    ErrorCode CATEGORY_APPROVAL_MATRIX_DOC_CONTROL_POSITION_MISSING = new ErrorCode(1_080_000_044, "Category approval matrix requires the fixed DCC position 文控");
    ErrorCode CATEGORY_APPROVAL_MATRIX_POSITION_INACTIVE_OR_MISSING = new ErrorCode(1_080_000_045, "Category approval matrix references an inactive or missing DCC position");
    ErrorCode CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED = new ErrorCode(1_080_000_138,
            "Category approval matrix effective access preview is blocked: {}");
    ErrorCode CATEGORY_PERMISSION_RULE_MANUAL_REVIEW_APPROVE_FORBIDDEN = new ErrorCode(1_080_000_137,
            "Review and approve permission rules must be maintained in DCC review matrix");
    ErrorCode CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED = new ErrorCode(1_080_000_046, "Current user is not authorized for DCC electronic signature");
    ErrorCode CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING = new ErrorCode(1_080_000_151,
            "DCC electronic signature image is missing or not enabled");
    ErrorCode CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID = new ErrorCode(1_080_000_152,
            "签名图片内容无效，请上传可正常打开的 PNG/JPEG 图片");
    ErrorCode CONTROLLED_FILE_SIGNATURE_IMAGE_HASH_MISMATCH = new ErrorCode(1_080_000_153,
            "DCC electronic signature image hash verification failed");
    ErrorCode CONTROLLED_FILE_SIGNATURE_IMAGE_PERSIST_FAILED = new ErrorCode(1_080_000_154,
            "DCC electronic signature image persist failed");
    ErrorCode PROJECT_CODE_ASSOCIATED_FILE_NOT_EXISTS = new ErrorCode(1_080_000_155,
            "DCC project-code associated file does not exist or is not visible");
    ErrorCode PROJECT_CODE_ASSOCIATED_FILE_ALREADY_CATEGORIZED = new ErrorCode(1_080_000_156,
            "DCC project-code associated file is already categorized");
    ErrorCode PROJECT_CODE_ASSOCIATED_FILE_CONCURRENT_MODIFICATION = new ErrorCode(1_080_000_157,
            "DCC project-code associated file was modified concurrently");
    ErrorCode DCC_BROWSER_EXTENSION_BLACKLIST_INVALID = new ErrorCode(1_080_000_159,
            "DCC browser extension blacklist pattern is invalid: {}");
    ErrorCode PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID = new ErrorCode(1_080_000_160,
            "DCC project-code assignment file is outside the current project-code scope");
    ErrorCode PROJECT_CODE_ASSIGNMENT_ASSIGNEE_INVALID = new ErrorCode(1_080_000_161,
            "DCC project-code assignment assignee is invalid");
    ErrorCode PROJECT_CODE_ASSIGNMENT_NOT_EXISTS = new ErrorCode(1_080_000_162,
            "DCC project-code assignment does not exist");
    ErrorCode PROJECT_CODE_ASSIGNMENT_INACTIVE = new ErrorCode(1_080_000_163,
            "DCC project-code assignment is inactive");
    ErrorCode PROJECT_CODE_ASSIGNMENT_SCOPE_EMPTY = new ErrorCode(1_080_000_164,
            "DCC project-code assignment scope contains no files");
    ErrorCode PROJECT_CODE_ASSIGNMENT_AUDIT_PERSIST_FAILED = new ErrorCode(1_080_000_165,
            "DCC project-code assignment metadata audit persist failed");
    ErrorCode PROJECT_CODE_ASSIGNMENT_REVOKE_NOT_ALLOWED = new ErrorCode(1_080_000_166,
            "Only active DCC project-code assignments can be revoked");
    ErrorCode PROJECT_CODE_ASSIGNMENT_AUDIT_NOT_EXISTS = new ErrorCode(1_080_000_167,
            "DCC project-code assignment metadata audit does not exist");
    ErrorCode PROJECT_CODE_ASSIGNMENT_ASSIGNEE_PERMISSION_MISSING = new ErrorCode(1_080_000_168,
            "DCC project-code assignment assignee lacks the assignment execution menu permission");
    ErrorCode PROJECT_CODE_ASSIGNMENT_TARGET_PROJECT_MISMATCH = new ErrorCode(1_080_000_197,
            "DCC project-code assignment target project does not match requested project code");
    ErrorCode CONTROLLED_FILE_PERSONAL_PAGE_DISABLED = new ErrorCode(1_080_000_169,
            "DCC personal file page is retired; use controlled file browser");
    ErrorCode DCC_DMR_SHEET_ROOT_CONFIG_MISSING = new ErrorCode(1_080_000_170,
            "DCC DMR sheet root config is missing: {}");
    ErrorCode DCC_DMR_SHEET_ROOT_UNAVAILABLE = new ErrorCode(1_080_000_171,
            "DCC DMR sheet root is unavailable: {}");
    ErrorCode DCC_DMR_SHEET_CATEGORY_MISSING = new ErrorCode(1_080_000_172,
            "DCC DMR sheet root contains no category directories: {}");
    ErrorCode DCC_DMR_SHEET_EXPORT_FAILED = new ErrorCode(1_080_000_173,
            "DCC DMR sheet export failed: {}");
    ErrorCode SIGNATURE_GOVERNANCE_RECORD_NOT_EXISTS = new ErrorCode(1_080_000_174,
            "电子签名记录不存在: {}");
    ErrorCode SIGNATURE_GOVERNANCE_RECORD_PDF_EXPORT_FAILED = new ErrorCode(1_080_000_175,
            "电子签名 PDF 导出失败: {}");
    ErrorCode FILE_TYPE_TAXONOMY_NOT_EXISTS = new ErrorCode(1_080_000_180,
            "DCC file type taxonomy does not exist");
    ErrorCode FILE_TYPE_TAXONOMY_PARENT_NOT_EXISTS = new ErrorCode(1_080_000_181,
            "DCC file type taxonomy parent does not exist");
    ErrorCode FILE_TYPE_TAXONOMY_LEVEL_INVALID = new ErrorCode(1_080_000_182,
            "DCC file type taxonomy supports only level 1 to 5");
    ErrorCode FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING = new ErrorCode(1_080_000_183,
            "DCC file type taxonomy sibling name or code already exists");
    ErrorCode FILE_TYPE_TAXONOMY_INACTIVE = new ErrorCode(1_080_000_184,
            "DCC file type taxonomy path is inactive");
    ErrorCode FILE_TYPE_TAXONOMY_DELETE_CHILD_EXISTS = new ErrorCode(1_080_000_185,
            "DCC file type taxonomy cannot be deleted because child nodes still exist");
    ErrorCode FILE_TYPE_TAXONOMY_DELETE_REFERENCED = new ErrorCode(1_080_000_186,
            "DCC file type taxonomy cannot be deleted because DCC categories or files still reference it");
    ErrorCode FILE_TYPE_TAXONOMY_PARENT_CHANGE_FORBIDDEN = new ErrorCode(1_080_000_187,
            "DCC file type taxonomy parent cannot be changed");
    ErrorCode PROJECT_CODE_DISABLED = new ErrorCode(1_080_000_188, "DCC project code is disabled");
    ErrorCode CONTROLLED_FILE_PRINT_NOT_ALLOWED = new ErrorCode(1_080_000_189,
            "Current controlled file cannot be printed as a controlled copy");
    ErrorCode CONTROLLED_FILE_PRINT_REQUIRED_FIELD_MISSING = new ErrorCode(1_080_000_190,
            "Controlled print request is missing required print fields");
    ErrorCode CONTROLLED_FILE_APPROVER_POST_REQUIRED = new ErrorCode(1_080_000_199,
            "审批人未配置系统岗位");
    ErrorCode CONTROLLED_FILE_ROUTE_NOT_READY = new ErrorCode(1_080_000_200,
            "审批路线未就绪：{}");
    ErrorCode CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH = new ErrorCode(1_080_000_201,
            "审批路线快照与实际任务分配不一致");
    ErrorCode CONTROLLED_FILE_FINAL_APPROVAL_NOT_READY = new ErrorCode(1_080_000_202,
            "最终批准条件未就绪：{}");
    ErrorCode CONTROLLED_FILE_SIGNATURE_BINDING_FAILED = new ErrorCode(1_080_000_203,
            "签名证据受控副本绑定失败：{}");
    ErrorCode CONTROLLED_FILE_ALREADY_CHECKED_OUT = new ErrorCode(1_080_000_304,
            "受控文件已由用户 {} 检出，不能重复检出");
    ErrorCode CONTROLLED_FILE_NOT_CHECKED_OUT = new ErrorCode(1_080_000_305,
            "受控文件当前未检出");
    ErrorCode CONTROLLED_FILE_CHECKIN_NOT_OWNER = new ErrorCode(1_080_000_306,
            "受控文件已由用户 {} 检出，只有检出人可以检入");
    ErrorCode CONTROLLED_FILE_RELATED_FILE_INVALID = new ErrorCode(1_080_000_302,
            "关联文件必须属于当前 DCC 项目代码");
    ErrorCode CONTROLLED_FILE_RELATED_FILE_DUPLICATE = new ErrorCode(1_080_000_303,
            "关联文件不能重复选择");
    ErrorCode CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT = new ErrorCode(1_080_000_204,
            "正式源文件已由其它受控记录占用：sourceFileId={}");
    ErrorCode CONTROLLED_FILE_SOURCE_ISOLATION_FAILED = new ErrorCode(1_080_000_205,
            "正式源文件隔离失败：{}");
    ErrorCode CONTROLLED_FILE_SOURCE_MIGRATION_CONFLICT = new ErrorCode(1_080_000_206,
            "正式源文件历史迁移发生并发变更：controlledFileId={}");
    ErrorCode CONTROLLED_FILE_SIGNATURE_BINDING_MIGRATION_BLOCKED = new ErrorCode(1_080_000_207,
            "历史签名受控副本绑定迁移被阻止：{}");
    ErrorCode REGISTRATION_CERTIFICATE_NOT_EXISTS = new ErrorCode(1_080_000_208,
            "注册证不存在");
    ErrorCode REGISTRATION_CERTIFICATE_DRAFT_NOT_EXISTS = new ErrorCode(1_080_000_209,
            "注册证草稿不存在");
    ErrorCode REGISTRATION_CERTIFICATE_STATUS_INVALID = new ErrorCode(1_080_000_210,
            "注册证状态不合法");
    ErrorCode REGISTRATION_CERTIFICATE_VERSION_CONFLICT = new ErrorCode(1_080_000_211,
            "注册证版本已存在");
    ErrorCode REGISTRATION_CERTIFICATE_CURRENT_CONFLICT = new ErrorCode(1_080_000_212,
            "注册证已存在当前有效版本");
    ErrorCode REGISTRATION_CERTIFICATE_PENDING_CONFLICT = new ErrorCode(1_080_000_213,
            "注册证已存在待生效版本");
    ErrorCode REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID = new ErrorCode(1_080_000_214,
            "注册证生产方式不合法：是否委托生产和是否自行生产不能同时为否，委托生产为是时必须选择受托企业");
    ErrorCode REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH = new ErrorCode(1_080_000_215,
            "注册证受托生产关系投影与正式数据不一致");
    ErrorCode REGISTRATION_CERTIFICATE_FILE_CONFLICT = new ErrorCode(1_080_000_216,
            "注册证文件已绑定");
    ErrorCode REGISTRATION_CERTIFICATE_FORMAL_FACT_IMMUTABLE = new ErrorCode(1_080_000_217,
            "已正式化的注册证信息不可修改");
    ErrorCode REGISTRATION_CERTIFICATE_AUDIT_EVENT_KEY_REQUIRED = new ErrorCode(1_080_000_218,
            "注册证审计事件标识不能为空");
    ErrorCode REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT = new ErrorCode(1_080_000_219,
            "注册证审计事件已存在");
    ErrorCode REGISTRATION_CERTIFICATE_REVISION_CONFLICT = new ErrorCode(1_080_000_220,
            "注册证数据版本已变更，请刷新后重试");
    ErrorCode REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED = new ErrorCode(1_080_000_221,
            "注册证请求幂等键不能为空");
    ErrorCode REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT = new ErrorCode(1_080_000_222,
            "注册证幂等键已绑定其他请求内容");
    ErrorCode REGISTRATION_CERTIFICATE_TENANT_MISMATCH = new ErrorCode(1_080_000_223,
            "注册证不属于当前租户");
    ErrorCode REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED = new ErrorCode(1_080_000_224,
            "请选择公司名称");
    ErrorCode REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED = new ErrorCode(1_080_000_225,
            "当前账号无该公司注册证上传权限，请选择已授权公司");
    ErrorCode REGISTRATION_CERTIFICATE_PRODUCT_REQUIRED = new ErrorCode(1_080_000_226,
            "请输入产品名称");
    ErrorCode REGISTRATION_CERTIFICATE_PRODUCT_INVALID = new ErrorCode(1_080_000_227,
            "所选 DCC 项目代码绑定的产品不存在或已停用，请更换项目代码或维护产品状态");
    ErrorCode REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID = new ErrorCode(1_080_000_228,
            "DCC项目代码不存在或不合法");
    ErrorCode REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED = new ErrorCode(1_080_000_229,
            "DCC项目代码已停用");
    ErrorCode REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH = new ErrorCode(1_080_000_230,
            "DCC项目代码不属于当前租户");
    ErrorCode REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH = new ErrorCode(1_080_000_231,
            "DCC项目代码绑定的产品不合法");
    ErrorCode REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID = new ErrorCode(1_080_000_232,
            "注册证日期顺序不正确：首次获证日期不能晚于生效日期，生效日期必须早于有效期至");
    ErrorCode REGISTRATION_CERTIFICATE_FIRST_OBTAINED_DATE_INVALID = new ErrorCode(1_080_000_233,
            "首次获证日期不合法");
    ErrorCode REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID = new ErrorCode(1_080_000_234,
            "批准日期不合法");
    ErrorCode REGISTRATION_CERTIFICATE_FILE_REQUIRED = new ErrorCode(1_080_000_235,
            "请先选择注册证文件");
    ErrorCode REGISTRATION_CERTIFICATE_FILE_NOT_STAGED = new ErrorCode(1_080_000_236,
            "注册证文件尚未完成暂存");
    ErrorCode REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH = new ErrorCode(1_080_000_237,
            "注册证文件不属于当前租户");
    ErrorCode REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT = new ErrorCode(1_080_000_238,
            "注册证文件归属状态冲突");
    ErrorCode REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT = new ErrorCode(1_080_000_239,
            "注册证审批入库状态冲突，请刷新后重试");
    ErrorCode REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN = new ErrorCode(1_080_000_240,
            "注册证延续不允许修改当前字段");
    ErrorCode REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED = new ErrorCode(1_080_000_241,
            "注册证延续变更类别时，必须填写新注册证号和类别");
    ErrorCode REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT = new ErrorCode(1_080_000_242,
            "该注册证已有待审批或待生效的延续，请勿重复提交");
    ErrorCode REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT = new ErrorCode(1_080_000_243,
            "注册证延续基准版本已变更，请刷新后重试");
    ErrorCode REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT = new ErrorCode(1_080_000_244,
            "注册证生效基准版本已变更，请刷新后重试");
    ErrorCode REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE = new ErrorCode(1_080_000_245,
            "注册证生效重放记录不完整");
    ErrorCode REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT = new ErrorCode(1_080_000_246,
            "注册证生效事件冲突");
    ErrorCode REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED = new ErrorCode(1_080_000_247,
            "请提供注册证支持文件");
    ErrorCode REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID = new ErrorCode(1_080_000_248,
            "注册证支持文件状态不合法");
    ErrorCode REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REJECT_REASON_REQUIRED = new ErrorCode(1_080_000_249,
            "请填写注册证支持文件驳回原因");
    ErrorCode REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID = new ErrorCode(1_080_000_250,
            "注册证变更类型不合法");
    ErrorCode REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED = new ErrorCode(1_080_000_251,
            "请填写注册证变更内容");
    ErrorCode REGISTRATION_CERTIFICATE_CHANGE_VALUE_FORBIDDEN = new ErrorCode(1_080_000_252,
            "当前注册证变更类型不允许填写该内容");
    ErrorCode REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED = new ErrorCode(1_080_000_253,
            "注册证生产关系变更信息不能为空");
    ErrorCode REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT = new ErrorCode(1_080_000_254,
            "注册证变更历史冲突");
    ErrorCode REGISTRATION_CERTIFICATE_CHANGE_PENDING_CONFLICT = new ErrorCode(1_080_000_262,
            "该注册证已有待审批的变更，请审批完成后再提交");
    ErrorCode REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT = new ErrorCode(1_080_000_255,
            "注册证生命周期事件冲突");
    ErrorCode REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_SEQUENCE_CONFLICT = new ErrorCode(1_080_000_256,
            "注册证生命周期事件顺序冲突");
    ErrorCode REGISTRATION_CERTIFICATE_CANDIDATE_VOID_REASON_REQUIRED = new ErrorCode(1_080_000_257,
            "请填写注册证延续候选作废原因");
    ErrorCode REGISTRATION_CERTIFICATE_TOP_LEVEL_VOID_REASON_REQUIRED = new ErrorCode(1_080_000_258,
            "请填写注册证作废原因");
    ErrorCode REGISTRATION_CERTIFICATE_LIFECYCLE_SCHEMA_CONFLICT = new ErrorCode(1_080_000_259,
            "注册证生命周期数据结构不完整");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_CONFIG_REVISION_CONFLICT = new ErrorCode(1_080_000_260,
            "注册证提醒配置已被修改，请刷新后重试");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_CONFIG_TIME_INVALID = new ErrorCode(1_080_000_261,
            "注册证提醒配置时间不合法");
    ErrorCode REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT = new ErrorCode(1_080_000_262,
            "注册证提醒每日任务运行冲突");
    ErrorCode REGISTRATION_CERTIFICATE_DAILY_RUN_FAILED = new ErrorCode(1_080_000_263,
            "注册证提醒每日任务运行失败");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_OCCURRENCE_CONFLICT = new ErrorCode(1_080_000_264,
            "注册证提醒事件冲突");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_OCCURRENCE_STATUS_INVALID = new ErrorCode(1_080_000_265,
            "注册证提醒事件状态不合法");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_CONFLICT = new ErrorCode(1_080_000_266,
            "注册证提醒发送记录冲突");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_STATUS_INVALID = new ErrorCode(1_080_000_267,
            "注册证提醒发送状态不合法");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_DELIVERY_MESSAGE_ID_REQUIRED = new ErrorCode(1_080_000_268,
            "注册证提醒发送消息 ID 不能为空");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_REQUIRED = new ErrorCode(1_080_000_269,
            "注册证提醒接收人不能为空");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_THRESHOLD_INVALID = new ErrorCode(1_080_000_270,
            "注册证提醒阈值不合法");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_RECIPIENT_RESOLUTION_FAILED = new ErrorCode(1_080_000_271,
            "注册证提醒接收人解析失败");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_INVALID = new ErrorCode(1_080_000_272,
            "注册证提醒通知模板不合法");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_TEMPLATE_PARAM_MISSING = new ErrorCode(1_080_000_273,
            "注册证提醒通知模板参数缺失");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_NOTIFY_SEND_FAILED = new ErrorCode(1_080_000_274,
            "注册证提醒通知发送失败");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED = new ErrorCode(1_080_000_275,
            "未配置注册证提醒任务");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_RUN_RETRY_CONFLICT = new ErrorCode(1_080_000_276,
            "注册证提醒任务重试冲突");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_SUPPRESSION_CONFLICT = new ErrorCode(1_080_000_277,
            "注册证提醒抑制状态冲突");
    ErrorCode REGISTRATION_CERTIFICATE_EVENT_NOTIFICATION_SCOPE_UNAPPROVED = new ErrorCode(1_080_000_278,
            "注册证事件通知范围未获批准");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_SCHEMA_CONFLICT = new ErrorCode(1_080_000_279,
            "注册证提醒数据结构不完整");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_REQUEST_KEY_REQUIRED = new ErrorCode(1_080_000_280,
            "注册证访问申请幂等键不能为空");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT = new ErrorCode(1_080_000_281,
            "注册证访问申请冲突");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_REQUEST_TYPE_INVALID = new ErrorCode(1_080_000_282,
            "注册证访问申请类型不合法");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID = new ErrorCode(1_080_000_283,
            "注册证访问申请状态不合法");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_PROJECT_CODE_REQUIRED = new ErrorCode(1_080_000_284,
            "注册证下载申请必须关联有效的项目代码");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT = new ErrorCode(1_080_000_285,
            "注册证审批绑定状态异常，请刷新后重试");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_BPM_CANDIDATE_EMPTY = new ErrorCode(1_080_000_286,
            "未配置注册部经理审批人，请先配置注册证上传审批权限");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_APPROVAL_REJECT_REASON_REQUIRED = new ErrorCode(1_080_000_287,
            "请填写注册证访问申请驳回原因");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT = new ErrorCode(1_080_000_288,
            "注册证访问申请无法撤回，请刷新后重试");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_GRANT_CONFLICT = new ErrorCode(1_080_000_289,
            "注册证访问授权冲突");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID = new ErrorCode(1_080_000_290,
            "注册证访问授权状态不合法");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED = new ErrorCode(1_080_000_291,
            "注册证访问授权已过期");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED = new ErrorCode(1_080_000_292,
            "注册证访问授权已撤销");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID = new ErrorCode(1_080_000_293,
            "注册证访问授权范围不合法");
    ErrorCode REGISTRATION_CERTIFICATE_DOWNLOAD_CONSUMPTION_CONFLICT = new ErrorCode(1_080_000_294,
            "注册证下载记录冲突");
    ErrorCode REGISTRATION_CERTIFICATE_DOWNLOAD_ALREADY_CONSUMED = new ErrorCode(1_080_000_295,
            "注册证下载授权已使用");
    ErrorCode REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID = new ErrorCode(1_080_000_296,
            "注册证下载项目代码不合法");
    ErrorCode REGISTRATION_CERTIFICATE_FILE_DELIVERY_AUDIT_CONFLICT = new ErrorCode(1_080_000_297,
            "注册证文件交付审计记录冲突");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_AUDIT_CONFLICT = new ErrorCode(1_080_000_298,
            "注册证访问审计记录冲突");
    ErrorCode REGISTRATION_CERTIFICATE_ACCESS_SCHEMA_CONFLICT = new ErrorCode(1_080_000_299,
            "注册证访问数据结构不完整");
    ErrorCode REGISTRATION_CERTIFICATE_SORT_INVALID = new ErrorCode(1_080_000_300,
            "注册证列表排序参数不合法");
    ErrorCode REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID = new ErrorCode(1_080_000_301,
            "注册证提醒状态参数不合法");

}
