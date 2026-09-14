# Bug Regression Evidence

## Bug Summary

测试租户中的 3 条正式串行路线均无法完整跑完：两个首节点的 Codex 子进程退出码为 `1`，另一个首节点达到 Runner `600000ms` 超时。

## Expected Behavior

Runner 应在正式执行前验证 Codex CLI 可用性，并在真实页面发起后按节点串顺序完成全部节点、结构化回写检查点结果。

## Reproduction

- Runner 同构短预算 `codex exec` 自检当前可以成功，说明 stderr 前段的插件认证和旧 feature 信息本身不是致命错误。
- 上一轮写入型首节点从仓库根目录启动，未应用只读任务的受控推理与执行限制。
- 工艺路线首节点创建了 `doc/tasks/20260730-route-node-basic-maintenance-e2e/` 并执行 Git 基线提交；智能排产首节点创建了 `doc/tasks/20260730-smart-scheduling-workorder-admission-e2e/` 后达到 `600000ms` 超时。
- 批记录首节点要求的 `E:\IntRuoyi\resource\批记录节点-解析样本.docx` 不存在。
- 修复 Runner 隔离与固定样本后，从真实页面顺序执行 `工艺路线节点闭环` 创建批次 `37`；首节点不再触发仓库任务文档/Git 流程，但回写失败截图时被后端配置缺口阻断：`artifact 临时目录未配置`。
- 2026-07-31 复核发现 artifact 配置回归测试已存在，但 `application-local.yaml` 实际未包含对应本地配置；补齐配置后，静态断言和 Maven 定向测试均通过。
- 最新真实执行 `47` 证明 Codex 子任务已经开始运行临时 Playwright 脚本，但脚本依赖默认 Playwright 浏览器缓存；本机缓存 `chromium_headless_shell-1223` 不存在，导致首节点 `BLOCKED`，后续串行节点按前置失败阻断。
- 真实执行 `48` 证明浏览器 executablePath 阻塞已解除，但 Codex 临时脚本仍停在 `个人中心 / 个人工作台`；脚本只猜测了 `/#/mes/route` 等 hash 路由，未使用当前 Vue history 正式入口 `/mes/pro/route`。
- 真实执行 `67` 证明 `工艺路线节点：基础维护` 已完整通过，`工艺路线节点：复制绑定` 能完成副本复位、复制和清理，但详情校验打开了空白 `工艺路线详情` 弹窗，字段显示 `请输入编码/请输入名称` 占位且没有加载副本编码/名称。
- 真实执行 `68` 证明副本详情校验已能把问题归因为“详情未显示副本编码/名称”，但失败截图显示页面仍停留在副本列表；生成脚本点击了包裹文本的 `.cell` 表格容器，而不是触发 `openForm('detail', scope.row.id)` 的真实路线编码 `el-button link`。
- 真实执行 `69` 证明首节点新增后列表唯一命中和清理闭环仍可完成，但详情校验只读取 `modal.innerText()`，没有读取 Element Plus 表单输入框 `inputValue`，导致把已有标签的 RouteForm 详情误判为空白详情。
- 2026-08-01 诊断脚本复现副本详情真实卡点：列表 API 返回副本 `id` 且直接 `/mes/pro/route/get?id=...` 返回正确 code/name，但首次点击副本路线编码只打开父级 `工艺路线详情` 空表单，不触发详情请求；修复 RouteForm 异步内容组件等待后，同一真实页面点击触发 `route/get?id=922327` 且编码/名称 inputValue 正确。
- 真实执行 `71` 证明 `工艺路线节点：基础维护` 全 4 检查点继续 PASS，但 `工艺路线节点：复制绑定` 在第 1 检查点前 BLOCKED：生成脚本停留在应用 splash/loading spinner 后直接调用 quickFilter，报 `visible quick filter not found`；截图显示尚未进入业务列表页。
- 真实执行 `72` 证明 `.login-form` 和目标页 ready 约束已生效，生成脚本进入工艺路线列表并完成新增保存；但首节点第 2 检查点 BLOCKED：脚本点击路线编码后只等待弹窗出现，单次读取 RouteForm 编码/名称 inputValue 为 empty 就失败，未等待 `/admin-api/mes/pro/route/get?id=...` 或循环等待 Element Plus input DOM value。
- 真实执行 `76` 证明前序业务卡点已越过到临时脚本生成阶段，但生成脚本在浏览器启动前被 Node 语法解析阻断：`SyntaxError: Identifier 'modal' has already been declared`，位置为临时脚本 `int_ruoyi_route_basic_e2e.js:561`。同一函数 `verifyCreatedDetail()` 内先声明 `const modal`，后续又重复声明 `const modal` 用于页签校验，导致脚本未启动浏览器即失败。
- 真实执行 `77` 证明 `工艺路线节点：基础维护` 已全部 PASS；`工艺路线节点：复制绑定` 在进入工艺路线列表前 BLOCKED，actualText=`Route list controls did not render. URL=http://127.0.0.1:8081/login?redirect=/mes/pro/route ... 登录`。生成脚本在直接导航 `/mes/pro/route` 后只短暂检查 `.login-form`，过早返回“已登录或登录表单未显示”；随后 Vue 应用异步重定向到 `/login?redirect=/mes/pro/route`，导致脚本等待业务控件而不是执行登录。
- 真实执行 `78` 证明登录卡点已解除且 `工艺路线节点：基础维护` 全 PASS；`工艺路线节点：复制绑定` 完成副本复位、复制、详情打开和副本清理，但第 3 检查点 FAIL：`详情页签内容为空：流转关系图`。失败截图显示 `流转关系图` 页签中路线标题、当前版本、工序节点卡片、连线和工具栏均可见，脚本因为只查 `.el-tab-pane:visible` 文本/canvas/svg，没有识别 `RouteFlowGraphDesigner` 的 div 节点和 CSS 连接线。
- 真实执行 `80` 证明 `工艺路线节点：基础维护` 和 `工艺路线节点：复制绑定` 均已全检查点 PASS；`工艺路线节点：版本发布` 在复制生成版本测试路线时 BLOCKED：弹窗中编码/名称已正确填入，右下角 `确认复制` 主按钮可见，但生成脚本只按 `保存/确定/提交` 查找提交按钮，误报 `No visible enabled dialog save action`。
- 真实执行 `81` 证明 `工艺路线节点：版本发布` 已越过复制和候选创建，第 1、2、3 检查点 PASS；第 4 检查点 BLOCKED：版本工作台中 V2 行状态为 `草稿`，操作列可见 `编辑 / 查看断项 / 提交发布 / 删除草稿`，但生成脚本只找 `取消候选版本/取消候选/放弃候选/删除候选/作废候选/撤销候选`，误报候选版本取消入口缺失。
- 真实执行 `88` 证明本机重启后 Runner 可重新领取并进入工艺路线列表，`工艺路线节点：基础维护` 第 1 检查点 PASS；第 2 检查点 BLOCKED 于 `Unhandled error: form item not found for /编码|路线编码/; phase=checkpoint-2-add`。失败截图显示 `新增工艺路线` 弹窗壳已打开，但异步 `RouteFormContent` 表单项尚未渲染，生成脚本过早查找字段。
- 真实执行 `89` 证明新增弹窗字段缺失阻塞解除后，首节点退回到外层 Runner `600000ms` 硬超时。最新生成脚本 `route-basic-maintenance-e2e.js` 在执行开始后约 5 分钟才写入，且脚本自身设置 `Math.min(560000, 540000)`，无法在剩余外层预算内回传 `checkpointResults`。
- 真实执行 `90` 证明 `工艺路线节点：基础维护` 和 `工艺路线节点：复制绑定` 均全检查点 PASS，`工艺路线节点：版本发布` 第 1-3 检查点 PASS；第 4 检查点仍 BLOCKED，因为页面可见草稿候选和 `删除草稿` 操作，但生成脚本未能在 Element Plus 固定列拆分 DOM 中把可见 `删除草稿` 解析成可点击动作。
- 真实执行 `91` 证明最新登录卡点仍会让首节点全部 BLOCKED：页面停留在 `http://127.0.0.1:8081/login?redirect=/mes/pro/route`，生成脚本未覆盖登录页旧值/空密码，也没有在登录接口缺失或业务码非 `0` 时 fail-fast，而是继续等待工艺路线业务控件。
- 真实执行 `92` 证明登录与前两节点已恢复：`工艺路线节点：基础维护`、`工艺路线节点：复制绑定` 全检查点 PASS；`工艺路线节点：版本发布` 第 2 检查点 FAIL，页面实际显示 `V2 草稿`、`V1 已生效 ACTIVE`、`当前 ACTIVE`，但生成脚本只接受 `当前生效/生效版本/当前版本`，误报候选或当前生效说明缺失。
- 真实执行 `93` 证明 `工艺路线节点：基础维护`、`复制绑定`、`版本发布` 已全检查点 PASS，进入第 4 个 `状态删除` 节点后第 2 检查点 BLOCKED。页面固定路线已创建，状态列可见灰色 `el-switch`，操作列只有 `产品/编辑/复制/版本/删除`；生成脚本只找操作列文字 `停用/启用`，误报停用入口不可见。
- 真实执行 `94` 证明目标页实际上已经渲染筛选区、表格标题和数据行，但首节点仍全部 BLOCKED：生成脚本在 page-ready 判定中对未过滤的多匹配 locator 调用 `isVisible()`，命中隐藏副本后误报 `Target route controls did not render`。
- 真实执行 `102` 证明 `工艺路线节点：基础维护` 和 `工艺路线节点：复制绑定` 均全检查点 PASS；`工艺路线节点：版本发布` 第 1 检查点 PASS 后第 2 检查点 BLOCKED。页面已筛选到 `TN-ROUTE-VERSION-001` 行且操作列有 `版本`，但生成脚本全局点击 `/版\s*本/` 打开了页脚/全局 `版本变更说明 / 版本信息未生成` 覆盖层，导致真实 `route-version-workspace` 未打开。
- 真实执行 `103` 证明 `工艺路线节点：基础维护` 第 1-3 检查点 PASS，第 4 检查点 BLOCKED：脚本已解析到目标行 `删除` 操作的真实按钮 `BUTTON.el-button--danger.is-link`，但仍报告 `action handle unavailable`，没有直接点击该有效 Element Plus link-style danger button。
- 真实执行 `104` 证明上轮残留的 `TN-ROUTE-BASIC-001` 会在 checkpoint 1 复位阶段被命中；生成脚本没有先点击行内 `删除` 并确认到无结果，而是直接把“复位后仍有固定路线行”记为 FAIL，导致后续串行节点全部按前置失败阻断。
- 真实执行 `105` 证明 `工艺路线节点：基础维护` 和 `工艺路线节点：复制绑定` 均全检查点 PASS，版本发布已进入行内版本弹窗但第 2 检查点 FAIL。失败截图显示 `工艺路线版本` 弹窗正文已渲染，包含 `当前 ACTIVE：V1`、`创建候选版本` 和 `候选版本工作区`；生成脚本却把工作台文本读成 `版本`，因为它把列表行内 `data-testid="route-version-workspace"` 的 `版本` 按钮当成已打开的工作台容器。
- 真实执行 `106` 证明版本工作台定位修复后，`工艺路线节点：基础维护` 全 PASS，但 `工艺路线节点：复制绑定` 第 3 检查点 BLOCKED：副本详情校验报 `matchedRow.locator is not a function`。生成脚本中的表格行 helper 返回 `{ row, text, index }` 包装对象，后续却直接调用 `matchedRow.locator(...)`，没有使用 `matchedRow.row.locator(...)`。
- 真实执行 `107` 证明 `工艺路线节点：基础维护`、`复制绑定`、`版本发布` 三个节点均全检查点 PASS；第 4 个 `状态删除` 节点第 2 检查点 FAIL。失败截图显示启用时真实页面 toast 为 `请先添加组成工序`，说明生成脚本用新增空白路线做启停校验，违反工艺路线启用必须有组成工序的正式业务前置。
- 真实执行 `108` 证明 `工艺路线节点：基础维护`、`复制绑定` 全检查点 PASS；`版本发布` 在复制保存后第 2 检查点 BLOCKED。失败截图显示 quick-filter 输入已是 `TN-ROUTE-VERSION-001`，但表格仍在 loading 且可见旧的源路线 `RT000028` 行，生成脚本过早读取旧 body row 并误判复制后的测试路线未命中。

