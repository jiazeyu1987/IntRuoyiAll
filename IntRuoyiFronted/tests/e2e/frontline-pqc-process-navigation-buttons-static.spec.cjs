const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const extractFunctionBlock = (name) => {
  const start = source.indexOf('const ' + name + ' =')
  assert.ok(start >= 0, 'missing function: ' + name)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, 'missing function body: ' + name)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex + 1, index)
      }
    }
  }
  assert.fail('unterminated function: ' + name)
}

const extractStyleBlock = (selector) => {
  const start = source.indexOf(selector + ' {')
  assert.ok(start >= 0, 'missing style block: ' + selector)
  const openIndex = source.indexOf('{', start)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex + 1, index)
      }
    }
  }
  assert.fail('unterminated style block: ' + selector)
}

const pqcHeaderStart = source.indexOf('<header class="frontline-operator-top is-pqc">')
const pqcHeaderEnd = source.indexOf('</header>', pqcHeaderStart)
assert.ok(pqcHeaderStart >= 0 && pqcHeaderEnd > pqcHeaderStart, 'PQC top header must exist.')
const pqcHeader = source.slice(pqcHeaderStart, pqcHeaderEnd)

assert.match(
  pqcHeader,
  /data-pqc-process-nav-card/,
  'PQC process card must expose a stable navigation card selector.'
)
assert.match(
  pqcHeader,
  /data-pqc-process-previous[\s\S]*frontline-production-process-nav-icon is-previous/,
  'PQC process card must render the previous process arrow button.'
)
assert.match(
  pqcHeader,
  /data-pqc-process-next[\s\S]*frontline-production-process-nav-icon is-next/,
  'PQC process card must render the next process arrow button.'
)
assert.doesNotMatch(
  pqcHeader,
  />\s*(前一个|后一个|←|→)\s*</,
  'PQC process arrows must use the formal icon drawing, not visible text labels.'
)
assert.match(
  pqcHeader,
  /@click\.stop="handleNavigatePqcProcess\(-1\)"/,
  'PQC previous process button must call the adjacent navigation handler with -1.'
)
assert.match(
  pqcHeader,
  /@click\.stop="handleNavigatePqcProcess\(1\)"/,
  'PQC next process button must call the adjacent navigation handler with 1.'
)
assert.match(
  pqcHeader,
  /:disabled="isPqcProcessPreviousDisabled"/,
  'PQC previous process button must be disabled when no previous process exists or switching is blocked.'
)
assert.match(
  pqcHeader,
  /:disabled="isPqcProcessNextDisabled"/,
  'PQC next process button must be disabled when no next process exists or switching is blocked.'
)

for (const requiredToken of [
  'const switchablePqcProcessOptions = computed',
  'const selectedPqcProcessIndex = computed',
  'const previousPqcProcess = computed',
  'const nextPqcProcess = computed',
  'const isPqcProcessNavigationBlocked = computed',
  'const isPqcProcessPreviousDisabled = computed',
  'const isPqcProcessNextDisabled = computed',
  'const handleNavigatePqcProcess = async'
]) {
  assert.ok(source.includes(requiredToken), 'missing PQC process navigation token: ' + requiredToken)
}

const navigateBlock = extractFunctionBlock('handleNavigatePqcProcess')
assert.match(
  navigateBlock,
  /const targetProcess = direction < 0[\s\S]*previousPqcProcess\.value[\s\S]*nextPqcProcess\.value/,
  'PQC navigation handler must choose the adjacent formal PQC process from computed neighbors.'
)
assert.match(
  navigateBlock,
  /if \(!targetProcess \|\| isPqcProcessNavigationBlocked\.value\) \{[\s\S]*return[\s\S]*\}/,
  'PQC navigation handler must not switch when there is no adjacent process or interaction is blocked.'
)
assert.match(
  navigateBlock,
  /await handleSelectProcess\(targetProcess\)/,
  'PQC navigation handler must reuse the formal process selection workflow.'
)
assert.doesNotMatch(
  navigateBlock,
  /deviceState\.selectedProcess\s*=/,
  'PQC navigation handler must not mutate selectedProcess directly.'
)

assert.match(
  source,
  /\.frontline-operator-top[\s\S]*&\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*minmax\(480px,\s*1\.45fr\)\s+minmax\(360px,\s*1\.2fr\)\s+minmax\(140px,\s*0\.45fr\)\s+150px;/,
  'PQC top grid must widen the process card and shrink the employee card.'
)
assert.match(
  source,
  /\.frontline-operator-panel\.is-pqc-fullscreen \.frontline-operator-top\.is-pqc,[\s\S]*?\.frontline-operator-panel:fullscreen \.frontline-operator-top\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*minmax\(480px,\s*1\.45fr\)\s+minmax\(360px,\s*1\.2fr\)\s+minmax\(140px,\s*0\.45fr\)\s+150px;[\s\S]*gap:\s*12px/,
  'PQC fullscreen top grid must use the same widened process and narrowed employee proportions.'
)

const pqcNavCardStyle = extractStyleBlock('.frontline-pqc-process-nav-card')
for (const [pattern, message] of [
  [/grid-template-columns:\s*72px minmax\(0,\s*1fr\) 72px;/, 'PQC process nav card must reserve compact arrow tracks around a larger label area.'],
  [/gap:\s*10px;/, 'PQC process nav card must keep arrows close enough for the enlarged process label.'],
  [/padding:\s*10px 12px;/, 'PQC process nav card must stay compact inside the top bar.']
]) {
  assert.match(pqcNavCardStyle, pattern, message)
}

console.log('PASS: frontline PQC process navigation buttons static contract')
