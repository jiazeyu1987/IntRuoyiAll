const assert = require('assert')
const fs = require('fs')
const path = require('path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(moduleRoot, '..', '..')
const read = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const detailModel = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderDetail.java'
)
const detailResp = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'controller',
  'admin',
  'pro',
  'processpool',
  'team',
  'vo',
  'MesTeamLeaderActiveOrderDetailRespVO.java'
)
const detailController = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'controller',
  'admin',
  'pro',
  'processpool',
  'team',
  'MesProcessPoolTeamLeaderController.java'
)
const detailService = read(
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderDetailServiceImpl.java'
)

assert.match(
  detailModel,
  /class PqcSubmissionDetail[\s\S]*private Integer qaProcessSort;/,
  '详情领域模型必须输出一线 PQC 工序显示序号。'
)
assert.match(
  detailResp,
  /class PqcSubmissionDetail[\s\S]*private Integer qaProcessSort;/,
  '详情响应 VO 必须输出一线 PQC 工序显示序号给前端。'
)
assert.match(
  detailController,
  /\.setQaProcessSort\(submission\.getQaProcessSort\(\)\)/,
  '控制器必须把 PQC 工序显示序号透传给前端。'
)
assert.match(
  detailService,
  /arrangePqcSubmissionDisplayOrder\(\s*activeOrder,[\s\S]*pqcSubmissionAccumulators\.values\(\)\)/,
  '详情服务必须在输出 PQC 提交前按一线 PQC 工序顺序整理提交。'
)
assert.match(
  detailService,
  /Objects\.equals\(activeOrder\.getQaRegulationVersionId\(\), task\.getRegulationVersionId\(\)\)/,
  '详情服务必须使用活跃订单产品 QA 版本识别一线 PQC 产品工序。'
)
assert.match(
  detailService,
  /productProcesses[\s\S]*commonPackagingProcesses[\s\S]*orderedProcesses\.addAll\(productProcesses\)[\s\S]*orderedProcesses\.addAll\(commonPackagingProcesses\)/,
  '模拟 PQC 提交必须先输出产品 QA 工序，再追加通用包装工序。'
)
assert.match(
  detailService,
  /productMaxSort[\s\S]*setQaProcessDisplaySort\(\+\+nextSort\)/,
  '通用包装 PQC 工序必须在产品工序最大序号后连续编号。'
)
assert.match(
  detailService,
  /\.setQaProcessName\(qaProcessDisplayName\)[\s\S]*\.setQaProcessSort\(qaProcessDisplaySort\)/,
  'PQC 提交详情必须使用显示名称和显示序号，避免模拟提交页按原始规程顺序展示。'
)

console.log('PASS: active order PQC submission process order static contract')
