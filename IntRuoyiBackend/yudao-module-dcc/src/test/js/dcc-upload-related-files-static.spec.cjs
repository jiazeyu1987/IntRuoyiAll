const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../../../../..')
const backendRoot = path.join(root, 'IntRuoyiBackend')
const frontendRoot = path.join(root, 'IntRuoyiFronted')

const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const submitReq = read('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java')
assert.match(submitReq, /private\s+List<Long>\s+relatedControlledFileIds\s*;/,
  'submit request must expose relatedControlledFileIds for 0..n related DCC files')
assert.doesNotMatch(submitReq, /@NotEmpty[\s\S]{0,120}relatedControlledFileIds/,
  'relatedControlledFileIds must stay optional so uploads can have zero related files')

const respVO = read('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java')
assert.match(respVO, /private\s+List<DccControlledFileRelatedFileRespVO>\s+relatedFiles\s*;/,
  'controlled file response must return all selected related files')

const workflow = read('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java')
assert.match(workflow, /relatedFileService\.validateAndBindRelatedFiles\(/,
  'submit transaction must validate and bind related files after controlled file insert')
assert.match(workflow, /validateAndBindRelatedFiles\([\s\S]*?context\.projectCode\(\) == null \? null : context\.projectCode\(\)\.getId\(\)/,
  'binding must validate related files against the selected DCC project code id')

const queryService = read('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java')
assert.match(queryService, /setRelatedFiles\(relatedFileService\.listRelatedFiles\(/,
  'detail/list projection must include related file rows from the explicit relation table')

const schema = read('IntRuoyiBackend/sql/mysql/20260903_dcc_controlled_file_related_file.sql')
assert.match(schema, /CREATE TABLE IF NOT EXISTS `dcc_controlled_file_related_file`/,
  'migration must create the explicit upload related-file relation table')
assert.match(schema, /UNIQUE KEY `uk_dcc_related_file` \(`tenant_id`, `controlled_file_id`, `related_controlled_file_id`, `deleted`\)/,
  'relation table must prevent duplicate related file selections')
assert.match(schema, /KEY `idx_dcc_related_file_target` \(`tenant_id`, `related_controlled_file_id`, `deleted`\)/,
  'relation table must support reverse impact lookup')

const submitter = read('IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts')
assert.match(submitter, /relatedControlledFileIds:\s*\[\.\.\.\(draft\.relatedControlledFileIds \?\? \[\]\)\]/,
  'frontend submit payload must send selected related files and preserve an empty array')
assert.match(submitter, /EDITABLE_SOURCE_EXTENSIONS\s*=\s*\[[\s\S]*?'pdf'[\s\S]*?\]\s+as const/,
  'frontend DCC controlled file source whitelist must include pdf as a main upload file')
assert.match(submitter, /EDITABLE_SOURCE_EXT_PATTERN\s*=\s*\/\\\.\(doc\|docx\|xls\|xlsx\|pdf\|dwg\|sldprt\|sldasm\|slddrw\)\$\/i/,
  'frontend source file validation must accept pdf alongside Office and drawing source files')

const uploadPage = read('IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue')
assert.match(uploadPage, /data-testid="dcc-upload-related-files-select"/,
  'upload page must expose a related files multi-select')
assert.match(uploadPage, /accept="\.doc,\.docx,\.xls,\.xlsx,\.pdf,\.dwg,\.sldprt,\.sldasm,\.slddrw"/,
  'upload page file picker must expose pdf in the controlled file source accept list')
assert.match(uploadPage, /getProjectCodeControlledFilesPage\(/,
  'related files selector must load candidates from the selected DCC project code')
assert.match(uploadPage, /formData\.relatedControlledFileIds = \[\]/,
  'changing project code must clear related file selections')

const externalReviewPage = read('IntRuoyiFronted/src/views/dcc/controlled-file/external-review/index.vue')
assert.match(externalReviewPage, /accept="\.doc,\.docx,\.xls,\.xlsx,\.pdf,\.dwg,\.sldprt,\.sldasm,\.slddrw"/,
  'external review source picker must expose the same pdf-capable SOURCE accept list')

const typePolicy = read('IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadTypePolicy.java')
assert.match(typePolicy, /EDITABLE_SOURCE_EXTENSIONS\s*=\s*Set\.of\([\s\S]*?"pdf"[\s\S]*?\)/,
  'backend source upload whitelist must include pdf as a first-class controlled file source')
assert.match(typePolicy, /return "doc、docx、xls、xlsx、pdf、dwg、sldprt、sldasm、slddrw"/,
  'backend source upload validation message must list pdf as an allowed source extension')

console.log('DCC upload related files static contract passed')
