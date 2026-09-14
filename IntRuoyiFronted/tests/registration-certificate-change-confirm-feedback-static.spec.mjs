import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const source = readFileSync(join(process.cwd(), 'src/views/dcc/registration-certificate/change/ChangeDialog.vue'), 'utf8').replace(/\r\n/g, '\n')

assert.match(source, /data-testid="registration-certificate-change-action-error"/, 'change dialog must expose a stable inline error area')
assert.match(source, /请选择变更批件批准日期/, 'missing approval date must have a visible business validation message')
assert.match(source, /const actionError = ref\(''\)/, 'change dialog must keep visible action error state')
assert.match(source, /actionError\.value = resolveRegistrationCertificateUserMessage\(error, '提交变更申请失败'\)/, 'submit failures must populate the inline action error')
assert.match(source, /v-if="actionError"[\s\S]*data-testid="registration-certificate-change-action-error"/, 'inline error area must render when confirmation fails')
assert.match(source, /const submit = async \(\) => \{[\s\S]*actionError\.value = ''[\s\S]*payload = buildChangePayload\(\)[\s\S]*saving\.value = true/, 'confirmation must validate before entering saving state')
assert.match(source, /payload = buildChangePayload\(\)[\s\S]*catch \(error\) \{[\s\S]*message\.error\(actionError\.value\)[\s\S]*return[\s\S]*saving\.value = true/, 'local validation failures must show feedback and stay in the dialog')
assert.match(source, /payload, `DCC-REG-CERT-CHANGE-\$\{generateUUID\(\)\}`/, 'confirmation must keep the formal change submission call')
assert.match(source, /catch \(error\) \{[\s\S]*message\.error\(actionError\.value\)[\s\S]*throw error/, 'confirmation failures must remain visible and observable')
assert.doesNotMatch(source, />作废证书<\/el-button>/, 'change dialog must not expose certificate void action')
assert.doesNotMatch(source, /voidRegistrationCertificate/, 'change dialog must not submit certificate void requests')
assert.doesNotMatch(source, /const voidCertificate = async/, 'change dialog must not keep the removed void handler')

console.log('PASS: registration certificate change confirm feedback static contract')
