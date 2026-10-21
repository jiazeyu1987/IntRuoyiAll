# Verification Report

E2E-03 源件与多附件：PREPARING，尚未执行真实E2E。准备、前置、真实执行、Root review及清理状态分开记录；当前不能宣称 PASS。

## Preparation

- 分支：`int_qms`；目标运行 profile：`int_qms`，前端 `8061`，后端 `48061`。
- 当前 dispatch：本例 `gate=prepare_only`，环境 `runtimeVerified=false`、`roleFixtureVerified=false`；脚本在浏览器启动前严格退出 `PREPARED_WAITING`，实际退出码 `20`。
- 脚本：[e2e-03.cjs](/C:/IntRuoyiAll-int_main/doc/tasks/20261016-dcc-e2e-03/e2e-03.cjs)。命令：`node doc/tasks/20261016-dcc-e2e-03/e2e-03.cjs`，工作目录仓库根目录。静态语法命令 `node --check ...` exit `0`。
- Playwright 从 `IntRuoyiFronted/node_modules/playwright` 加载，版本 `1.60.0`；缓存 Chromium 可执行文件已静态确认存在。未启动浏览器，未生成 trace/截图/业务数据。

## Subcase Contract

| 子用例 | 页面动作与断言 | 当前状态 |
| --- | --- | --- |
| U1 新建上传就绪 | 真实页面选择源件、附件A、附件B；保存草稿；详情附件区可见两项；初始申请确认框列出源件和两项附件；取消后再次确认 | 待 execute |
| U2 上传中拦截 | 真实附件上传未完成时读取状态提示和提交按钮 disabled | 待 execute |
| U3 上传失败拦截 | 真实失败附件页面提示明确对象/原因；未移除失败项不能提交 | 待 execute；需任务自有失败文件/真实失败条件 |
| U4 保存失败恢复 | 页面可触达的真实保存失败后记录提示、无成功申请，恢复后重试 | BLOCKED 前置：尚未确认可安全、可重复触发条件 |
| R1 升版源件与附件 | 浏览页检出/检入变更源件和两个附件；WORKING 小版本同主档；升版确认框清单完整 | 待 execute |
| R2 版本隔离 | 受控浏览选择旧版/新版并分别进入追溯详情，旧版仅见旧附件，新版仅见新附件 | 待 execute；缺少版本/附件标识则 BLOCKED |

## Preconditions / Blockers

- Root 尚未核对 `48061` 运行 Jar 来源、健康状态及当前源码/应用 schema；用户已说明前端 `8061` 未监听、`48061` 为旧外部 Jar，本例因此不启动浏览器。
- Root dispatch 尚未确认角色夹具。申请人需要真实 `dcc:controlled-file:submit`/`query` 页面权限；升版操作人需要能对任务自有 ACTIVE 版本检出/检入。缺少独立账号时不得用 admin 冒充。
- 必须提供同一已确认租户内的任务自有项目、叶子文件夹、正式文件类型 taxonomy/category、有效审批路线和申请人组织关系；缺数据时不能通过 API/DB 补齐。
- 必须提供三个内容可区分的源件/附件文件，以及升版变更源件和两个附件；脚本通过环境变量读取路径，不写入密码或凭据。
- U4 的“保存失败”没有在当前源码/页面入口中确认可重复、可恢复的真实触发器；不能用 route interception、mock 响应或篡改请求制造失败。该项保持 BLOCKED，待Root确认真实条件。

## Evidence Boundary

- 真实业务动作全部由 Playwright 页面完成；脚本只监听自然请求 method/path/status，绝不调用 API、SQL、HTTP 客户端、mock、localStorage 或组件方法。
- 只有页面可见 DOM、输入值、按钮 disabled、确认框文本、导航和详情附件清单可作为通过依据；本轮准备阶段没有提交受理证据。
- trace/截图仅在 Root 将 gate 改为 `execute` 且前置通过后生成；认证页面不进入 trace，脚本不打印密码/token/cookie。
- 每个真实 case 的 result 分离 `businessError`、`evidenceErrors`、`cleanupError`/`cleanupErrors`；确认框隐藏等待、截图、trace.stop、context.close 和 browser.close 不再静默吞异常。截图或 trace 缺失不能记为完整验收，业务失败/前置 BLOCKED 原因保留不被 cleanup 错误覆盖。
- `browser.close` 若失败会写入任务目录 `cleanup-errors.json`；若该证据写入失败则显式输出 `evidenceError` 并以非零退出，不伪造清理完成。

## Root Review Checklist

- [ ] dispatch 改为 `execute`，`runtimeVerified`/`roleFixtureVerified` 均为 true，并核对端口与运行 Jar。
- [ ] 真实账号、租户、角色、项目、文件夹、类型、路线和任务自有文件已确认，环境变量仅在运行时注入。
- [ ] 每个子用例独立输出 PASS/FAIL/BLOCKED、截图/trace、页面提示、写请求统计和 cleanupAttempted/cleanupCompleted/residual。
- [ ] 旧版/新版附件详情分别核对，确认无串版；U4 若无正式触发条件保持 BLOCKED。
