const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = process.cwd()
const componentPath = 'src/views/signature-governance/components/RetentionGovernanceListPane.vue'

const readComponentSource = () => {
  if (process.argv.includes('--fixture=head')) {
    return execFileSync('git', ['show', `HEAD:${componentPath}`], {
      cwd: root,
      encoding: 'utf8'
    })
  }
  return fs.readFileSync(path.join(root, componentPath), 'utf8')
}

test('long retention page does not auto-query eDHR archive candidate on visit', () => {
  const source = readComponentSource()
  const refreshBody = source.match(
    /const refreshRetentionSources = async \(\) => \{([\s\S]*?)\n\}/
  )?.[1] || ''
  const mountedBody = source.match(/onMounted\(\(\) => \{([\s\S]*?)\n\}\)/)?.[1] || ''

  assert.match(refreshBody, /loadRetentionStorageConfig\(\)/)
  assert.match(refreshBody, /loadDccSignatureCandidates\(\)/)
  assert.doesNotMatch(refreshBody, /loadEdhrArchiveCandidate\(\)/)
  assert.match(mountedBody, /refreshRetentionSources\(\)/)
  assert.doesNotMatch(mountedBody, /loadEdhrArchiveCandidate\(\)/)
  assert.match(source, /@click="loadEdhrArchiveCandidate"[\s\S]*>\s*归档\s*</)
})
