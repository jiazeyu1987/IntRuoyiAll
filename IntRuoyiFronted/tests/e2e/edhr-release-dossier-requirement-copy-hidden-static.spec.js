const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const component = readSource('src/views/Profile/components/EdhrReleaseDossierRequirementSetting.vue')
const template = component.split('<script setup')[0]
const realE2e = readSource('tests/e2e/edhr-release-dossier-requirement-setting-real.e2e.js')

const assertIncludes = (content, token, message) => {
  assert.ok(content.includes(token), message)
}

const assertNotIncludes = (content, token, message) => {
  assert.ok(!content.includes(token), message)
}

assertIncludes(template, 'eDHR 放行资料限制', '卡片标题必须保留。')
for (const label of ['来料检报告', '灭菌报告', '成品检报告', '成品检记录限制']) {
  assertIncludes(component, `label: '${label}'`, `开关标签必须保留：${label}`)
}
assertIncludes(template, '<el-switch', '四个配置开关必须保留。')

assertNotIncludes(
  template,
  '仅金手指用户可配置；开启后，对应特殊节点必须完成并保存 ADD 附件后才可放行。',
  '截图红框中的顶部辅助说明不应显示。'
)
assertNotIncludes(template, '默认关闭', '截图红框中的默认关闭标签不应显示。')
assertNotIncludes(template, 'item.description', '截图红框中的开关项说明不应显示。')
assertNotIncludes(template, '当前配置 hash', '截图红框中的配置 hash 不应显示。')
assertNotIncludes(template, 'edhr-release-dossier-requirement-setting__meta', '配置 hash 元信息节点不应渲染。')

assertIncludes(component, 'ElMessageBox.confirm', '开关保存前确认框必须保留。')
assertIncludes(component, 'updateEdhrReleaseDossierRequirementSetting', '保存接口调用必须保留。')
assertIncludes(component, 'getEdhrReleaseDossierRequirementSetting', '加载接口调用必须保留。')
assertIncludes(component, '接口保存失败', '保存失败错误提示必须保留。')

assertNotIncludes(
  realE2e,
  '页面必须展示当前配置 hash',
  '真实 E2E 不应继续等待已隐藏的配置 hash。'
)
assertIncludes(
  realE2e,
  '页面不应展示当前配置 hash',
  '真实 E2E 必须同步断言配置 hash 不再可见。'
)

console.log('PASS: eDHR release dossier requirement helper copy hidden static contract')
