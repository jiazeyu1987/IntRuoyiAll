import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const repoRoot = process.cwd()
const pagePath = path.resolve(
  repoRoot,
  'src/views/dcc/registration-certificate/index/index.vue'
)
const apiPath = path.resolve(repoRoot, 'src/api/dcc/registrationCertificate/index.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const testTabMatch = pageSource.match(
  /<el-tab-pane\s+name="test"\s+label="注册测试">([\s\S]*?)<\/el-tab-pane>/
)
assert.ok(testTabMatch, '注册证页面必须新增“注册测试”页签。')

const testTab = testTabMatch[1]
assert.match(testTab, /data-testid="registration-certificate-test-tab"/)
assert.match(testTab, /data-testid="registration-certificate-business-date"/)
assert.match(testTab, /data-testid="registration-certificate-simulate-daily-run"/)
assert.match(testTab, /<el-date-picker[\s\S]*type="date"[\s\S]*value-format="YYYY-MM-DD"/)
assert.match(testTab, /<el-button[\s\S]*>\s*[\s\S]*模拟[\s\S]*<\/el-button>/)
assert.equal(
  (testTab.match(/<el-date-picker/g) || []).length,
  1,
  '注册测试页签只能有一个日期选择控件。'
)
assert.equal(
  (testTab.match(/<el-button/g) || []).length,
  1,
  '注册测试页签只能有一个模拟按钮。'
)
assert.equal(
  (testTab.match(/<el-input|<el-select|<el-upload/g) || []).length,
  0,
  '注册测试页签不得加入额外输入、下拉或上传控件。'
)

assert.match(
  pageSource,
  /simulateRegistrationCertificateBusinessTimeDailyRun/,
  '注册测试页签必须调用正式业务时间模拟 API。'
)
assert.match(
  apiSource,
  /simulateRegistrationCertificateBusinessTimeDailyRun/,
  '注册证 API 必须导出业务时间模拟方法。'
)
assert.match(
  apiSource,
  /url:\s*'\/dcc\/registration-certificates\/business-time\/simulate-daily-run'/,
  '业务时间模拟 API 路径必须稳定。'
)
assert.match(apiSource, /request\.post/, '业务时间模拟必须使用 POST。')

console.log('registration certificate business time simulation static contract: PASS')
