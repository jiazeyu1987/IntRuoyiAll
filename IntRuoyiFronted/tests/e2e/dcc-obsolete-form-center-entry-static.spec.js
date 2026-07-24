const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src/views/dcc/controlled-file/detail/index.vue'
)
const source = fs.readFileSync(detailPath, 'utf8')

const assertContains = (needle, message) => {
  if (!source.includes(needle)) {
    throw new Error(message)
  }
}

const assertNotContains = (needle, message) => {
  if (source.includes(needle)) {
    throw new Error(message)
  }
}

assertContains(
  'data-testid="dcc-obsolete-form-center-panel"',
  'DCC obsolete dialog must expose the form-center obsolete approval panel.'
)
assertContains(
  'ActionFormPanel',
  'DCC obsolete dialog must reuse the existing form-center ActionFormPanel.'
)
assertContains(
  "actionCode: 'OBSOLETE'",
  'DCC obsolete form-center context must submit the OBSOLETE action.'
)
assertContains(
  "objectType: 'CONTROLLED_FILE'",
  'DCC obsolete form-center context must target CONTROLLED_FILE.'
)
assertContains(
  'dccObsoleteFormCenterFormData',
  'DCC obsolete form-center submission must pass form data with the obsolete reason.'
)
assertNotContains(
  'obsoleteControlledFile,',
  'DCC detail page must not import the old direct obsolete API for the obsolete dialog.'
)
assertNotContains(
  'await obsoleteControlledFile(',
  'DCC detail obsolete submission must not bypass form-center instances.'
)
