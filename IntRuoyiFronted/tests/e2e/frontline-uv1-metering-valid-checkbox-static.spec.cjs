const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repositoryRoot = path.resolve(frontendRoot, '..')
const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')
const readRepository = (relativePath) =>
  fs.readFileSync(path.join(repositoryRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const feedbackApi = readFrontend('src/api/mes/pro/feedback/index.ts')
const teamApi = readFrontend('src/api/mes/pro/processpool/teamLeader.ts')
const frontlinePanel = readFrontend('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const leaderPage = readFrontend('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const ruleDo = readRepository(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolDeviceParameterRuleDO.java'
)
const runtimeService = readRepository(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceImpl.java'
)
const frontlineValidator = readRepository(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesFrontlineDeviceParameterValidatorImpl.java'
)
const migrationRelativePath =
  'IntRuoyiBackend/sql/mysql/20260811_mes_process_pool_uv1_metering_valid_parameter.sql'
const migrationPath = path.join(repositoryRoot, migrationRelativePath)

assert.match(
  teamApi,
  /DeviceParameterValueType\s*=\s*[^\n]*'BOOLEAN'/,
  '工序设备参数正式类型必须包含 BOOLEAN。'
)
assert.match(
  feedbackApi,
  /FrontlineRuntimeDeviceParameterVO[\s\S]*valueType\?:\s*DeviceParameterValueType/,
  '一线运行态参数必须复用正式设备参数类型。'
)
assert.match(ruleDo, /VALUE_TYPE_BOOLEAN\s*=\s*"BOOLEAN"/, '后端 DO 必须定义 BOOLEAN 值类型。')
assert.match(
  runtimeService,
  /VALUE_TYPE_BOOLEAN[\s\S]*BigDecimal\.ZERO[\s\S]*BigDecimal\.ONE/,
  '后端保存校验必须只允许 BOOLEAN 默认值 0 或 1。'
)
assert.match(
  frontlineValidator,
  /VALUE_TYPE_BOOLEAN[\s\S]*BigDecimal\.ZERO[\s\S]*BigDecimal\.ONE/,
  '正式报工校验必须只接受 BOOLEAN 读数 0 或 1。'
)
assert.match(
  leaderPage,
  /<el-option\s+label="勾选"\s+value="BOOLEAN"\s*\/>/,
  '通用设备参数配置必须可识别勾选类型。'
)
assert.match(
  leaderPage,
  /data-team-leader-process-config-boolean-default/,
  'BOOLEAN 参数配置必须明确默认勾选状态。'
)
assert.match(frontlinePanel, /const isBooleanParameter =/, '一线页面必须识别 BOOLEAN 参数。')
assert.match(
  frontlinePanel,
  /type="checkbox"[\s\S]*data-frontline-boolean-parameter[\s\S]*updateProductionDeviceBooleanParameter/,
  'BOOLEAN 参数必须渲染为正式 checkbox 控件并更新当前设备草稿。'
)
assert.match(
  frontlinePanel,
  /const isNumericProductionParameter[\s\S]*!isBooleanParameter\(parameter\)/,
  'BOOLEAN 参数不得进入数值加减控件。'
)
assert.match(
  frontlinePanel,
  /if \(isBooleanParameter\(parameter\)\)[\s\S]*value:\s*booleanValue\s*\?\s*1\s*:\s*0/,
  '未选和已选状态必须分别提交为 0 和 1，不能丢弃未选读数。'
)
assert.match(
  frontlinePanel,
  /const resetProductionDeviceParameterDraft[\s\S]*syncProductionDeviceParameterDraft\(visibleDeviceCards\.value\)[\s\S]*const resetProductionSubmissionDraft[\s\S]*resetProductionDeviceParameterDraft\(\)/,
  '提交或手动重置后必须恢复 BOOLEAN 的正式默认值，不能留下类型缺失的未选状态。'
)
assert.doesNotMatch(
  frontlinePanel,
  /FRONTLINE_PRODUCTION_METERING_VALIDITY_DEVICE_CODES|productionMeteringValidityDraft/,
  '一线页面不得按设备编码硬编码计量效期 checkbox 或维护旁路草稿。'
)

assert.ok(fs.existsSync(migrationPath), '必须提供光固Ⅰ目标设备正式参数数据迁移。')
const migration = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')
assert.match(
  migration,
  /release-migration:[^\n]*dependsOn=20260810_mes_process_pool_device_parameter_select_options;\s*type=data;/,
  '数据迁移必须声明依赖和 data 类型。'
)
assert.match(migration, /SIGNAL SQLSTATE '45000'/, '迁移缺少正式前置时必须 fail fast。')
assert.match(migration, /光固Ⅰ/, '迁移必须限定光固Ⅰ工序。')
assert.match(migration, /A05075/, '迁移必须覆盖 A05075。')
assert.match(migration, /A05059/, '迁移必须覆盖 A05059。')
assert.match(
  migration,
  /INSERT INTO `mes_pro_process_pool_team_device`/,
  '迁移必须补齐缺失的 A05059 正式班组设备，不能要求它预先存在。'
)
assert.match(
  migration,
  /INSERT INTO `mes_pro_process_pool_team_process_device`/,
  '迁移必须把 A05059 绑定到光固Ⅰ正式工序，运行态才能返回第二台设备。'
)
assert.match(
  migration,
  /REPLACE\(source_rule\.`parameter_code`,\s*'A05075',\s*'A05059'\)/,
  '迁移必须从 A05075 的光固Ⅰ正式基础参数映射出 A05059 参数身份。'
)
assert.doesNotMatch(migration, /光固Ⅱ/, 'A05059 本次只能绑定光固Ⅰ，不能扩展到光固Ⅱ。')
assert.match(migration, /METERING_VALID/, '迁移必须使用稳定参数编码 METERING_VALID。')
assert.match(
  migration,
  /'在计量效期内'[\s\S]*'BOOLEAN'[\s\S]*0/,
  '目标规则必须保存 checkbox 名称、BOOLEAN 类型和未选默认值 0。'
)
assert.match(migration, /INSERT INTO `mes_pro_process_pool_device_parameter_rule`/, '迁移必须写入正式参数规则表。')
assert.match(migration, /NOT EXISTS/, '迁移必须按正式业务键幂等插入。')
assert.doesNotMatch(migration, /\bDELETE\b|\bTRUNCATE\b/i, '迁移不得删除业务参数规则。')

console.log('frontline-uv1-metering-valid-checkbox-static PASS')
