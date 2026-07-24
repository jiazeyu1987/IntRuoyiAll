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
  /\.release-info-dock__version\s*\{/,
  '版本号按钮必须有稳定样式类'
)

assert.doesNotMatch(
  component,
  /mock|placeholder data|默认成功|静默|吞异常|fallback|降级/i,
  '版本入口调整不得引入 mock、fallback、降级或静默错误'
)

console.log('PASS: release info dock version-only static contract')
