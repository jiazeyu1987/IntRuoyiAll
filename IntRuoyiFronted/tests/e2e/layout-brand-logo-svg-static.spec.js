const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const svg = fs.readFileSync(path.join(repoRoot, 'src/assets/imgs/sidebar-brand-logo.svg'), 'utf8')

assert.match(svg, /<image\b/, '旧 SVG 资源也必须渲染 INT MEDICAL 图标，避免刷新缓存仍显示旧首页图标。')
assert.match(svg, /data:image\/png;base64,/, '旧 SVG 资源必须内嵌透明 PNG，避免外部资源加载失败。')
assert.doesNotMatch(svg, /fill="#009688"/, '旧 SVG 不得保留绿色首页图标颜色。')
assert.doesNotMatch(svg, /M512 128L128 447\.936V896/, '旧 SVG 不得保留首页房子路径。')

console.log('PASS: legacy SVG brand asset renders the transparent INT MEDICAL logo')
