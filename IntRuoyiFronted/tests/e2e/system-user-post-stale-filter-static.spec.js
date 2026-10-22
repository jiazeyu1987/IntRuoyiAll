const path = require('node:path')
const { spawnSync } = require('node:child_process')

// The former string contract required silently deleting assignments absent
// from enabled-only candidates. UM-04 explicitly retires that behavior.
// Keep the existing entry point, but execute the actual SFC/API regressions.
// This entry point does not claim browser E2E or actual Element Plus tag behavior.
const regression = path.resolve(__dirname, '../../scripts/system-user-post-retention.test.mjs')
const result = spawnSync(process.execPath, ['--test', regression], { stdio: 'inherit' })
if (result.error) throw result.error
if (result.signal) throw new Error(`User post retention regression interrupted by ${result.signal}`)
process.exitCode = result.status
