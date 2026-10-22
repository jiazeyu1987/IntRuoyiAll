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

assert.match(requestVo, /兼容旧来源入口的电子签名密码；ACTIVE_ORDER 创建不需要/, '创建请求必须区分活跃订单新建和旧来源入口')
const createMethod = service.match(/public MesProEdhrNonconformanceReviewRespVO create\([\s\S]*?\n    }\n\n    @Override/)?.[0]
assert(createMethod, '创建服务方法必须存在')
assert.match(createMethod, /String signaturePassword = requireText\(reqVO\.getSignaturePassword\(\)/, '旧来源入口仍必须校验创建签名密码')
assert.match(createMethod, /createFromActiveOrder\(reqVO, reason\)/, '活跃订单入口必须走统一正式来源创建分支')
assert.match(signatureService, /ACTION_NONCONFORMANCE_REVIEW_CREATE/, '统一签名服务仍必须保留旧入口创建动作')

console.log('PASS: backend NCR create signature static contract')
