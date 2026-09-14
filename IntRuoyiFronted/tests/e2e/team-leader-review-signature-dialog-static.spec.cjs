const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const readUtf8 = (...segments) => fs.readFileSync(path.join(...segments), 'utf8')

const page = readUtf8(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts')
const reviewReqVo = readUtf8(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderSubmissionReviewReqVO.java'
)
const allocationReqVo = readUtf8(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderReportAllocationConfirmReqVO.java'
)
const controller = readUtf8(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)

const reviewDialogStart = page.indexOf('<el-dialog v-model="reviewVisible"')
assert.ok(reviewDialogStart >= 0, '组长复核弹框必须存在')
const correctionDialogStart = page.indexOf('data-production-report-correction-dialog', reviewDialogStart)
assert.ok(correctionDialogStart > reviewDialogStart, '必须能定位组长复核弹框结束边界')
const reviewDialog = page.slice(reviewDialogStart, correctionDialogStart)

assert.match(
  reviewDialog,
  /label="电子签名"[\s\S]*data-team-leader-review-signature[\s\S]*v-model="reviewForm\.reviewSignaturePassword"[\s\S]*type="password"[\s\S]*show-password/,
  '复核弹框必须展示密码型电子签名输入，并绑定 reviewForm.reviewSignaturePassword'
)
assert.doesNotMatch(
  reviewDialog,
  /复核签名ID|签名员工ID|签名快照/,
  '复核弹框不得把内部签名 ID、签名员工 ID 或签名快照暴露给用户填写'
)

assert.match(page, /reviewSignaturePassword:\s*''/, 'reviewForm 必须声明复核签名密码字段')
assert.match(
  page,
  /reviewForm\.reviewSignaturePassword = ''/,
  '打开复核或分配弹框时必须清空旧电子签名密码'
)
assert.match(
  page,
  /const signaturePassword = reviewForm\.reviewSignaturePassword\.trim\(\)[\s\S]*if \(!signaturePassword\)[\s\S]*请输入电子签名密码/,
  '提交复核前必须校验电子签名密码不能为空'
)
assert.match(
  page,
  /const signaturePassword = reviewForm\.reviewSignaturePassword\.trim\(\)[\s\S]*return \{\s*signaturePassword\s*\}/,
  '复核提交载荷必须携带签名密码，由后端生成正式签名记录'
)

assert.match(
  api,
  /export interface TeamLeaderSubmissionReviewReqVO\s*\{[\s\S]*signaturePassword:\s*string/,
  '组长复核前端 API 请求类型必须包含 signaturePassword'
)
assert.doesNotMatch(
  api.match(/export interface TeamLeaderSubmissionReviewReqVO\s*\{[\s\S]*?\n\}/)?.[0] || '',
  /reviewSignatureId|reviewSignatureEmployeeUserId|reviewSignatureSnapshotJson/,
  '组长复核前端 API 请求类型不得继续要求客户端传内部签名字段'
)

assert.match(reviewReqVo, /@NotBlank\(message = "电子签名密码不能为空"\)[\s\S]*private String signaturePassword;/, '后端复核 VO 必须要求签名密码')
assert.doesNotMatch(
  reviewReqVo,
  /@NotNull[\s\S]*private Long reviewSignatureId|@NotNull[\s\S]*private Long reviewSignatureEmployeeUserId/,
  '后端复核 VO 不得继续要求客户端传复核签名 ID 或签名用户 ID'
)
assert.match(
  allocationReqVo,
  /private String signaturePassword;/,
  '生产组长复核+分配入口需要可选签名密码，纯分配入口仍可不传'
)
assert.match(
  controller,
  /\.signaturePassword\(reqVO\.getSignaturePassword\(\)\)/,
  'Controller 必须把签名密码传入服务层，由服务层记录正式电子签名'
)

console.log('PASS: team leader review signature dialog static contract')
