const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const headerStart = qaSource.indexOf('<div class="qa-regulation-page__header">')
const loadErrorStart = qaSource.indexOf('v-if="dccProjectCodeLoadError"', headerStart)
const headerEnd = qaSource.lastIndexOf('</div>', loadErrorStart)
assert.ok(headerStart >= 0 && headerEnd > headerStart, 'QA header must exist.')

const header = qaSource.slice(headerStart, headerEnd)

assert.match(
  qaSource,
  /import \{ useClipboard \} from '@vueuse\/core'/,
  'QA page must use the project clipboard composable instead of a silent custom fallback.'
)
assert.match(
  qaSource,
  /getProjectCode,\s*[\s\S]*getProjectCodePage,/,
  'QA page must import the formal DCC project detail API for restoring the last project ID.'
)
assert.match(
  qaSource,
  /const QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY\s*=\s*'int-ruoyi:qa-regulation:last-dcc-project-code-id'/,
  'QA page must persist the last selected DCC project ID under a stable storage key.'
)
assert.match(
  qaSource,
  /window\.localStorage\.setItem\(\s*QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY,\s*String\(project\.id\)\s*\)/,
  'Selecting a formal project must persist only its formal project ID.'
)
assert.match(
  qaSource,
  /window\.localStorage\.removeItem\(QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY\)/,
  'Clearing the project selector must clear the remembered project ID.'
)
assert.match(
  qaSource,
  /window\.localStorage\.getItem\(\s*QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY\s*\)/,
  'Opening QA page must read the remembered DCC project ID.'
)
assert.match(
  qaSource,
  /const restoreLastDccProjectCodeSelection\s*=\s*async\s*\(\)\s*=>[\s\S]*await getProjectCode\(lastProjectId\)[\s\S]*project\.status !== DCC_PROJECT_CODE_STATUS_ENABLE[\s\S]*applyDccProjectToQaDraft\(project\)/,
  'Restoring the previous selection must load/validate a formal enabled project before applying it.'
)
assert.match(
  qaSource,
  /onMounted\(async\s*\(\)\s*=>\s*\{[\s\S]*await loadDccProjectCodeOptions\(\)[\s\S]*await restoreLastDccProjectCodeSelection\(\)[\s\S]*\}\)/,
  'QA page mount must load options and then restore the last selected project.'
)
assert.match(
  qaSource,
  /const selectedDccProjectCodeLabel\s*=\s*computed\(\(\)\s*=>[\s\S]*formatDccProjectCodeOption\(selectedDccProjectCode\.value\)/,
  'QA page must centralize the displayed selected project label for copy parity.'
)
assert.match(
  qaSource,
  /const \{ copy: copyQaProjectSelectionToClipboard \} = useClipboard\(\{ legacy: true \}\)/,
  'QA copy action must use the legacy clipboard path used elsewhere in the project.'
)
assert.match(
  qaSource,
  /await copyQaProjectSelectionToClipboard\(copyableProjectLabel\)[\s\S]*ElMessage\.success\('DCC 项目代码已复制'\)/,
  'QA copy action must write the selected label and show success only after the write.'
)
assert.match(
  qaSource,
  /ElMessage\.error\('DCC 项目代码复制失败，请检查浏览器剪贴板权限或浏览器限制。'\)[\s\S]*throw error/,
  'QA copy action must expose clipboard failures and rethrow them.'
)
assert.match(
  header,
  /data-qa-regulation-project-copyable/,
  'QA header project selector must expose a stable copyable control marker.'
)
assert.match(
  header,
  /data-qa-regulation-project-dropdown/,
  'QA header project selector must expose a stable dropdown-capable control marker.'
)
assert.match(
  header,
  /<el-select[\s\S]*automatic-dropdown[\s\S]*default-first-option[\s\S]*filterable[\s\S]*remote[\s\S]*remote-show-suffix[\s\S]*:remote-method="loadDccProjectCodeOptions"[\s\S]*<el-option[\s\S]*:label="formatDccProjectCodeOption\(project\)"[\s\S]*:value="project\.id"/,
  'The project code field must remain a searchable dropdown select, not a pure copyable input.'
)
assert.match(
  header,
  /data-qa-regulation-project-copy/,
  'QA header must provide a dedicated copy action inside the red-box project area.'
)
assert.match(
  header,
  /aria-label="复制 DCC 项目代码"/,
  'QA project copy action must be accessible.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__project-select\s*:deep\(\.el-select__selected-item\)[\s\S]*user-select:\s*text/,
  'Selected project text must remain user-selectable within the Element Plus selector.'
)

console.log('PASS qa-regulation-project-last-copy-static')
