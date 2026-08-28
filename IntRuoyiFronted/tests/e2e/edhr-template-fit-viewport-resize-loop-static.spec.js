const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const source = fs
  .readFileSync(
    path.join(root, 'src/views/mes/pro/edhr/components/EdhrTemplateFitViewport.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const syncStart = source.indexOf('const syncViewportScale = () => {')
const syncEnd = source.indexOf('\nconst scheduleViewportScale = () => {', syncStart)
assert.ok(syncStart >= 0 && syncEnd > syncStart, '必须定位模板缩放同步函数。')

const syncBlock = source.slice(syncStart, syncEnd)

assert.doesNotMatch(
  syncBlock,
  /contentSize\.value\s*=\s*\{\s*width:\s*nextWidth,\s*height:\s*nextHeight\s*\}/,
  '进入填写页后缩放测量不能每帧写入同尺寸 contentSize，否则 onUpdated + ResizeObserver 会形成持续重渲染。'
)

assert.doesNotMatch(
  syncBlock,
  /scale\.value\s*=\s*Number\.isFinite\(nextScale\)\s*&&\s*nextScale\s*>\s*0\s*\?\s*nextScale\s*:\s*1/,
  '进入填写页后缩放比例不能每次无条件写入，否则会放大 onUpdated 重算成本。'
)

assert.match(
  syncBlock,
  /const nextContentSize = \{ width: nextWidth, height: nextHeight \}/,
  '缩放测量必须先计算 nextContentSize，再比较是否真正变化。'
)

assert.match(
  syncBlock,
  /if\s*\(\s*contentSize\.value\.width !== nextContentSize\.width\s*\|\|\s*contentSize\.value\.height !== nextContentSize\.height\s*\)\s*\{\s*contentSize\.value = nextContentSize\s*\}/,
  'contentSize 只有尺寸变化时才允许写入。'
)

assert.match(
  syncBlock,
  /const normalizedScale = Number\.isFinite\(nextScale\) && nextScale > 0 \? nextScale : 1/,
  '缩放比例必须先归一化为 normalizedScale。'
)

assert.match(
  syncBlock,
  /if\s*\(\s*Math\.abs\(scale\.value - normalizedScale\) > 0\.0001\s*\)\s*\{\s*scale\.value = normalizedScale\s*\}/,
  'scale 只有实质变化时才允许写入。'
)

console.log('PASS: eDHR template fit viewport avoids resize/update loop')
