const fs = require('fs')
const path = require('path')
const assert = require('assert')

const projectRoot = path.resolve(__dirname, '..', '..', '..')
const servicePath = path.join(
  projectRoot,
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
const domainPath = path.join(
  projectRoot,
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
const voPath = path.join(
  projectRoot,
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
const controllerPath = path.join(
  projectRoot,
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

const service = fs.readFileSync(servicePath, 'utf8')
const domain = fs.readFileSync(domainPath, 'utf8')
const vo = fs.readFileSync(voPath, 'utf8')
const controller = fs.readFileSync(controllerPath, 'utf8')

const identityRecord = service.match(/private record PqcSubmissionIdentity\(([^)]*)\)/)
assert(identityRecord, '必须存在 PQC 提交聚合身份')
assert(
  /String\s+qaItemCode/.test(identityRecord[1]),
  'PQC 提交聚合身份必须包含 qaItemCode，避免复合同名项目被 inspectionRuleKey 合并'
)
assert(
  /task\.getQaItemCode\(\)/.test(service),
  '构建 PqcSubmissionIdentity 时必须使用任务冻结 qaItemCode'
)
assert(
  /\.setQaItemCode\(firstTask\.getQaItemCode\(\)\)/.test(service),
  '详情响应领域对象必须透传任务冻结 qaItemCode'
)

assert(/private String qaItemCode;/.test(domain), '领域详情 PqcSubmissionDetail 必须包含 qaItemCode')
assert(/private String qaItemCode;/.test(vo), '控制器响应 VO PqcSubmissionDetail 必须包含 qaItemCode')
assert(
  /\.setQaItemCode\(submission\.getQaItemCode\(\)\)/.test(controller),
  '控制器必须把领域详情 qaItemCode 映射到响应 VO'
)

console.log('PASS mes-active-order-pqc-composite-item-identity-static')
