const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const normalized = executionPage.replace(/\r\n/g, '\n')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

const railStart = normalized.indexOf('<aside class="edhr-fill-workspace__rail">')
const railEnd = normalized.indexOf('<main', railStart)
assert.ok(railStart >= 0 && railEnd > railStart, '必须能定位 eDHR 填写页左侧工具栏。')
const railTemplate = normalized.slice(railStart, railEnd)

for (const retainedControl of [
  '适应宽度',
  '适应高度',
  '填写辅助模式',
  '原表模式',
  '保存草稿',
  '提交执行',
  'toggleFillWorkspaceFullscreen'
]) {
  assertIncludes(executionPage, retainedControl, `删除软键盘后必须保留现有填写页控制：${retainedControl}`)
}

for (const removedRailToken of [
  'edhr-fill-workspace__soft-keyboard-section',
  'edhr-fill-workspace__soft-keyboard-trigger',
  'aria-label="打开软键盘"',
  '@click="openSoftKeyboard"',
  'edhr-fill-workspace__soft-keyboard-popover',
  'mdi:keyboard-outline',
  'data-soft-keyboard-action='
]) {
  assertNotIncludes(railTemplate, removedRailToken, `左侧工具栏不得继续渲染软键盘入口：${removedRailToken}`)
}

for (const removedSourceToken of [
  'softKeyboardVisible',
  'softKeyboardRows',
  'softKeyboardTarget',
  'SoftKeyboardEditableElement',
  'insertSoftKeyboardText',
  'handleSoftKeyboardBackspace',
  'openSoftKeyboard',
  'handleSoftKeyboardFocusIn',
  'edhr-fill-workspace__soft-keyboard'
]) {
  assertNotIncludes(executionPage, removedSourceToken, `软键盘实现代码必须删除：${removedSourceToken}`)
}

assertIncludes(executionPage, "document.addEventListener('fullscreenchange'", '必须保留填写页全屏状态监听。')
assertNotIncludes(executionPage, "document.addEventListener('focusin', handleSoftKeyboardFocusIn)", '不得保留软键盘焦点监听。')
assertNotIncludes(executionPage, "document.removeEventListener('focusin', handleSoftKeyboardFocusIn)", '不得保留软键盘焦点监听清理。')
assertNotIncludes(executionPage, 'catch {}', '删除软键盘不得引入吞异常。')
assertNotIncludes(executionPage, 'mockSoftKeyboard', '删除软键盘不得保留 mock 或占位成功。')

console.log('PASS: edhr soft keyboard removal static contract')
