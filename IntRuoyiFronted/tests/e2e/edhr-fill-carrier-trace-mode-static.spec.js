const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const domainTraceDetailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr',
  'DomainTraceDetailPage.vue'
)

const detailSource = fs.readFileSync(detailPath, 'utf8')
const domainTraceDetailSource = fs.readFileSync(domainTraceDetailPath, 'utf8')
const executionFormPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr',
  'ExecutionPage.vue'
)
const executionFormSource = fs.readFileSync(executionFormPath, 'utf8')

assert(
  detailSource.includes('填写方式') &&
    detailSource.includes('resolveFillCarrierLabel') &&
    detailSource.includes('edhr-batch-detail__fill-carrier-option'),
  '批次详情必须把 recordCategory 展示为明确的填写方式，而不是模糊的未知类型。'
)

assert(
    detailSource.includes('FORM') &&
    detailSource.includes('RECORDBOOK') &&
    detailSource.includes('UNCONFIGURED') &&
    detailSource.includes("recordCategory === 'BATCH_RECORD'") &&
    detailSource.includes("recordCategory === 'INTERNAL_RECORD'"),
  '填写方式必须明确映射 BATCH_RECORD=FORM、INTERNAL_RECORD=RECORDBOOK、缺失=UNCONFIGURED。'
)

assert(
  detailSource.includes('resolveOpenProcessEvidenceLabel') &&
    detailSource.includes('打开表单') &&
    detailSource.includes('填写记录本') &&
    detailSource.includes('配置填写方式'),
  '打开工序入口必须按填写方式显示表单、记录本或未配置动作。'
)

assert(
  detailSource.includes('edhr-batch-detail__fill-carrier-control-wrap') &&
    detailSource.includes('edhr-batch-detail__fill-carrier-option') &&
    detailSource.includes("openPendingTaskByFillCarrier(task, 'FORM')") &&
    detailSource.includes("openPendingTaskByFillCarrier(task, 'RECORDBOOK')") &&
    detailSource.includes('批记录') &&
    detailSource.includes('记录本') &&
    detailSource.includes("if (fillCarrier === 'RECORDBOOK')") &&
    !detailSource.includes('启用') &&
    !detailSource.includes('未启用'),
  '填写方式控件必须放在工序列表卡片原未知位置，并使用批记录/记录本双选项直接进入对应填写入口。'
)

assert(
  detailSource.includes('resolveRecordCategoryByFillCarrier') &&
    detailSource.includes("if (fillCarrier === 'FORM') return 'BATCH_RECORD'") &&
    detailSource.includes("return 'INTERNAL_RECORD'") &&
    detailSource.includes('recordCategory: resolveRecordCategoryByFillCarrier(fillCarrier)') &&
    detailSource.includes('fillCarrier,'),
  '选择批记录或记录本时，跳转和追溯参数必须按用户当前选择覆盖 recordCategory/fillCarrier。'
)

assert(
  !detailSource.includes('<div class="edhr-batch-detail__rail-label">填写方式</div>') &&
    !detailSource.includes('填写方式：{{ resolveFillCarrierLabel(currentProcessFillCarrier) }}'),
  '填写方式控件不得继续放在右侧摘要或详情弹层里。'
)

assert(
  detailSource.includes('fillCarrier') &&
    detailSource.includes('buildSelectedProcessEvidenceQuery') &&
    detailSource.includes('RECORDBOOK_UNRESTRICTED_FILL_MODE') &&
    detailSource.includes("path: '/mes/pro/feedback/edhr-execution/form'") &&
    detailSource.includes("fillMode = RECORDBOOK_UNRESTRICTED_FILL_MODE") &&
    detailSource.includes("await handleOpenTask(row, 'RECORDBOOK')") &&
    !detailSource.includes('openRecordbookForSelectedProcess'),
  '记录本填写方式必须复用批次执行表单，并携带不受控填写模式，不能再进入独立记录本页。'
)

assert(
  executionFormSource.includes('RECORDBOOK_UNRESTRICTED_FILL_MODE') &&
    executionFormSource.includes('isRecordbookUnrestrictedMode') &&
    executionFormSource.includes('记录本不受控填写') &&
    executionFormSource.includes(':required="isFieldRequiredForCurrentMode(field)"') &&
    executionFormSource.includes('formRenderError.value || isRecordbookUnrestrictedMode.value') &&
    executionFormSource.includes('if (!isRecordbookUnrestrictedMode.value)') &&
    executionFormSource.includes('resolveRuleConstraintValidation(field, newValueJson)') &&
    executionFormSource.includes('saveEdhrFieldChanges') &&
    executionFormSource.includes('fieldAuditSignatureForm.password.trim()') &&
    executionFormSource.includes('submitForm.password.trim()') &&
    executionFormSource.includes('buildSignatureTimePayload'),
  '记录本不受控填写只放开字段/附件必填校验，必须保留字段审计保存、电子签名和提交签名证据。'
)

assert(
  detailSource.includes('currentProcessFillCarrier') &&
    detailSource.includes('selectedProcessEvidenceItems') &&
    detailSource.includes('domain-trace'),
  '主数据追溯入口必须使用当前工序填写方式上下文。'
)

assert(
  domainTraceDetailSource.includes('填写方式') &&
    domainTraceDetailSource.includes('resolveFillCarrierLabel') &&
    domainTraceDetailSource.includes('route.query.fillCarrier'),
  '主数据追溯详情必须展示从批次详情传入的填写方式。'
)

console.log('PASS: EDHR fill carrier trace mode static contract')
