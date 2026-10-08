# 会话边界修复验证报告

## Bug / Expected
资料保存请求或回读触发无感续期后，应完成真实用户store与缓存昵称同步；新的登录仍清上一身份缓存。本人改密退出后工作台不再请求，旧响应不回写；无刷新令牌的401必须拒绝且不使下一登录会话的刷新队列挂起。

## Reproduction / Root Cause
- BasicInfo.submit -> updateUserProfile/getUserProfile -> Axios401 refresh -> auth.setToken删除USER/ROLE_ROUTERS/VisitTenantId -> userStore.setUserNicknameAction访问空USER.user。
- ResetPwd.submit -> clearSession删除token并重置store -> ProfileWorkbench同步scope监听调用loadWorkbench发匿名请求 -> Axios401在检查refresh token前置isRefreshToken=true -> early return绕过finally；在/login上handleAuthorized返回undefined。后续401被放入无刷新动作的队列。

## Verification

定向实现验证PASS；前端及后端正式业务链独立复审PASS，详见review-report.md。本结论不代表全量类型检查或真实E2E通过。

| 验收要求 | 实际证据 | 结果 |
| --- | --- | --- |
| 同会话续期保留用户/菜单/访问租户，资料保存正常同步 | 执行生产BasicInfo、auth、user store、Axios；分别在PUT及GET返回401后回放，断言昵称store/cache和visit-tenant-id | PASS |
| 新登录清除上一身份上下文 | 实际auth.setToken清除USER/ROLE_ROUTERS/VisitTenantId；独立审查所有登录入口仍调用setToken | PASS |
| 无refresh令牌401不能锁死后续刷新 | 登录页匿名401必须reject；随后新登录下一401实际续期成功 | PASS |
| 并发刷新成功/失败均能结束并可恢复 | 实际Axios并发成功共享单次刷新；业务失败全部reject，后续新登录续期恢复 | PASS |
| clearSession中间状态不发匿名工作台查询 | 实际Vue同步watcher、Pinia clearSession、工作台handler；清会话后page请求数量不变，旧响应不提交页面/角标 | PASS |
| 保留合法权限刷新，阻断卸载/旧成功及失败回写 | 权限变化正式选择来源并刷新；卸载后不再发请求；仅移除token时旧错误也不提交 | PASS |
| 原资料/密码/会话行为未回归 | 原3组行为测试55/55；原4组静态合同4/4 | PASS |
| 目标源文件lint与组件编译 | ESLint三项生产文件exit0；compiler-sfc解析及compileScript/compileTemplate检查ProfileWorkbench、BasicInfo、ResetPwd均通过 | PASS |
| 类型诊断没有增量 | 修复前与修复后ts:check均exit2，诊断完全相同 | PASS：无新增；全量类型检查BLOCKED |
| 后端身份、落库与会话合同一致 | 独立只读追踪Profile Controller、正式登录ID、资料Mapper写/读、改密策略/历史/落库与全部会话撤销、正式refresh身份及锁内校验 | PASS：静态合同，不代表真实数据运行验证 |

RED: `node --test tests/e2e/profile-session-boundaries-behavior.spec.cjs` -> FAIL exit1，原5项中4项失败：续期后USER空引用；登录页匿名401未reject；清会话请求数3而非1；卸载后请求数4而非3。

GREEN: 同一命令 -> PASS exit0，8/8（补充并发刷新和token/身份边界回归）。日志session-green.log。

GREEN: `node --test tests/e2e/profile-basic-info-optional-contact-behavior.spec.cjs tests/e2e/profile-reset-password-submit-behavior.spec.cjs tests/e2e/system-user-password-session-behavior.spec.cjs` -> PASS exit0，55/55。

GREEN: `node --test tests/e2e/auth-refresh-token-business-failure-static.spec.js tests/e2e/login-auth-error-message-static.spec.js tests/e2e/login-default-credentials-static.spec.mjs tests/e2e/system-login-security-idle-logout-static.spec.js` -> PASS exit0，4/4。

GREEN: 仅本次进程设置NODE_PATH为当前worktree前端node_modules后，`pnpm.cmd exec eslint src/utils/auth.ts src/config/axios/service.ts src/views/Profile/components/ProfileWorkbench.vue` -> PASS exit0。最初调用因task专有virtual store下vue-eslint-parser不能解析项目@typescript-eslint/parser而exit1；显式解析路径修正后通过，未修改配置或降低规则。

