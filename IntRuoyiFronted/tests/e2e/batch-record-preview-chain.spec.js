const fs = require('fs')
const path = require('path')

const packageJsonPath = path.resolve(__dirname, '../../package.json')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))
if (packageJson.scripts['dev:batch-record-preview'] !== 'vite --mode batch-record-preview') {
  throw new Error('missing frontend preview worktree dev script')
}

const envPath = path.resolve(__dirname, '../../.env.batch-record-preview')
const envSource = fs.readFileSync(envPath, 'utf8')
if (!envSource.includes("VITE_BASE_URL='http://127.0.0.1:48082'")) {
  throw new Error('preview mode must point to backend 48082')
}
if (!envSource.includes('VITE_PORT=8082')) {
  throw new Error('preview mode must expose frontend on 8082')
}

const wrapperPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue'
)
const wrapperSource = fs.readFileSync(wrapperPath, 'utf8')
if (!wrapperSource.includes("data.path.includes('/jmreport/view/')")) {
  throw new Error('DesignerWrapper must detect jmreport view mode')
}
if (!wrapperSource.includes('电子批记录报表预览')) {
  throw new Error('DesignerWrapper must expose preview wording')
}

const reportPagePath = path.resolve(__dirname, '../../src/views/report/jmreport/index.vue')
const reportPageSource = fs.readFileSync(reportPagePath, 'utf8')
if (!reportPageSource.includes("data.path.includes('/jmreport/view/')")) {
  throw new Error('report management page must branch hint text on jmreport view mode')
}
if (!reportPageSource.includes('当前正在预览')) {
  throw new Error('report management page must expose preview hint text')
}

console.log('PASS: frontend preview chain source wiring is present')
