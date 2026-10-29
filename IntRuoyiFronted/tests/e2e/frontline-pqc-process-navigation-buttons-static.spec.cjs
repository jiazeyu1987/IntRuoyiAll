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
  'const filteredPqcProcessOptions = computed',
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
  /const selectedPqcProcessIndex = computed\(\(\) => \{[\s\S]*filteredPqcProcessOptions\.value\.findIndex/,
  'PQC process navigation must locate the current process in the selected-rule filtered process list.'
)
assert.match(
  source,
  /const previousPqcProcess = computed\(\(\) => \{[\s\S]*filteredPqcProcessOptions\.value\[selectedIndex - 1\]/,
  'PQC previous arrow must use the selected-rule filtered process list.'
)
assert.match(
  source,
  /const nextPqcProcess = computed\(\(\) => \{[\s\S]*filteredPqcProcessOptions\.value\[selectedIndex \+ 1\]/,
  'PQC next arrow must use the selected-rule filtered process list.'
)

assert.match(
  source,
  /\.frontline-operator-top[\s\S]*&\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1\.45fr\)\s+minmax\(0,\s*1\.55fr\)\s+minmax\(0,\s*0\.6fr\)\s+minmax\(160px,\s*0\.55fr\);/,
  'PQC top grid must follow the production order, process, employee and action proportions.'
)
assert.doesNotMatch(
  source,
  /\.frontline-operator-panel:fullscreen \.frontline-operator-top\.is-pqc\s*\{/,
  'PQC fullscreen must reuse the normal header grid rather than override its proportions.'
)

const pqcNavCardStyle = extractStyleBlock('.frontline-pqc-process-nav-card')
for (const [pattern, message] of [
  [/grid-template-columns:\s*clamp\(72px, 7\.29vw, 140px\) minmax\(0,\s*1fr\) clamp\(72px, 7\.29vw, 140px\);/, 'PQC process arrows must scale to production-sized tracks.'],
  [/gap:\s*12px;/, 'PQC process nav card must use production spacing.'],
  [/padding:\s*14px 16px;/, 'PQC process nav card must use production padding.']
]) {
  assert.match(pqcNavCardStyle, pattern, message)
}

console.log('PASS: frontline PQC process navigation buttons static contract')
