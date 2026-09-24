const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..', '..', '..', '..')
const moduleRoot = path.join(repoRoot, 'IntRuoyiBackend/yudao-module-mes')
const requestVo = fs.readFileSync(
  path.join(
    moduleRoot,
    'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewCreateReqVO.java'
  ),
  'utf8'
)
const service = fs.readFileSync(
  path.join(
    moduleRoot,
    'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java'
  ),
  'utf8'
)
const signatureService = fs.readFileSync(
  path.join(
    moduleRoot,
    'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java'
  ),
  'utf8'
)

assert.match(requestVo, /@NotBlank\(message = "电子签名密码不能为空"\)\s+private String signaturePassword;/, '创建请求必须强制电子签名密码')
const createMethod = service.match(/public MesProEdhrNonconformanceReviewRespVO create\([\s\S]*?\n    }\n\n    @Override/)?.[0]
assert(createMethod, '创建服务方法必须存在')
assert.match(createMethod, /recordNonconformanceReviewCreateSignature/, '创建评审必须调用正式创建签名动作')
assert.match(createMethod, /recordReviewOperation\("NONCONFORMANCE_REVIEW_CREATE"[\s\S]*signatureId/, '创建操作事实必须关联签名 ID')
assert.match(signatureService, /ACTION_NONCONFORMANCE_REVIEW_CREATE/, '统一签名服务必须定义评审创建动作')

console.log('PASS: backend NCR create signature static contract')
