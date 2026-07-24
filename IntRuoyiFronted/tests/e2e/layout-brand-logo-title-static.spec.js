const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const readBinary = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath))

const logoComponent = readSource('src/layout/components/Logo/src/Logo.vue')
const rootCss = readSource('src/styles/var.css')
const appStore = readSource('src/store/modules/app.ts')
const setting = readSource('src/layout/components/Setting/src/Setting.vue')
const logoPng = readBinary('src/assets/imgs/sidebar-brand-logo.png')

assert.match(
  logoComponent,
  /src="@\/assets\/imgs\/sidebar-brand-logo\.png"/,
  '左上角品牌区必须使用 INT MEDICAL 透明 PNG。'
)
assert.match(logoComponent, /alt="瑛泰管理系统品牌图标"/, '品牌图标 alt 文案必须保留。')
assert.match(logoComponent, /ml-18px text-16px font-700/, '标题前必须保留约两个空格宽度。')
assert.doesNotMatch(
  logoComponent,
  /text-\[var\(--top-header-text-color\)\]/,
  '左上角品牌标题在所有布局下都必须使用 logo 主蓝，不得继续跟随顶部栏文字色。'
)
assert.doesNotMatch(
  logoComponent,
  /sidebar-brand-logo\.svg/,
  '左上角品牌区不得继续引用旧 home SVG 图标。'
)

assert.match(rootCss, /--logo-title-text-color:\s*#033886;/, '首屏标题色必须匹配 logo 主蓝。')
assert.match(appStore, /logoTitleTextColor:\s*'#033886'/, '默认主题标题色必须匹配 logo 主蓝。')
assert.match(
  setting,
  /logoTitleTextColor:\s*isDarkColor\s*\?\s*'#fff'\s*:\s*'#033886'/,
  '设置面板切换白色菜单主题时必须恢复 logo 主蓝标题色。'
)

assert.equal(logoPng.readUInt32BE(0), 0x89504e47, '品牌 logo 必须是 PNG 文件。')
assert.equal(logoPng.toString('ascii', 1, 4), 'PNG', '品牌 logo 必须包含 PNG 签名。')
assert.equal(logoPng.readUInt32BE(16), 80, '品牌 logo PNG 宽度必须是 80px。')
assert.equal(logoPng.readUInt32BE(20), 80, '品牌 logo PNG 高度必须是 80px。')
assert.equal(logoPng.readUInt8(25), 6, '品牌 logo PNG 必须是 RGBA，保留透明背景。')

console.log('PASS: layout brand logo and title match INT MEDICAL blue spacing contract')
