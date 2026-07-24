import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const backlog = read('src/views/crm/backlog/index.vue')
const contractAudit = read('src/views/crm/backlog/components/ContractAuditList.vue')
const receivableAudit = read('src/views/crm/backlog/components/ReceivableAuditList.vue')

assert.doesNotMatch(
  backlog,
  /ContractAuditList|ReceivableAuditList|contractAudit|receivableAudit/,
  'CRM backlog must not mount private audit task lists after unified approval closeout'
)
assert.match(
  backlog,
  /\/approval-center\?moduleCode=BPM&viewType=TODO/,
  'CRM backlog audit entry must route users to the unified BPM approval center'
)
assert.match(
  `${contractAudit}\n${receivableAudit}`,
  /BpmProcessInstanceDetail/,
  'retired CRM audit list components may remain only as formal BPM detail references'
)

process.stdout.write('approval-center phase_final CRM retirement static contract passed\n')