## Root Cause

1. Runner 只对识别为只读的测试项追加受控推理和最短路径约束；写入型业务页面测试继承仓库开发规则与用户级 `xhigh`，被引导执行建档、Git 和工程流程，而不是直接完成业务 UI 测试。
2. Codex 子进程工作目录是仓库根目录，进一步触发项目开发规则。
3. 非零退出错误从 stderr 头部截断，已知非致命 warning 覆盖了真实尾部错误。
4. 批记录解析节点还缺少正式固定 Word 样本，属于独立前置缺口。
5. 本地后端 `application-local.yaml` 配置了 Codex Runner 启动参数，但未配置 `yudao.codex-test.artifact-temp-dir`；当 Codex 返回失败截图 `screenshotPath` 时，Runner artifact 上传接口按设计 fail-fast，导致串行路线首节点 `BLOCKED`。
6. Runner 只把前端 `node_modules` 传给隔离 Codex 子任务，没有同时传入本机正式 Chrome/Edge executablePath，也没有在 prompt 中要求临时 Playwright 脚本显式使用该路径；因此子任务在缺少 Playwright 浏览器缓存的本机上仍会调用默认缓存浏览器并失败。
7. Runner prompt 没有把当前前端 history 路由和正式页面路径传给自然语言子任务；Codex 生成的脚本沿用 hash 路由猜测，导致真实页面导航失败后首节点 `BLOCKED`。
8. Runner prompt 没有要求 Codex 子任务在临时 Playwright 脚本输出 `checkpointResults` 后立即返回；子任务继续自由调试，导致 Runner 边界达到 `600000ms` 超时。
9. Runner prompt 没有限定 Element Plus 弹窗字段填充必须匹配精确 `.el-form-item` 标签，导致 `名称` 匹配到包含 `编码/名称` 的大块 `基础信息` 容器并填错输入框。
10. Runner prompt 没有要求 `TableQuickFilter` 读取和填写必须限定在可见 `.table-quick-filter` / `.unified-list-template__quick-filter` 容器内，导致 post-save 验证把 `路线名称` 字段与 route code 值错配，列表返回 `No Data`。
11. Runner prompt 没有区分 `工艺路线` 列表的基础详情入口和候选版本入口；`路线编码` 列链接才是 RouteForm 基础详情，操作列 `编辑` 是生产配置候选版本编辑，`版本` 是版本工作区。基础维护节点误用候选版本链路后触发 `工艺路线候选版本快照不完整`。
12. Runner prompt 没有强制列表 quick-filter 查询前关闭仍可见的 Element Plus 弹窗/抽屉/overlay；保存后仍停留 `新增工艺路线` 弹窗时，脚本点击背景查询按钮被 `.el-overlay-dialog` 拦截。
13. Runner prompt 没有要求 Element Plus MessageBox 打开时只在前景 `.el-message-box` / `.el-overlay-message-box` 内查找确认按钮；删除确认框打开后，脚本的全局 action locator 命中背景操作列按钮并被 `.el-overlay-message-box` 拦截。
14. Runner prompt 没有要求 `工艺路线复制绑定` 详情校验在检查页签前先确认打开的 `工艺路线详情` 已加载副本编码和副本名称；在 Element Plus 固定列/重复 DOM 或空白详情入口场景下，生成脚本会把空白占位弹窗误判为页签缺失。
15. Runner prompt 没有明确禁止把 Element Plus 表格 `.cell` 容器当成路线编码详情入口；`.cell` 不是交互控件，点击不会触发 `openForm('detail', scope.row.id)`，必须点击真实 `button.el-button.is-link` / `.el-button.is-link` / `a` / `[role="link"]`。
16. Runner prompt 没有要求 RouteForm 详情读取精确 `.el-form-item` 编码/名称的 `inputValue`；Element Plus input 值不会可靠出现在 `modal.innerText()` 中，innerText-only 判断会把真实详情误判为空。
17. `RouteForm.vue` 使用异步 `RouteFormContent`，父弹窗设置 `dialogVisible=true` 后只等待一个 `nextTick()` 并通过 `contentRef.value?.open(type, id)` 可选链调用；当子组件尚未挂载时调用被静默跳过，父标题显示“工艺路线详情”，但子组件仍处于默认 create 空表单状态，导致不发 `/mes/pro/route/get?id=...` 且详情 inputValue 为空。
18. Runner prompt 对本机登录仍过于宽松，允许生成脚本用 `page.locator('input:visible').first()` 和 `filter({ hasNot: page.locator('[type="password"]') })` 填登录表单；这可能误填租户/用户名/密码字段。生成脚本还未强制等待目标 history 路由的 `.table-quick-filter` 与 `.el-table` 渲染完成，就调用 quickFilter，导致应用 splash/loading spinner 阶段被误判为业务 quick-filter 缺失。
19. Runner prompt 虽要求读取 RouteForm `inputValue`，但没有禁止单次读取为空即失败，也没有要求点击路线编码后等待 `/admin-api/mes/pro/route/get?id=...` 或轮询 `input.el-input__inner/input/textarea` DOM value；生成脚本在 RouteForm 数据尚未完成灌入时把空值误判为详情失败。
20. Runner prompt 对工艺路线 quick-filter 仍允许强制切换到 `路线名称`；当当前页面只稳定显示 `路线编码` 字段且 `路线名称` 选项不可见时，生成脚本会因 `quick-filter option 路线名称 not visible` 阻塞，而不是按固定路线编码继续查询、复位和清理。
21. Runner prompt 未同步复制绑定固定源路线的当前正式编码；历史 `922067 / RT000006 / 球囊扩张压力泵` 已软删除，当前可用源路线是 `922119 / RT000028 / 球囊扩张压力泵`。生成脚本在字段仍为 `路线编码` 时填入源路线名称，误报源路线缺失。
22. Runner prompt 未要求保存成功后的弹窗关闭按钮 detached/unstable 时先用列表结果确认保存是否已成功；生成脚本把 Element Plus transient close click 抖动误报为新增阻塞，尽管截图和列表已经显示固定路线 `TN-ROUTE-BASIC-001`。
23. Runner prompt 未要求临时 Playwright 脚本执行前先运行 `node --check`，也未禁止同一函数/块作用域内重复声明 `const/let` 标识符；因此生成脚本重复声明 `const modal` 时直接进入 `node` 执行并在浏览器启动前失败。
24. Runner prompt 未要求目标 history 路由加载时同时等待“业务控件或登录页”并循环处理登录；生成脚本只在直接导航后短暂检查登录表单，Vue 异步重定向稍后发生时已经误判为已登录，最终停留在 `/login?redirect=/mes/pro/route`。
25. Runner prompt 未说明工艺路线 `流转关系图` 使用 div 节点卡片和 CSS 连接线渲染；生成脚本只按 active tab pane 文本、canvas 或 svg 判断内容，忽略 `.route-flow-graph-designer__node` / `[data-flow-node="route-process"]` 等真实图谱 DOM，误报图谱为空。
26. Runner prompt 对复制弹窗提交按钮仍存在正则分裂：已提示业务确认按钮可能是 `确认复制`，但确定性 footer selector 和生成脚本 helper 仍只用 `/保存|确定|提交/`，导致版本发布复制弹窗中可见 `确认复制` 主按钮被漏掉；同时版本发布源路线仍允许硬编码旧名称 `按压式球囊扩充压力泵`，需要同步为当前正式源样本 `RT000028 / 球囊扩张压力泵`。
27. Runner prompt 未同步版本工作台候选草稿的真实收尾入口；当前页面草稿候选行的正式清理动作是 `删除草稿`，生成脚本只找 `取消候选` 同义词，导致真实可见入口被漏掉并留下 `TN-ROUTE-VERSION-001` 测试路线待清理。
28. Runner prompt 未同步版本工作台候选清理完成态；草稿删除后页面显示 `无打开候选` 且只剩 `创建候选版本`，这应代表候选清理已完成。生成脚本仍可能继续找 `删除草稿` 或重新创建候选，导致 Codex 子进程再次达到 `600000ms` 硬超时。
29. Runner prompt 未要求点击 `新增工艺路线` 后等待可见弹窗内异步 `RouteFormContent` 和 `.el-form-item` 字段标签渲染完成；生成脚本在弹窗 shell 刚出现但内容尚未加载时立即查找 `编码/路线编码` 表单项，误报 `form item not found`。
30. Runner prompt 将临时 Playwright 脚本 deadline 描述为基于完整 Codex exec timeout 的示例，没有强制短 browser-flow 上限；子任务生成脚本时已消耗数分钟，又把脚本 deadline 设为 `540000ms`，最终被外层 `600000ms` 硬超时截断，无法结构化回写。
31. Runner prompt 虽要求 `删除草稿` 文本上溯真实按钮，但仍默认 `草稿` 状态和 `删除草稿` 操作在同一个 DOM 行/卡片内；Element Plus 固定列会拆分状态列与操作列，导致脚本看到 `删除草稿` 文本却误报无法解析到候选行可点击动作。
32. Runner prompt 登录规则仍允许保留登录页旧残留值、只在输入框为空时填默认值，且没有强制缺省默认账号/密码或登录接口缺失时立即 BLOCKED；这会把真实登录失败伪装成目标页面未就绪。
33. Runner prompt 的版本发布候选可见性判定只接受 `当前生效/生效版本/当前版本`，没有覆盖当前工作台真实文案 `V1 已生效 ACTIVE`、`当前 ACTIVE` 或 `ACTIVE`；因此生成脚本把已经可见的 V2 草稿候选与 V1 生效版本误判为缺失。
34. Runner prompt 未说明工艺路线列表的启停入口是 `状态` 列 `el-switch`，不是操作列文字按钮。生成脚本只扫描 `停用/启用` 文本操作，且要求行文本出现状态文案，导致在仅显示开关的正式页面误判入口缺失。
35. Runner prompt 未要求目标页 ready 判定使用 `:visible` 控件选择器或逐个候选检查；生成脚本对 `.table-quick-filter, .unified-list-template__quick-filter` 这类多匹配 locator 直接调用 `isVisible()`，可能被隐藏副本误导，即使页面正文已经有 `工艺流程`、`查询/新增`、表格列和数据行也返回 BLOCKED。
36. Runner prompt 未要求版本发布打开工作台时必须限定到目标路线表格 body 行操作列/固定右列 `版本` 动作；生成脚本使用全局 `/版\s*本/` 点击时会误中页脚/全局 `版本信息` 入口，打开 `版本变更说明` 覆盖层而不是目标路线版本工作台。
37. Runner prompt 未明确 `BUTTON.el-button--danger.is-link` 是有效行操作按钮；生成脚本在已经解析到目标删除按钮时仍可能把 danger/link-style class 当作 unavailable，而不是直接点击 resolved ElementHandle。
38. Runner prompt 未要求 `工艺路线基础维护` checkpoint 1 在发现 `TN-ROUTE-BASIC-001` 残留时必须先执行行内删除、确认 Element Plus MessageBox 并重新按编码查询到无可见 body 行；因此脚本允许把“残留存在”直接作为 FAIL，而没有完成复位动作。
39. Runner prompt 把 `[data-testid="route-version-workspace"]` 描述成可接受的版本工作台容器，但该 testid 实际在列表行操作列 `版本` 按钮上；生成脚本因此可能选择行内按钮作为 workspaceLocator，导致 workspace text 只有 `版本`，漏掉真正弹窗正文 `.route-version-workspace__body` 中的 `创建候选版本`。
40. Runner prompt 未明确表格行 helper 可能返回 `{ row, text, index }` 包装对象；生成脚本把包装对象当成 Playwright Locator 直接调用 `matchedRow.locator(...)`，导致复制绑定详情校验在进入副本详情前被运行时异常阻断。
41. Runner prompt 未要求状态删除节点使用完整源路线副本作为启停样本；生成脚本创建空白 `TN-ROUTE-STATUS-001` 后直接启用，触发正式业务校验 `请先添加组成工序`。状态删除节点应复制 `RT000028 / 球囊扩张压力泵`，确保路线有组成工序后再验证状态列开关和删除闭环。
42. Runner prompt 未要求 quick-filter 查询后等待 Element Plus 表格 loading mask/spinner 消失；生成脚本在目标编码 `TN-ROUTE-VERSION-001` 已提交但表格仍加载时读取旧的 `RT000028` 行，误把 stale row 当成最终查询结果。

