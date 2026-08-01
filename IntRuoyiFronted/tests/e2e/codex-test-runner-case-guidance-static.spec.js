const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { pathToFileURL } = require('node:url')

const root = path.resolve(__dirname, '../..')
const runnerPath = path.join(root, 'scripts/codex-test-runner.mjs')
const guidancePath = path.join(root, 'scripts/codex-test-runner-guidance.mjs')
const runnerSource = fs.readFileSync(runnerPath, 'utf8')

assert.equal(
  fs.existsSync(guidancePath),
  true,
  'Runner 必须把节点专用提示词拆到独立 guidance 模块，避免所有节点共享整套工艺路线规则。'
)

const task = (caseName) => ({
  caseName,
  methodText: '',
  testDataText: '',
  checkpoints: []
})

;(async () => {
  const { resolveCaseSpecificGuidance } = await import(pathToFileURL(guidancePath).href)
  const basicGuidance = resolveCaseSpecificGuidance(task('工艺路线节点：基础维护'))
  const copyGuidance = resolveCaseSpecificGuidance(task('工艺路线节点：复制绑定'))
  const versionGuidance = resolveCaseSpecificGuidance(task('工艺路线节点：版本发布'))
  const statusGuidance = resolveCaseSpecificGuidance(task('工艺路线节点：状态删除'))
  const batchGuidance = resolveCaseSpecificGuidance(task('批记录节点：批次创建'))
  const scheduleGuidance = resolveCaseSpecificGuidance(task('智能排产节点：工单准入'))

  assert.match(basicGuidance, /For 工艺路线基础维护 checkpoint 1 reset/)
  assert.match(basicGuidance, /After clicking 新增工艺路线/)
  assert.doesNotMatch(basicGuidance, /For 工艺路线复制绑定 fixed source route lookup/)
  assert.doesNotMatch(basicGuidance, /For 工艺路线版本发布/)
  assert.doesNotMatch(basicGuidance, /For 工艺路线状态删除/)

  assert.match(copyGuidance, /For 工艺路线复制绑定 fixed source route lookup/)
  assert.match(copyGuidance, /For 工艺路线复制绑定 detail verification/)
  assert.doesNotMatch(copyGuidance, /For 工艺路线版本发布/)
  assert.doesNotMatch(copyGuidance, /For 工艺路线状态删除/)
  assert.doesNotMatch(copyGuidance, /For 工艺路线基础维护 checkpoint 1 reset/)

  assert.match(versionGuidance, /For 工艺路线版本发布 opening the version workspace/)
  assert.match(versionGuidance, /For 工艺路线版本发布 candidate cleanup completed state/)
  assert.doesNotMatch(versionGuidance, /For 工艺路线复制绑定 detail verification/)
  assert.doesNotMatch(versionGuidance, /For 工艺路线状态删除/)
  assert.doesNotMatch(versionGuidance, /For 工艺路线基础维护 checkpoint 1 reset/)

  assert.match(statusGuidance, /For 工艺路线状态删除 enable\/disable verification/)
  assert.doesNotMatch(statusGuidance, /For 工艺路线复制绑定 detail verification/)
  assert.doesNotMatch(statusGuidance, /For 工艺路线版本发布/)
  assert.doesNotMatch(statusGuidance, /For 工艺路线基础维护 checkpoint 1 reset/)

  assert.equal(batchGuidance, '', '批记录节点不得收到工艺路线专用提示词。')
  assert.equal(scheduleGuidance, '', '智能排产节点不得收到工艺路线专用提示词。')

  const buildPromptStart = runnerSource.indexOf('function buildPrompt(')
  const buildPromptEnd = runnerSource.indexOf('\nfunction taskText(', buildPromptStart)
  assert.notEqual(buildPromptStart, -1, 'Runner 必须保留 buildPrompt。')
  assert.notEqual(buildPromptEnd, -1, 'Runner 必须能定位 buildPrompt 结束边界。')
  const buildPromptSource = runnerSource.slice(buildPromptStart, buildPromptEnd)

  assert.match(
    runnerSource,
    /import\s+\{\s*resolveCaseSpecificGuidance\s*\}\s+from\s+'\.\/codex-test-runner-guidance\.mjs'/
  )
  assert.match(buildPromptSource, /\$\{resolveCaseSpecificGuidance\(task\)\}/)
  assert.doesNotMatch(buildPromptSource, /For 工艺路线复制绑定 fixed source route lookup/)
  assert.doesNotMatch(buildPromptSource, /For 工艺路线版本发布 opening the version workspace/)
  assert.doesNotMatch(buildPromptSource, /For 工艺路线状态删除 enable\/disable verification/)

  console.log('PASS: Codex runner case-specific guidance static contract')
})().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
