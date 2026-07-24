const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'yudao-ui-admin-vue3')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')

const readText = (filePath) => fs.readFileSync(filePath, 'utf8')

const sources = [
  {
    name: '排产员工作台页面',
    source: readText(path.join(frontendRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'))
  },
  {
    name: '排产员工作台 API',
    source: readText(path.join(frontendRoot, 'src/api/mes/pro/schedulerWorkbench/index.ts'))
  },
  {
    name: '首页快捷入口',
    source: readText(path.join(frontendRoot, 'src/views/mes/home/HomeShortcuts.vue'))
  },
  {
    name: '排产员工作台后端摘要',
    source: readText(
      path.join(
        backendRoot,
        'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchServiceImpl.java'
      )
    )
  }
]

const forbiddenPatterns = [
  { pattern: /\bEDHR\b/, label: 'EDHR' },
  { pattern: /\beDHR\b/, label: 'eDHR' },
  { pattern: /电子批记录/, label: '电子批记录' },
  { pattern: /批记录/, label: '批记录' },
  { pattern: /\/mes\/pro\/feedback\/edhr-/, label: 'EDHR 路由' }
]

for (const { name, source } of sources) {
  for (const { pattern, label } of forbiddenPatterns) {
    assert.doesNotMatch(source, pattern, `${name} 不得包含 ${label}，避免 EDHR 与智能排产边界混淆。`)
  }
}

console.log('PASS: scheduler and EDHR boundary static contract')
