const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const componentPath = path.join(repoRoot, 'src/components/ReleaseInfoDock/ReleaseInfoDock.vue')

assert.equal(fs.existsSync(componentPath), true, 'missing ReleaseInfoDock component')

const component = fs.readFileSync(componentPath, 'utf8')
const template = component.slice(component.indexOf('<template>'), component.indexOf('</template>'))
const dockTemplate = template.slice(
  template.indexOf('<div class="release-info-dock"'),
  template.indexOf('<ElDialog')
)
const dialogTemplate = template.slice(template.indexOf('<ElDialog'), template.indexOf('</ElDialog>'))

assert.match(
  dockTemplate,
  /<button[\s\S]*class="release-info-dock__version"[\s\S]*@click="dialogVisible = true"[\s\S]*>\s*\{\{\s*statusText\s*\}\}\s*<\/button>/,
  '版本入口必须是一个可点击的版本号按钮，并打开变更说明弹窗'
)

for (const forbiddenText of ['运行版本', '版本信息', '查看变更']) {
  assert.equal(
    dockTemplate.includes(forbiddenText),
    false,
    `版本入口不应显示额外文字：${forbiddenText}`
  )
}

assert.doesNotMatch(
  template,
  /<ElButton[\s\S]*release-info-dock__button/,
  '版本入口不应保留单独的查看变更按钮'
)

assert.match(
  component,
  /const\s+gitChangeItems\s*=\s*computed\([\s\S]*slice\(0,\s*10\)/,
  '变更说明弹窗必须只取 Git 差异前 10 条'
)

assert.match(
  dialogTemplate,
  /Git 变更（最多 10 条）/,
  '变更说明弹窗必须明确只展示 Git 变更'
)

assert.match(
  dialogTemplate,
  /v-for="item in gitChangeItems"/,
  '变更说明弹窗必须渲染 gitChangeItems，而不是发布包元信息'
)

assert.match(
  dialogTemplate,
  /Git 变更未生成/,
  'Git 差异为空时必须显示明确空状态，不能回退到发布包元信息'
)

for (const forbiddenDialogText of ['版本号', '构建时间', '发布范围', '组件', '摘要', '变更项', '源码提交']) {
  assert.equal(
    dialogTemplate.includes(forbiddenDialogText),
    false,
    `变更说明弹窗不应显示旧发布元信息区块：${forbiddenDialogText}`
  )
}

assert.match(
  component,
  /\.release-info-dock__version\s*\{/,
  '版本号按钮必须有稳定样式类'
)

assert.doesNotMatch(
  component,
  /mock|placeholder data|默认成功|静默|吞异常|fallback|降级/i,
  '版本入口调整不得引入 mock、fallback、降级或静默错误'
)

console.log('PASS: release info dock version-only static contract')
