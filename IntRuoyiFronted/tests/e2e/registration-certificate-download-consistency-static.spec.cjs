const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')
const detailPage = fs.readFileSync(
  path.join(frontendRoot, 'src', 'views', 'dcc', 'registration-certificate', 'detail', 'index.vue'),
  'utf8'
)
const referenceService = fs.readFileSync(
  path.join(repoRoot, 'IntRuoyiBackend', 'yudao-module-dcc', 'src', 'main', 'java', 'cn', 'iocoder', 'yudao',
    'module', 'dcc', 'registrationcertificate', 'service', 'reference',
    'DccRegistrationCertificateFileReferenceServiceImpl.java'),
  'utf8'
)
const roleMigration = fs.readFileSync(
  path.join(repoRoot, 'IntRuoyiBackend', 'sql', 'mysql',
    '20260903_dcc_registration_certificate_download_role_permission.sql'),
  'utf8'
)

assert.match(detailPage,
  /item\.fileKind\s*===\s*'CHANGE_APPROVAL'\s*&&\s*item\.fileStatus\s*===\s*'BOUND'\s*&&\s*item\.changeStatus\s*===\s*'APPLIED'/u,
  '下载申请列表必须只包含已生效且已绑定的变更批件')
assert.match(detailPage,
  /\.filter\(isHistoryItemForCurrentDetailVersion\)/u,
  'OLD 注册证详情只能展示当前版本对应的变更履历，不能串到其它版本变更文件')
assert.match(detailPage,
  /item\.fileKind\s*!==\s*'REGISTRATION_CERTIFICATE'\s*&&\s*item\.fileKind\s*!==\s*'CHANGE_APPROVAL'/u,
  'OLD 注册证失效命名必须同时覆盖注册证主文件和变更批件文件')
assert.match(detailPage,
  /\$\{baseName\}_已失效\.\$\{extension\}/u,
  '前端补充失效标识时必须保留 _已失效 分隔符')
assert.match(referenceService, /OWNER_TYPE_CHANGE\s*=\s*"CHANGE"/u,
  '注册证文件引用服务必须识别变更文件所属类型')
assert.match(referenceService, /FILE_KIND_CHANGE_APPROVAL\s*=\s*"CHANGE_APPROVAL"/u,
  '注册证文件引用服务必须识别变更批件类型')
assert.match(referenceService, /status\s*=\s*'APPLIED'/u,
  '变更批件引用必须仅解析审批已通过的变更记录')
assert.match(roleMigration, /dcc_registration_certificate_approver/u,
  '下载权限迁移必须定位注册部经理角色')
assert.match(roleMigration, /dcc:registration-certificate:access-request:create/u,
  '注册部经理必须获得下载接口所需权限')

console.log('注册证三类文件下载一致性静态合同通过')