## Regression Test

- `IntRuoyiFronted/tests/e2e/codex-test-runner-readonly-timeout-static.spec.js`
- `IntRuoyiFronted/tests/e2e/codex-runner-on-demand-startup-script-static.spec.js`
- `IntRuoyiFronted/tests/e2e/codex-test-runner-failure-diagnostics-static.spec.js`
- `IntRuoyiBackend/yudao-server/src/test/java/cn/iocoder/yudao/server/CodexTestLocalConfigTest.java`
- `IntRuoyiFronted/tests/e2e/codex-test-runner-playwright-dependency-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-form-async-open-static.spec.js`
- `doc/tasks/20260730-test-management-serial-routes-repair/run-serial-routes-real-e2e.mjs`

## RED

- `RED: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> FAIL, expected reason: 写入型任务没有独立推理预算和统一隔离策略`
- `RED: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> FAIL, expected reason: Runner 工作目录仍为仓库根目录`
- `RED: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> FAIL, expected reason: 错误诊断未脱敏保留 stderr 尾部`
- `RED: node stdin static config assertion -> FAIL, expected reason: application-local.yaml 缺少 yudao.codex-test.artifact-temp-dir`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner 未解析本机 Chrome/Edge executablePath，未传递 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH 给子任务`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 缺少 Vue history 路由和工艺路线正式入口 /mes/pro/route`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 缺少 one-shot JSON return、工艺路线详情行级操作、精确 .el-form-item 填字段和 TableQuickFilter 作用域约束`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 没有区分 路线编码 基础详情链接和操作列 编辑/版本 候选版本入口`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 没有要求列表查询前关闭 Element Plus overlay 并等待遮罩消失`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 没有要求 MessageBox 确认按钮限定在前景系统提示框内`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 没有要求复制绑定详情校验先确认副本编码/名称，且空白占位详情需关闭后从可见副本行路线编码链接重开`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 允许把 .cell 表格容器当成路线编码点击候选，未要求点击真实 Element Plus link button 并确认详情弹窗打开`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 没有要求 RouteForm 详情读取编码/名称 inputValue，允许只用 modal.innerText 判断详情为空`
- `RED: node tests\e2e\mes-route-form-async-open-static.spec.js -> FAIL, expected reason: RouteForm 缺少 waitForContentRef()，仍通过 contentRef.value?.open(type, id) 可选链跳过异步内容组件 open`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 缺少 .login-form 精确租户/用户名/密码定位与登录接口成功等待，也未要求进入 /mes/pro/route 后先等待 quick-filter/table 业务控件渲染`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求 RouteForm 详情等待 route/get 或轮询 input DOM value，允许单次读取为空即失败`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求路线名称选项不可见时保持路线编码并按 route code 查询，允许生成脚本因 quick-filter option 路线名称 not visible 阻塞`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求复制绑定固定源路线使用正式编码 RT000028，允许把源路线名称填入路线编码字段后误报源路线缺失`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求保存成功后关闭按钮 detached/unstable 时先返回列表并用 quick-filter 验证固定路线已保存，允许把已列表命中的新增结果误报为 BLOCKED`
- `RED: 真实 E2E execution 76 -> FAIL, expected reason: 生成的临时 Playwright 脚本同一函数内重复声明 const modal，且 Runner prompt 未要求 node --check 语法自检或修复同作用域 const/let 重复声明`
- `RED: 真实 E2E execution 77 -> FAIL, expected reason: 复制绑定节点在 Vue 异步重定向后停留 /login?redirect=/mes/pro/route，Runner prompt 未要求登录页/业务控件二选一循环等待和登录后重返目标路由`
- `RED: 真实 E2E execution 78 -> FAIL, expected reason: 流转关系图页签已有可见 div 工序节点和连线，但生成脚本按 pane 文本/canvas/svg 误判为空`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求复制弹窗 footer 的“确认复制”按业务按钮点击，且未要求版本发布固定源样本使用 RT000028 / 球囊扩张压力泵`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求版本发布候选草稿用“删除草稿”收尾，允许生成脚本只查找“取消候选”后误报入口缺失`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求版本发布删除草稿按可见文本子节点上溯真实按钮，也未区分按钮禁用/加载与入口缺失`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求删除/复位后只用可见表格 body 行判断目标路线是否仍存在，允许把筛选输入框里的路线编码当成残留`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求临时 Playwright 脚本自带全局截止时间并在超时前输出 BLOCKED JSON，允许等 Codex 子进程 600 秒硬超时`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求版本发布工作台显示“无打开候选”时视为候选清理已完成，不能继续找删除草稿或重新创建候选直到超时`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求新增工艺路线弹窗等待 RouteFormContent 和表单项渲染完成后再填字段，允许弹窗壳已打开但内容异步加载时误报 form item not found`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 必须强制临时 Playwright 脚本 deadline <= 240000ms，不能按外层 600000ms 预算生成 540000/560000ms 后再被外层硬超时截断`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 必须覆盖 Element Plus 固定列拆分场景：草稿状态和删除草稿操作不在同一 DOM 行时仍要点击可见删除草稿`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 必须要求登录时覆盖旧残留值且密码为空时 fail-fast，不能空登录后继续等待目标页面`
- `RED: 真实 E2E execution 92 -> FAIL, expected reason: 版本工作台已有 V2 草稿与 V1 已生效/ACTIVE，但生成脚本未把 已生效/ACTIVE 识别为当前生效版本标记`
- `RED: 真实 E2E execution 93 -> FAIL, expected reason: 状态删除节点页面已有状态列 el-switch，但生成脚本只找操作列 停用/启用 文字按钮并误报入口不可见`
- `RED: 真实 E2E execution 94 -> FAIL, expected reason: 目标页已显示筛选区/表格/数据行，但生成脚本用未过滤 multi-locator isVisible 命中隐藏副本并误报 controls did not render`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求删除草稿后等待确认框关闭、核对 /admin-api/mes/pro/route-version/cancel 请求或刷新证据，允许点击后仅轮询旧 workspace 文本并误报候选仍可见`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未禁止把 ElementHandle 包装成 locator，允许生成脚本在可见行含 删除 时仍误报路线删除入口不可见`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 必须要求版本工作台入口限定在目标路线表格行操作列，不能误点页脚/全局版本信息`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未明确 BUTTON.el-button--danger.is-link 是有效行操作，允许生成脚本在已解析到删除按钮时仍报告 action handle unavailable`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求基础维护 checkpoint 1 命中固定路线时先删除确认并复查，允许直接 FAIL`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 允许把行内 data-testid="route-version-workspace" 的 版本 按钮当成已打开工作台，没有要求等待 .route-version-workspace__body`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未禁止把包含 row/text/index 的表格行包装对象直接当成 Playwright Locator，允许生成 matchedRow.locator is not a function`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求状态删除节点通过复制完整源路线准备 TN-ROUTE-STATUS-001，允许创建空白路线后启用触发 请先添加组成工序`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求 quick-filter 查询后等待 Element Plus loading mask/spinner 消失，允许目标查询值为 TN-ROUTE-VERSION-001 时读取 stale RT000028 旧行`

## GREEN

- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-child-settlement-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes browser executablePath and official navigation hints`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes one-shot JSON return, exact Element Plus form-item fills, row action detail verification, and visible TableQuickFilter scoping`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes route-code detail link guidance and forbids operation-column 编辑/版本 for 工艺路线基础维护 base detail`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes pre-query Element Plus overlay close/wait/retry guidance`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes visible MessageBox-scoped primary action selection`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes copied-route detail code/name validation and blank placeholder detail reopen guidance`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes forbidding .cell route-code click candidates and requiring the actual Element Plus route-code link button plus 工艺路线详情 open assertion`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes RouteForm detail .el-form-item 编码/名称 inputValue verification instead of innerText-only blank judgment`
- `GREEN: node tests\e2e\mes-route-form-async-open-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes .login-form scoped login selectors, /system/auth/login + permission-info waits, and target route quick-filter/table page-ready wait`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes RouteForm detail /mes/pro/route/get wait or 30-second exact form-item DOM value polling helper`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes keeping 路线编码 and route-code search when quick-filter option 路线名称 is unavailable`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes fixed source route RT000028 / 球囊扩张压力泵 and forbids submitting the source name into 路线编码`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes post-save detached/unstable close handling by verifying the saved fixed route in the list before marking BLOCKED`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes node --check before temporary Playwright script execution and forbids same-scope const/let redeclarations such as modal`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes post-login target-route navigation and login-or-business-controls loop for async /login redirects`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes RouteFlowGraphDesigner div node/CSS connector visibility criteria for 流转关系图 tab checks`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 确认复制 footer business action selector and 工艺路线版本发布 source sample RT000028 / 球囊扩张压力泵`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 工艺路线版本发布 candidate cleanup via visible 删除草稿 action`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes Element Plus link-button 删除草稿 visible text descendant climb, 15-second enabled wait, and visible-but-disabled/loading diagnostic`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes route delete/reset absence judged only from visible table body rows, not body text or quick-filter input values`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes temporary Playwright script overall deadline, main-flow race, browser close, and BLOCKED checkpoint JSON before Codex child timeout`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 工艺路线版本发布 candidate cleanup completed state via 无打开候选 without clicking 创建候选版本 during checkpoint 4 cleanup`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 新增工艺路线 dialog waiting for .route-form-content and visible form-item labels before filling`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes hard-capping temporary browser script deadline at 240000ms instead of using the outer 600000ms budget`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes Element Plus fixed-column candidate cleanup where 草稿 and 删除草稿 are split across DOM tables`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes always overwriting .login-form username/password defaults, missing-default fail-fast, and login response code=0 gating before business-control waits`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 工艺路线版本发布 candidate visibility accepting V2 草稿 plus V1 已生效 / 已生效 ACTIVE / 当前 ACTIVE / ACTIVE`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 工艺路线状态删除 enable/disable verification through 状态 column el-switch / role=switch rather than operation-column text buttons`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes target route ready checks using :visible quick-filter/table locators or candidate iteration instead of unfiltered multi-locator isVisible`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 删除草稿 candidate cleanup MessageBox hidden wait, cancel request/list-refresh evidence, and failure diagnostics with request/message-box state`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes Element Plus row operation link-buttons resolved from span text and clicked through direct ElementHandle without locator wrapping`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes row-scoped 工艺路线版本发布 workspace entry and forbids global clickVisibleTextAction/page.getByText('版本')`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes BUTTON.el-button--danger.is-link as a valid resolved row action`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 工艺路线基础维护 checkpoint 1 reset deleting stale TN-ROUTE-BASIC-001 before failing`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes 工艺路线版本弹窗正文 .route-version-workspace__body as the workspace locator and rejects row button testid as workspace`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes row helper wrapper normalization via matchedRow.row before calling locator methods`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes status-delete setup by copying complete source route RT000028 and treating 请先添加组成工序 as invalid blank-route setup`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS, includes quick-filter post-query wait for loading mask/spinner removal and stale RT000028 row handling after TN-ROUTE-VERSION-001 query`
- `GREEN: node --check doc\tasks\20260730-test-management-serial-routes-repair\run-serial-routes-real-e2e.mjs -> PASS`
- `GREEN: node stdin static config assertion -> PASS, application-local.yaml 已包含运行态 artifact 临时目录和保留时长`
- `GREEN: mvn.cmd -pl yudao-server -Dtest=CodexTestLocalConfigTest test -> PASS, Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS`

