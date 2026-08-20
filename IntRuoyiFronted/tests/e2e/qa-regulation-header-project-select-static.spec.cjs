const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const sourcePath = path.join(
  __dirname,
  '..',
  '..',
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'QaRegulationPage.vue'
)
const source = fs.readFileSync(sourcePath, 'utf8')

const headerStart = source.indexOf('<div class="qa-regulation-page__header">')
const loadErrorStart = source.indexOf('v-if="dccProjectCodeLoadError"', headerStart)
const headerSource =
  headerStart >= 0 && loadErrorStart > headerStart
    ? source.slice(headerStart, loadErrorStart)
    : ''

const readCssBlock = (selector) => {
  const selectorStart = source.indexOf(selector)
  const blockStart = selectorStart >= 0 ? source.indexOf('{', selectorStart) : -1
  const blockEnd = blockStart >= 0 ? source.indexOf('}', blockStart) : -1
  return blockStart >= 0 && blockEnd > blockStart ? source.slice(blockStart, blockEnd + 1) : ''
}

assert.match(
  headerSource,
  /qa-regulation-page__title[\s\S]*qa-regulation-page__project-form[\s\S]*aria-label="DCC 项目代码"[\s\S]*<el-tag/,
  'QA header must render title, project selector, and lifecycle status in that order.'
)

const headerCss = readCssBlock('.qa-regulation-page__header')
assert.match(headerCss, /align-items:\s*center/, 'QA header must vertically center its controls.')
assert.match(
  headerCss,
  /justify-content:\s*flex-start/,
  'QA header must keep the project selector directly after the title.'
)
assert.match(headerCss, /margin-bottom:\s*0/, 'QA header must not leave a blank row below it.')

const projectFormCss = readCssBlock('.qa-regulation-page__project-form')
assert.match(
  projectFormCss,
  /flex:\s*0 1 720px/,
  'QA project selector must occupy the bounded yellow-box width on desktop.'
)
assert.match(
  projectFormCss,
  /min-width:\s*280px/,
  'QA project selector must retain a usable desktop minimum width.'
)
assert.match(projectFormCss, /margin:\s*0/, 'QA project selector form must not add outer spacing.')

const statusCss = readCssBlock('.qa-regulation-page__header :deep(.el-tag)')
assert.match(
  statusCss,
  /margin-left:\s*0/,
  'QA lifecycle status must not push save, publish, import, or test reset buttons out of view.'
)
assert.match(statusCss, /flex-shrink:\s*0/, 'QA lifecycle status must not be compressed.')

assert.match(
  source,
  /@media\s*\(max-width:\s*1180px\)[\s\S]*\.qa-regulation-page__header\s*\{[\s\S]*flex-wrap:\s*wrap[\s\S]*\.qa-regulation-page__project-form\s*\{[\s\S]*order:\s*3[\s\S]*flex:\s*1 0 100%/,
  'QA project selector must wrap to a full-width row on narrower screens.'
)

console.log('PASS QA regulation header project selector static contract')
