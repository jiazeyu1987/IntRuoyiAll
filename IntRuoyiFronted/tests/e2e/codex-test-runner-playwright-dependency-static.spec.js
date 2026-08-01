const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')

assert.match(
  runner,
  /function resolveFrontendNodePath\(\)[\s\S]*FRONTEND_PROJECT_ROOT[\s\S]*node_modules[\s\S]*NODE_PATH/,
  'Runner 必须把前端 node_modules 加入 Codex 子任务环境，确保隔离工作目录里的临时 Playwright 脚本能解析依赖。'
)
assert.match(
  runner,
  /spawn\(command, commandArgs, \{[\s\S]*env:\s*\{[\s\S]*\.\.\.process\.env[\s\S]*NODE_PATH:\s*resolveFrontendNodePath\(\)[\s\S]*\}/,
  'Runner 启动 codex exec 时必须显式传入包含前端 node_modules 的 NODE_PATH。'
)
assert.match(
  runner,
  /function resolveBrowserExecutablePath\(\)[\s\S]*PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH[\s\S]*chrome\.exe[\s\S]*msedge\.exe/,
  'Runner 必须解析本机 Chrome/Edge 可执行文件，不能要求隔离脚本依赖 Playwright 浏览器缓存。'
)
assert.match(
  runner,
  /spawn\(command, commandArgs, \{[\s\S]*env:\s*\{[\s\S]*PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH:\s*resolveBrowserExecutablePath\(\)[\s\S]*\}/,
  'Runner 启动 codex exec 时必须把浏览器可执行文件路径传给子任务。'
)
assert.match(
  runner,
  /Playwright dependency note:[\s\S]*require\('playwright'\)[\s\S]*FRONTEND_PROJECT_ROOT/,
  'Runner prompt 必须提醒子任务隔离目录已通过 NODE_PATH 暴露前端 Playwright 依赖，避免重复探索仓库依赖。'
)
assert.match(
  runner,
  /Browser executable path:[\s\S]*\$\{resolveBrowserExecutablePath\(\)\}[\s\S]*chromium\.launch\(\{ executablePath:/,
  'Runner prompt 必须明确要求临时 Playwright 脚本使用传入的浏览器 executablePath。'
)
assert.match(
  runner,
  /function resolveNavigationHints\(task\)[\s\S]*工艺路线[\s\S]*\/mes\/pro\/route[\s\S]*history/i,
  'Runner 必须按测试项文本提供正式页面导航提示，工艺路线不能继续猜测 hash 路由。'
)
assert.match(
  runner,
  /Navigation hints:[\s\S]*\$\{resolveNavigationHints\(task\)\}/,
  'Runner prompt 必须把正式页面导航提示传给 Codex 子任务。'
)
assert.match(
  runner,
  /Element Plus dialog\/drawer footer action buttons[\s\S]*entire visible dialog\/drawer or page[\s\S]*not only the field form scope/,
  'Runner prompt 必须提醒子任务保存/确定等 footer 按钮可能不在字段表单 scope 内，必须在整个可见弹窗/抽屉或页面中定位。'
)
assert.match(
  runner,
  /page\.locator\('\.el-dialog__footer button, \.el-drawer__footer button'\)[\s\S]*filter\(\{ hasText: \/保\\\\s\*存\|确\\\\s\*定\|提\\\\s\*交\//,
  'Runner prompt 必须给出 Element Plus footer 保存按钮的确定性 Playwright selector。'
)
assert.match(
  runner,
  /custom footer rows[\s\S]*page\.locator\('button, \.el-button'\)[\s\S]*hasText: \/保\\\\s\*存\|确\\\\s\*定\|提\\\\s\*交\//,
  'Runner prompt 必须要求子任务在 Element Plus 标准 footer 缺失时继续定位可见自定义底部保存按钮。'
)
assert.ok(
  runner.includes('/保\\\\s*存|确\\\\s*定|提\\\\s*交/') &&
    runner.includes('Do not use locator.last() for save/confirm buttons') &&
    runner.includes('for (let i = 0; i < actionCount; i += 1)'),
  'Runner prompt 必须要求子任务使用可容忍中文空白的保存按钮正则，并禁止用 locator.last() 命中隐藏按钮。'
)
assert.ok(
  runner.includes('Form inputs inside add/edit dialogs must be scoped to the currently visible .el-dialog or .el-drawer') &&
    runner.includes('Do not fill background list filter inputs after opening a dialog or drawer') &&
    runner.includes('verify the dialog-scoped inputValue before clicking save'),
  'Runner prompt 必须要求子任务在新增/编辑弹窗内按可见弹窗作用域填字段，并保存前验证弹窗内 inputValue。'
)
assert.ok(
  runner.includes('After a successful save toast such as 新增成功 or 保存成功') &&
    runner.includes('close the still-open dialog or drawer before running a list search') &&
    runner.includes('Do not treat a still-open post-save dialog as a failed save') &&
    runner.includes('If a post-save close button becomes detached or unstable') &&
    runner.includes('return to the list and verify the saved row by quick-filter before marking the checkpoint BLOCKED'),
  'Runner prompt 必须要求子任务在保存成功但弹窗关闭按钮不稳定时先用列表命中确认保存结果，不得误报新增阻塞。'
)
assert.ok(
  runner.includes('Before clicking any list quick-filter 查询/搜索 button, assert that no .el-dialog, .el-drawer, or .el-overlay-dialog is still visible') &&
    runner.includes('If an Element Plus overlay is still visible, close it with the scoped footer 关闭/返回 button or header .el-dialog__headerbtn/.el-drawer__close-btn') &&
    runner.includes('wait for .el-dialog:visible, .el-drawer:visible, .el-overlay-dialog:visible to disappear before retrying the list query') &&
    runner.includes('If a quick-filter 查询 click is intercepted by .el-overlay-dialog, close the overlay and retry the same quick-filter query once'),
  'Runner prompt 必须要求列表查询前显式关闭 Element Plus 弹窗并等待遮罩消失，避免弹窗拦截 quick-filter 查询。'
)
assert.ok(
  runner.includes('Business confirmation dialogs may use verb-specific primary buttons') &&
    runner.includes('确\\\\s*认\\\\s*复\\\\s*制') &&
    runner.includes('search the current visible dialog or drawer before any background row buttons'),
  'Runner prompt 必须要求子任务识别“确认复制”等业务确认按钮，并优先在当前弹窗/抽屉内定位，避免误判按钮不可见。'
)
assert.ok(
  runner.includes('If a visible copy/edit dialog footer primary button is 确认复制, use the business action regex, not only /保存|确定|提交/') &&
    runner.includes("page.locator('.el-dialog__footer button, .el-drawer__footer button').filter({ hasText: /保\\\\s*存|确\\\\s*定|提\\\\s*交|确\\\\s*认\\\\s*复\\\\s*制|复\\\\s*制/ })") &&
    runner.includes('Never declare "No visible enabled dialog save action" while a visible enabled 确认复制 button exists in the current dialog footer'),
  'Runner prompt 必须要求复制弹窗 footer 的“确认复制”按业务按钮点击，不能只用保存/确定/提交正则导致误阻塞。'
)
assert.ok(
  runner.includes('For 工艺路线版本发布 fixed source route lookup') &&
    runner.includes('RT000028 / 球囊扩张压力泵') &&
    runner.includes('do not hardcode 按压式球囊扩充压力泵 as the only source route name'),
  'Runner prompt 必须要求版本发布节点也使用当前正式源路线 RT000028 / 球囊扩张压力泵，不能只硬编码旧名称。'
)
assert.ok(
  runner.includes('For 工艺路线版本发布 candidate cleanup') &&
    runner.includes('the visible cleanup action for a draft candidate row may be 删除草稿') &&
    runner.includes('scope the click to the row/card that contains 草稿 or 候选版本') &&
    runner.includes('Never report the candidate cleanup entry missing while a visible 删除草稿 action exists'),
  'Runner prompt 必须要求版本发布候选草稿用“删除草稿”收尾，不能只查找“取消候选”。'
)
assert.ok(
  runner.includes('For 工艺路线版本发布 candidate cleanup completed state') &&
    runner.includes('无打开候选') &&
    runner.includes('do not click 创建候选版本 during checkpoint 4 cleanup') &&
    runner.includes('close the version workspace and delete the temporary route') &&
    runner.includes('treat the candidate cleanup as already complete'),
  'Runner prompt 必须要求版本发布工作台显示“无打开候选”时视为候选清理已完成，不能继续找删除草稿或重新创建候选直到超时。'
)
assert.ok(
  runner.includes('For Element Plus link-button cleanup actions such as 删除草稿') &&
    runner.includes('include text descendants such as span and then climb to the closest button/.el-button/[role="button"]/a action element') &&
    runner.includes('wait up to 15 seconds for the action element to stop being [disabled], [aria-disabled="true"], .is-disabled, or .is-loading') &&
    runner.includes('If the page body contains 删除草稿 but the action never becomes enabled, return BLOCKED as visible but disabled/loading instead of entry missing'),
  'Runner prompt 必须要求版本发布删除草稿按可见文本子节点上溯真实按钮，并区分按钮禁用/加载与入口缺失。'
)
assert.ok(
  runner.includes('When an Element Plus message box is visible, click the primary action only inside .el-message-box:visible or .el-overlay-message-box:visible') &&
    runner.includes('Do not include background page buttons in the same locator while a message box is visible') &&
    runner.includes('Use .el-message-box__btns button or .el-overlay-message-box button filtered by 确定/确认/删除') &&
    runner.includes('If a click is intercepted by .el-overlay-message-box, re-scope to the visible message box and retry once'),
  'Runner prompt 必须要求 MessageBox 确认按钮限定在前景系统提示框内，不能用全局按钮匹配命中背景行按钮。'
)
assert.match(
  runner,
  /field-selector list filters[\s\S]*visible selected field[\s\S]*路线编码[\s\S]*TN-ROUTE-BASIC-001/,
  'Runner prompt 必须提醒子任务遇到字段选择器列表筛选时可按当前可见字段搜索，工艺路线固定样本应优先用路线编码。'
)
assert.ok(
  runner.includes('For 工艺路线复制绑定 fixed source route lookup') &&
    runner.includes('RT000028 / 球囊扩张压力泵') &&
    runner.includes('when the selected quick-filter field is 路线编码, fill RT000028, never 球囊扩张压力泵') &&
    runner.includes('when searching by source route name, first confirm the selected field is 路线名称'),
  'Runner prompt 必须要求复制绑定固定源路线使用正式编码 RT000028，不能把源路线名称填入路线编码字段。'
)
assert.ok(
  runner.includes('For list search forms with a left field selector and a right text input') &&
    runner.includes('fill the right text input, not the left selector input') &&
    runner.includes('do not fail merely because the left selector inputValue is truncated or empty'),
  'Runner prompt 必须要求子任务在字段选择器列表中填写右侧文本框，不能把左侧字段选择器当查询输入框。'
)
assert.ok(
    runner.includes('After deleting or resetting a route and re-running the quick-filter') &&
    runner.includes('judge absence only from visible table body rows') &&
    runner.includes("Do not search page.locator('body').innerText() for the route code/name because the quick-filter input still contains the submitted value") &&
    runner.includes('If no visible body row contains the fixed route code or name, treat No Data/empty table body as successful absence'),
  'Runner prompt 必须要求删除/复位后只用可见表格 body 行判断目标路线是否仍存在，不能把筛选输入框里的路线编码当成残留。'
)
assert.ok(
  runner.includes('The selected field and the submitted value must match') &&
    runner.includes('if the left selector is 路线编码, fill the route code') &&
    runner.includes('Only switch to 路线名称 when that option is visible') &&
    runner.includes('if 路线名称 is not visible, keep 路线编码 and search by route code instead of returning BLOCKED'),
  'Runner prompt 必须要求字段选择器与提交值一致，且路线名称选项不可见时不得阻塞。'
)
assert.ok(
  runner.includes('Scope quick-filter interactions to the visible .table-quick-filter or .unified-list-template__quick-filter') &&
    runner.includes('Do not scan all page .el-select controls when determining the quick-filter field') &&
    runner.includes('Read the selected field only from .table-quick-filter__field') &&
    runner.includes('fill the query text only inside .table-quick-filter__value') &&
    runner.includes('After switching 路线编码/路线名称, re-read .table-quick-filter__field before filling') &&
    runner.includes('Do not throw just because quick-filter option 路线名称 is unavailable') &&
    runner.includes('if the visible field is 路线名称, fill the route name, never the route code'),
  'Runner prompt 必须把 TableQuickFilter 定位限定到可见 quick-filter 容器，避免读取其它下拉导致字段和值错配。'
)
assert.ok(
  runner.includes('When the local login page appears') &&
    runner.includes('scope all login locators to .login-form') &&
    runner.includes('use the visible prefilled login form values or read VITE_APP_DEFAULT_LOGIN_* from the frontend .env files') &&
    runner.includes('If a tenant select exists, select/fill it from .login-form .el-select using VITE_APP_DEFAULT_LOGIN_TENANT first') &&
    runner.includes('.login-form input[placeholder="请输入用户名"]') &&
    runner.includes('.login-form input.el-input__inner:not([type="password"]):not([role="combobox"])') &&
    runner.includes('.login-form input[type="password"]') &&
    runner.includes("Never fill login by using page.locator('input:visible').first()") &&
    runner.includes("do not use locator.filter({ hasNot: page.locator('[type=\"password\"]') }) to exclude password fields") &&
    runner.includes('/admin-api/system/auth/login') &&
    runner.includes('/admin-api/system/auth/get-permission-info') &&
    runner.includes('After the URL leaves /login, explicitly navigate back to the target history route such as /mes/pro/route') &&
    runner.includes('do not assume the redirect parameter completed the target navigation') &&
    runner.includes('Do not require INT_RUOYI_E2E_USERNAME or INT_RUOYI_E2E_PASSWORD'),
  'Runner prompt 必须要求子任务使用 .login-form 精确登录定位、本机默认登录来源、登录接口成功等待和登录后重返目标路由。'
)
assert.ok(
  runner.includes('After login and after every direct navigation to a target history route such as /mes/pro/route') &&
    runner.includes('wait up to 60 seconds for either target business controls or a visible .login-form / /login URL before using list helpers') &&
    runner.includes('If login appears after an initial target-route navigation, perform the scoped local login, then navigate to the target route again') &&
    runner.includes('repeat this login-or-controls loop up to 2 times') &&
    runner.includes('Do not return "Already authenticated" only because .login-form was not visible in the first few seconds after route navigation') &&
    runner.includes('the Vue app may still be asynchronously redirecting to /login') &&
    runner.includes('require .table-quick-filter or .unified-list-template__quick-filter to be visible') &&
    runner.includes('require .el-table or .unified-list-template__table-shell to be visible') &&
    runner.includes('target controls still do not render after the second target-route navigation') &&
    runner.includes('do not call quickFilter() before this page-ready wait'),
  'Runner prompt 必须要求目标路由加载时处理异步登录重定向竞争，不能过早判断已登录后停在登录页。'
)
assert.match(
  runner,
  /query buttons may be labeled 查询 or 搜索[\s\S]*getByRole\('button', \{ name: \/查询\|搜索\/ \}\)/,
  'Runner prompt 必须提醒子任务列表查询按钮可能叫“查询”或“搜索”，并给出确定性 selector。'
)
assert.ok(
  runner.includes('Run the temporary Playwright script at most once before returning') &&
    runner.includes('If stdout contains raw JSON with checkpointResults, return that JSON verbatim immediately') &&
    runner.includes('Do not keep debugging, rerunning, or launching extra browsers after JSON is available'),
  'Runner prompt 必须要求子任务临时 Playwright 脚本一旦产出 checkpointResults JSON 就立即返回，不能继续自由调试直到 600 秒超时。'
)
assert.ok(
  runner.includes('The temporary Playwright script must enforce its own overall deadline') &&
    runner.includes('race the main browser flow against that deadline') &&
    runner.includes('always print checkpointResults JSON before the Codex child timeout') &&
    runner.includes('If the deadline is reached, close the browser and return BLOCKED checkpoints for unfinished items instead of letting codex exec hit 600000ms'),
  'Runner prompt 必须要求临时 Playwright 脚本自带全局截止时间并在超时前输出 BLOCKED JSON，不能等 Codex 子进程 600 秒硬超时。'
)
assert.ok(
  runner.includes('Before running the temporary Node.js Playwright script, run node --check <temporary-script-path>') &&
    runner.includes('This syntax check does not count as running the browser script') &&
    runner.includes('fix the generated script before browser launch instead of running invalid JavaScript') &&
    runner.includes('Generated scripts must avoid redeclaring const or let identifiers in the same function or block') &&
    runner.includes('Do not reuse names such as modal, dialog, rows, values, result, or button') &&
    runner.includes("If a syntax error says Identifier '<name>' has already been declared"),
  'Runner prompt 必须要求临时 Playwright 脚本执行前先 node --check，并禁止同作用域重复声明 const/let 变量。'
)
assert.ok(
  runner.includes('For generic 工艺路线 detail verification, use a visible 详情/查看 action only when that exact action exists') &&
    runner.includes('when the route list uses a 路线编码 column link as the only detail entry, click the route code link instead') &&
    runner.includes('operation-column 编辑 is production-config editing, not base detail'),
  'Runner prompt 必须区分工艺路线基础详情入口和操作列编辑入口，避免把候选版本编辑页当成详情页。'
)
assert.ok(
  runner.includes('For 工艺路线基础维护 detail verification on /mes/pro/route, the 路线编码 column link opens the RouteForm detail') &&
    runner.includes('Do not click operation-column 编辑 for this base detail check') &&
    runner.includes('Do not click 版本 for base detail verification') &&
    runner.includes('工艺路线候选版本快照不完整'),
  'Runner prompt 必须要求工艺路线基础维护用路线编码列打开基础详情，禁止把操作列编辑/版本当成基础详情入口。'
)
assert.ok(
  runner.includes('When filling Element Plus dialog fields, locate only .el-form-item containers for the exact label') &&
    runner.includes('do not search broad div, section, row, or column containers by hasText') &&
    runner.includes('For 名称, the container text must include 名称 and must not also include 编码 or 基础信息'),
  'Runner prompt 必须要求子任务按精确 .el-form-item 容器填弹窗字段，避免“名称”匹配到整个基础信息区域后填错输入框。'
)
assert.ok(
  runner.includes('For 工艺路线复制绑定 detail verification') &&
    runner.includes('click the visible 路线编码 link inside the copied row') &&
    runner.includes('the opened 工艺路线详情 dialog must show the copied route code and copied route name before checking tabs') &&
    runner.includes('placeholders such as 请输入编码 or 请输入名称 mean the wrong blank detail form was opened') &&
    runner.includes('close it and reopen from the visible copied row route-code link') &&
    runner.includes('do not fall back to 详情/查看 if the route-code link exists'),
  'Runner prompt 必须要求复制绑定详情校验先确认打开的是副本详情；空白占位弹窗必须关闭并从可见副本行的路线编码链接重开。'
)
assert.ok(
  runner.includes('For 工艺路线复制绑定 tab checks, do not declare 流转关系图 empty') &&
    runner.includes('the left/right detail sidebars say 请选择工序查看详情 or 点击左侧字段查看明细') &&
    runner.includes('rendered mostly with div nodes and CSS connectors, not necessarily canvas or svg') &&
    runner.includes('.route-flow-graph-designer__node') &&
    runner.includes('[data-flow-node="route-process"]') &&
    runner.includes('visible process node cards, connector lines, or the route name/current-version toolbar') &&
    runner.includes('no visible graph container, no route-process nodes/cards, no connector/flow canvas'),
  'Runner prompt 必须要求复制绑定流转关系图按真实 div 图谱节点/连线/版本标签判定可见，不能因 pane 文本短或侧栏提示为空而误判。'
)
assert.ok(
  runner.includes('The route-code detail entry is the actual Element Plus link button, not the surrounding .cell table container') &&
    runner.includes('Do not include .cell as a clickable route-code candidate') &&
    runner.includes('prefer button.el-button.is-link, .el-button.is-link, a, or [role=\"link\"] filtered by the exact copied route code') &&
    runner.includes('After clicking the code entry, assert that a 工艺路线详情 dialog opened') &&
    runner.includes('if no dialog opens, retry the real descendant link/button in the 路线编码 column before failing'),
  'Runner prompt 必须禁止把 Element Plus 表格 .cell 容器当成路线编码详情入口，必须点击真实 link/button 并确认详情弹窗打开。'
)
assert.ok(
  runner.includes('RouteForm detail values may be stored in Element Plus input values rather than dialog innerText') &&
    runner.includes('do not fail detail verification only because modal.innerText contains labels such as 编码 生成 名称') &&
    runner.includes('read the exact .el-form-item 编码 and 名称 inputValue') &&
    runner.includes('Do not read once and fail immediately when values are empty') &&
    runner.includes('/admin-api/mes/pro/route/get?id=') &&
    runner.includes('poll the exact form-item input values for up to 30 seconds') &&
    runner.includes('input.el-input__inner, input, or textarea') &&
    runner.includes('async function readRouteFormValue') &&
    runner.includes('input.evaluate(el => el.value || el.getAttribute') &&
    runner.includes('repeat the same 30-second value wait before failing') &&
    runner.includes('the opened dialog input values must match') &&
    runner.includes('placeholder-only or empty input values after this wait mean the detail data has not loaded or the wrong blank form is open') &&
    runner.includes('close the blank dialog, return to the list, search the fixed/copied route again, and reopen from the real route-code link button'),
  'Runner prompt 必须要求 RouteForm 详情等待 route/get 或轮询 input DOM value，不能单次读取为空就失败。'
)

console.log('PASS: Codex runner Playwright dependency static contract')