类型证据：`pnpm.cmd ts:check`修复前、后均exit2；3项为notifyMessageNavigation.ts:197的TS2677，以及activeOrderReworkSourceLocation.ts:1、ActiveOrderReworkSourcePanel.vue:27的processPool/processpool大小写TS1149。实际Compare-Object诊断对照为空，两个完整日志SHA256一致。不得将此写为全量类型PASS。

证据路径：D:/IntRuoyiTaskRuntime/profile-session-boundaries/verification，保留session-red.log、session-green.log、session-regression.log、session-auth-static.log、session-eslint.log/exit、baseline-types.log/exit、fixed-types.log/exit等任务自有日志至集成验收。

边界：传输、缓存与UI边界受控替换，真实生产Vue/Pinia/Axios模块及组件脚本执行；未执行浏览器、服务启动、真实后端写入或API/DB操作。tests/e2e目录名称不代表本次真实E2E已通过。

## Blockers
依赖安装与目标lint已解决。全量类型检查受3项既有错误阻塞；本次无新增，不扩大范围。授权的实现提交、修复分支推送、本地主干融合、托管worktree清理与槽位释放均已完成。可选外部依赖缓存递归删除被自动审批拦截，当前保留，不影响已完成的代码集成与workspace清理。

## 收尾边界
根AGENTS当轮Git授权门禁优先于关联文档的默认强制提交/推送。仅处理任务自有记录、修复分支、托管worktree和槽位，不操作其他任务。

- task-closeout-cleanup preview及apply均exit0，显式`--worktree-closeout off`；5项核心记录保留，0项删除，无blocked/warnings。日志cleanup-preview.json及cleanup-apply.json在任务runtime验证目录。没有触发脚本自动提交/融合/worktree删除。
- 已授权提交文件：3项生产文件、2项测试、docs/login-access.md，以及本目录5项核心记录。实现提交只含这11项；最终记录提交仅含本目录收尾更新，不打包主工作区并行改动或runtime日志。
- 先保持ready_for_closeout并完成preview/apply、授权融合及清理验证，再将状态记为completed。主干比基线新增的另一任务5项收尾记录已保留；6项并行未提交文件融合前后SHA256一致。

## 授权集成验证

- 实现提交：初始c06986186，对齐当前int_main 689674145a29a0ef9b7ca76fb20c027df581496b后为0e336ba8ff2acc9e5b638eb6bb880bd55296cbc2，11项任务文件；暂存精确清单及cached diff --check通过，未含并行改动、node_modules或日志。重基前后6项源码/测试/经验Git正文一致。
- 重基后再次合并执行本报告列出的8组Node文件：67/67 PASS exit0；目标ESLint及ProfileWorkbench/BasicInfo/ResetPwd的SFC脚本与模板编译全部PASS。日志integration-regression.log、integration-eslint.log、integration-sfc.log均在任务runtime验证目录。因当前主干增量仅为另一任务文档，类型日志基线证据仍适用，未宣称全量类型通过。
- 修复分支正常推送（无force），第一次TLS中断、同命令重试成功；ls-remote返回0e336ba8ff2acc9e5b638eb6bb880bd55296cbc2，分支ahead/behind=0/0。没有改变remote、全局代理或证书校验。
- 本地int_main以--ff-only融合同一提交，前后端口门禁PASS。主干6项并行dirty的SHA256在融合前后完全一致，主索引无混入；未stash/restore/提交它们。只推送修复分支，未推送主干原有6项领先提交，未发布运行态。
- Codex托管worktree归档已保存可恢复快照，物理目录不存在且git worktree list已无任务登记；归档前5项核心记录在主工作区与源worktree逐文件SHA256相同，归档不丢任务证据。
- 主工作区task-closeout-cleanup preview/apply exit0，keep=5、delete=0、blocked=0、warnings=0。槽位7在现有预约锁内释放，active=false，任务端口8088/48088无监听，全部无关预约条目前后完全相同；released-slot.json保留证据。
- 自动审批拒绝递归删除D:/IntRuoyiTaskRuntime/profile-session-boundaries/pnpm-virtual-store，工具只返回“blocked by policy”；删除未执行，未尝试绕过。该外部缓存保留，审计日志按计划保留。最终记录更新不改变生产与测试代码，无须重复业务验证。