## Verification

- `Verification: 真实 E2E preflight -> PASS, 三条节点串筛选数量为 4/6/4，Runner session 95 ONLINE/currentRunningCount=0`
- `Verification: 真实 E2E partial -> BLOCKED, 工艺路线节点闭环 execution 37 因 artifact 临时目录未配置失败；该配置缺口已补齐但 48081 尚未恢复，不能继续页面复验`
- `Verification: Runner browser dependency static regression -> PASS, 子任务环境现在包含前端 Playwright 依赖与本机浏览器 executablePath prompt/环境变量`
- `Verification: 真实 E2E execution 48 -> BLOCKED, 浏览器启动已正常但临时脚本未进入 /mes/pro/route；已用静态契约补齐正式导航提示，待复跑真实页面`
- `Verification: 真实 E2E execution 67 -> FAIL, 工艺路线基础维护已 PASS，复制绑定打开空白工艺路线详情导致绑定信息可见 FAIL；已用静态契约补齐副本详情编码/名称确认与空白详情重开要求，待复跑真实页面`
- `Verification: 真实 E2E execution 68 -> FAIL, 工艺路线基础维护已 PASS，复制绑定副本复位/复制/清理 PASS，但详情入口点击停留在列表；已用静态契约补齐真实路线编码 link button 点击要求，待复跑真实页面`
- `Verification: 真实 E2E execution 69 -> FAIL, 工艺路线基础维护新增和清理 PASS，但详情校验 innerText-only 误判 inputValue 为空；已用静态契约补齐 RouteForm inputValue 校验，待复跑真实页面`
- `Verification: node doc\tasks\20260730-test-management-serial-routes-repair\diagnose-route-copy-detail.mjs -> PASS, 修复前副本列表和详情 API 均有正确 id/code/name 但点击不触发 route/get；修复后真实页面点击副本路线编码触发 /admin-api/mes/pro/route/get?id=922327，详情 inputValue 为 TN-ROUTE-COPY-001 / 测试节点-工艺路线-复制绑定-副本`
- `Verification: 真实 E2E execution 71 -> FAIL, 工艺路线基础维护全检查点 PASS；复制绑定 BLOCKED at checkpoint 1，actualText=浏览器执行阻塞：visible quick filter not found，失败截图为应用 splash/loading spinner；已补齐登录精确定位和目标页 ready 等待，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 72 -> FAIL, 登录和目标页 ready 已生效；基础维护第 1 检查点 PASS，但第 2 检查点 BLOCKED，actualText=详情表单值不匹配，编码=空，名称=空；已补齐 RouteForm detail route/get 等待与 30 秒 DOM value 轮询 helper，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 73 -> FAIL, 工艺路线基础维护第 1 检查点 BLOCKED，actualText=Unhandled browser execution error: quick-filter option 路线名称 not visible；已补齐路线名称选项不可见时保持路线编码并按 route code 查询，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 74 -> FAIL, 工艺路线基础维护全 PASS；复制绑定第 1 检查点误报固定源路线缺失，真实 UI/API 只读诊断证明当前源路线为 922119 / RT000028 / 球囊扩张压力泵；已补齐固定源路线编码规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 75 -> FAIL, 工艺路线基础维护第 2 检查点误报新增阻塞；截图证明固定路线已在列表命中，真实阻塞是保存成功后关闭按钮 detached/unstable；已补齐 post-save 列表确认规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 76 -> FAIL, 临时 Playwright 脚本浏览器启动前语法失败：Identifier 'modal' has already been declared；已补齐 node --check 语法自检和同作用域 const/let 重复声明禁用规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 77 -> FAIL, 工艺路线基础维护全 PASS；复制绑定停留 /login?redirect=/mes/pro/route 导致列表控件未渲染；已补齐登录后显式重返目标路由与登录页/业务控件循环等待规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 78 -> FAIL, 工艺路线基础维护全 PASS；复制绑定副本复位、复制和清理 PASS，但流转关系图 div 图谱被误判为空；已补齐 RouteFlowGraphDesigner 节点/连线/版本标签可见性规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 80 -> FAIL, 工艺路线基础维护与复制绑定均 PASS；版本发布复制弹窗已正确填入编码/名称但漏点“确认复制”主按钮，已补齐业务按钮选择器与版本发布源样本规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 81 -> FAIL, 工艺路线基础维护与复制绑定均 PASS；版本发布第 1-3 检查点 PASS，第 4 检查点在草稿行“删除草稿”入口误判为缺失；已补齐删除草稿收尾规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 82 -> FAIL, 工艺路线基础维护与复制绑定均 PASS；版本发布第 1-3 检查点 PASS，第 4 检查点页面正文和截图均显示“删除草稿”可见，但生成脚本仍按直接 button hasText/isEnabled 判断误报入口缺失；已补齐文本子节点上溯真实按钮与 disabled/loading 诊断规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 83 -> FAIL, 工艺路线基础维护与复制绑定均 PASS；版本发布第 1 检查点截图显示删除成功且表格 No Data，但生成脚本把 quick-filter 输入框中的 TN-ROUTE-VERSION-001 当成删除后残留；已补齐删除/复位后只按可见表格 body 行判断残留的规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 84 -> FAIL, 工艺路线基础维护与复制绑定均 PASS；版本发布节点未返回结构化检查点而是 Codex Runner 子进程 600000ms 硬超时；已补齐临时 Playwright 脚本自带 deadline 并超时前返回 BLOCKED JSON 的规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 86 -> FAIL, 工艺路线基础维护与复制绑定均 PASS；版本发布截图显示候选清理后工作台处于“无打开候选”，但子任务仍未结构化返回并达到 600000ms；已补齐“无打开候选”即候选清理完成、checkpoint 4 不得点击“创建候选版本”的规则，待重启 Runner 后复跑真实页面`
- `Verification: 真实 E2E execution 88 -> FAIL, 工艺路线基础维护第 1 检查点 PASS；第 2 检查点在新增弹窗 shell 打开但 RouteFormContent 尚未渲染时误报 form item not found；已补齐新增工艺路线弹窗内容和表单项等待规则，待复跑真实页面`
- `Verification: 真实 E2E execution 89 -> FAIL, 工艺路线基础维护首节点被 Codex 子进程 600000ms 硬超时截断；最新临时脚本生成耗时约 5 分钟且脚本 deadline 为 540000ms，已补齐临时 browser-flow deadline 240000ms 硬上限，待复跑真实页面`
- `Verification: 真实 E2E execution 90 -> FAIL, 工艺路线基础维护和复制绑定均 PASS，版本发布第 1-3 检查点 PASS；第 4 检查点可见草稿候选和删除草稿但未能点击固定列操作，已补齐 Element Plus fixed-column 删除草稿定位规则，待复跑真实页面`
- `Verification: 真实 E2E execution 91 -> FAIL, 工艺路线基础维护 4 个检查点均 BLOCKED，actualText=目标页面未就绪且停留 /login?redirect=/mes/pro/route；已补齐登录默认值覆盖、缺省输入 fail-fast 和登录响应 code=0 门禁，待复跑真实页面`
- `Verification: 真实 E2E execution 92 -> FAIL, 工艺路线基础维护和复制绑定均全检查点 PASS；版本发布第 2 检查点实际页面含 V2 草稿、V1 已生效 ACTIVE、当前 ACTIVE，但误报当前生效说明缺失；已补齐版本候选可见性规则，待复跑真实页面`
- `Verification: 真实 E2E execution 93 -> FAIL, 工艺路线前三个节点全部 PASS；状态删除第 2 检查点固定路线已创建但误报停用入口不可见，截图证明正式入口为状态列 el-switch；已补齐状态开关定位与启停状态判定规则，待复跑真实页面`
- `Verification: 真实 E2E execution 94 -> FAIL, 首节点页面实际含工艺流程标题、查询/新增按钮、表格列和数据行，但误报 Target route controls did not render；已补齐 visible-only page-ready selector 规则，待复跑真实页面`
- `Verification: 真实 E2E execution 102 -> FAIL, 工艺路线基础维护和复制绑定均全检查点 PASS；版本发布第 2 检查点误点全局版本信息覆盖层，未打开目标行版本工作台；已补齐目标行操作列/固定右列版本入口约束，待复跑真实页面`
- `Verification: 真实 E2E execution 103 -> FAIL, 基础维护第 4 检查点已解析到 BUTTON.el-button--danger.is-link 删除按钮但误报 action handle unavailable；已补齐有效 danger link-button 直接点击规则，待复跑真实页面`
- `Verification: 真实 E2E execution 104 -> FAIL, 基础维护 checkpoint 1 命中上轮残留 TN-ROUTE-BASIC-001 后未先执行删除确认复查而直接 FAIL；已补齐 checkpoint 1 reset 删除闭环规则，待复跑真实页面`
- `Verification: 真实 E2E execution 105 -> FAIL, 基础维护和复制绑定均 PASS，版本发布已打开工艺路线版本弹窗但把行内 data-testid 按钮误当 workspace，导致创建候选版本 action unavailable；已补齐弹窗正文 .route-version-workspace__body 等待规则，待复跑真实页面`
- `Verification: 真实 E2E execution 106 -> FAIL, 基础维护全 PASS；复制绑定第 3 检查点因 matchedRow 包装对象被当成 Locator 调用而 BLOCKED；已补齐 matchedRow.row 归一化规则，待复跑真实页面`
- `Verification: 真实 E2E execution 107 -> FAIL, 工艺路线前三个节点均 PASS；状态删除节点启用空白路线时触发 请先添加组成工序；已补齐状态删除节点复制完整源路线 RT000028 的正式样本准备规则，待复跑真实页面`
- `Verification: 真实 E2E execution 108 -> FAIL, 基础维护和复制绑定均 PASS；版本发布复制保存后在目标编码查询 loading 中读取 stale RT000028 旧行并误报未命中；已补齐 quick-filter 查询后等待 loading 消失规则，待复跑真实页面`
- `Verification: 真实 E2E execution 109 -> FAIL, 基础维护和复制绑定均 PASS；版本发布第 1-3 检查点 PASS，第 4 检查点工作台显示“无打开候选”且无草稿/待处理/待发布行，却误报 cleanup candidate action not visible；已补齐该完成态禁止报入口缺失并直接按候选清理完成推进的规则，待复跑真实页面`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未锁定 execution 109 的 cleanup candidate action not visible 误判`
- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `Verification: 真实 E2E execution 110 -> FAIL, 工艺路线前三个节点 PASS；状态删除节点因 Codex exec timed out after 720000ms BLOCKED，Runner 无活动 execution 但任务临时 route-status-delete-e2e.js 仍残留 node/Chrome orphan`
- `RED: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> FAIL, expected reason: Runner prompt 未要求临时 Playwright 脚本 deadline 输出 BLOCKED JSON 后 process.exit(0)，允许未完成主流程/orphan 拖到 Codex exec timeout`
- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `Verification: 真实 E2E execution 111 -> FAIL, 首节点 heartbeat 请求后端排队超过 Runner 客户端 30000ms 但未超过后端 60s 在线窗口，被客户端提前 abort 后误写四个 BLOCKED 检查点`
- `RED: node tests\e2e\codex-test-runner-http-client-static.spec.js -> FAIL, expected reason: Runner heartbeat 客户端超时仍使用默认 30000ms，未覆盖后端 60 秒 heartbeat 窗口`
- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `Verification: 真实 E2E execution 115 -> FAIL, 首节点仍在 720000ms 超时且未生成新临时脚本；execution 114 的 69024 字节基础维护脚本直接运行 30 秒内 4/4 PASS，说明真实浏览器流程正常，主要耗时是 Codex 为每个节点重复生成完整独立 Playwright 脚本`
- `BDD: Runner 复用公共 Playwright harness 生成短场景脚本 -> Given 测试管理按节点领取三条串行路线中的业务测试项, When Runner 构造 Codex 浏览器执行 prompt, Then Codex 子任务必须导入官方 Playwright harness，只编写 checkpoint 场景编排，不得在每个节点脚本中重复生成登录、deadline、截图、checkpoint、Element Plus 弹窗、quick-filter、行操作和路线表单 helper。`
- `RED: node tests\e2e\codex-test-runner-short-script-harness-static.spec.js -> FAIL, expected reason: Runner 缺少 scripts/codex-test-playwright-harness.cjs，prompt 未要求导入官方 harness、限制短场景脚本、或禁止重复实现公共 helper`
- `GREEN: node --check scripts\codex-test-runner.mjs -> PASS`
- `GREEN: node --check scripts\codex-test-runner-guidance.mjs -> PASS`
- `GREEN: node --check scripts\codex-test-playwright-harness.cjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-short-script-harness-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-case-guidance-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-child-settlement-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> PASS`
- `GREEN: node tests\e2e\mes-route-form-async-open-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- `Verification: 真实 E2E execution 116 -> FAIL, 首节点已生成 4745 字节短场景脚本 codex-route-basic-maintenance-harness.js，证明旧 50-80KB 重复 helper 生成瓶颈已消除；新阻塞为 harness 把 { row, locator, text } 包装对象误当 Locator，导致详情编码 link 与删除动作定位失败`
- `RED: node tests\e2e\codex-test-runner-short-script-harness-static.spec.js -> FAIL, expected reason: Playwright harness 处理表格行时未区分 Locator 本体和 { row, locator, text } 包装对象，且路线编码详情入口缺少全页可见 link/button 候选兜底`
- `GREEN: node --check scripts\codex-test-playwright-harness.cjs -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-short-script-harness-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-playwright-dependency-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-case-guidance-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-child-settlement-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-test-runner-http-client-static.spec.js -> PASS`
- `GREEN: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> PASS`
- `GREEN: node tests\e2e\mes-route-form-async-open-static.spec.js -> PASS`

## Risk And Regression Scope

- 覆盖 Runner Codex 子进程隔离、失败诊断、子进程超时收敛、artifact 截图上传、本地后端运行态配置和三条正式串行路线。
- 仍需在真实页面复跑三路线，确认 `artifact-temp-dir`、本机浏览器 executablePath、正式 history 路由导航提示、按节点提示词拆分和短场景 Playwright harness 均不再阻断 Runner 回写。

## Blockers And Follow-up

- 当前剩余工作是通过真实 `系统管理 > 测试管理` 页面复跑 3 条串行路线并到 `测试记录` 核对终态；不得用 API-only、静态合同或缓存浏览器下载替代最终页面证据。
