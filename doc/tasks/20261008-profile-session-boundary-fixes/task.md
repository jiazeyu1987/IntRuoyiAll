# 个人中心会话边界修复

## Goal
修复静态评审确认的两项问题：资料保存遇到令牌刷新后用户缓存同步失败；改密退出触发工作台匿名请求并使后续401请求卡住。

## Milestones
- M0：完成。规则、独立worktree、依赖与验收范围已确认。
- M1：完成。原生产代码5项回归中4项失败；最小修复后扩展到8项，全部通过。
- M2：完成。8项新回归、55项原行为回归、4项静态合同、目标ESLint及3个相关SFC编译通过；前后端实际业务链独立复审PASS。全量类型检查仍有3项基线错误，没有新增。
- M3：完成。任务记录清理preview/apply通过，5项保留、0项删除、无blocked/warnings；按用户授权提交实现、推送修复分支并快进合入本地int_main，托管worktree归档与目录删除已验证，槽位7已释放。外部依赖缓存递归删除被自动审批拦截，保留并如实记录。

## Expected Verification
- 令牌刷新保持当前身份的缓存与租户上下文；新登录仍清除上一身份缓存。
- 无刷新令牌的401拒绝明确且不会锁死后续会话的刷新队列。
- 清除会话后工作台不发新请求，先前请求不写回页面；有效身份/权限变化仍刷新。
- 实际生产模块的受控行为测试及原个人资料/改密/会话定向回归；目标ESLint、SFC编译、类型检查与基线诊断对照。
- 独立静态审查覆盖资料更新、令牌刷新、改密退出、工作台监听、重新登录链。
- 本轮未要求E2E，不启动服务、不写数据库、不进行真实账号改密。

## Current Status
completed：实现、定向验证、完整业务链独立复审、任务范围清理与授权集成完成。正式实现提交0e336ba8ff2acc9e5b638eb6bb880bd55296cbc2已正常推送codex/profile-session-boundaries并快进合入E:/IntRuoyi的本地int_main；本目录为最终任务记录。原独立worktree已保存可恢复归档快照，物理目录与Git登记均已移除，槽位7为inactive。主工作区6项并行改动融合前后SHA256一致，未纳入提交。67项定向测试、目标ESLint、3项SFC编译与独立复审通过；全量类型检查仍有3项既有错误，本轮未执行真实E2E。

## 设计约束检查
保留登录身份切换的缓存失效；无感刷新不能按新登录处理。保留权限、租户隔离、错误显式拒绝、旧会话结果写回守卫；不引入fallback，不改密码服务策略或F02数据查询。根AGENTS覆盖关联文档/技能默认BDD/TDD/E2E与Git完成门禁；本轮已获提交、修复分支推送与本地主干融合授权。

## 实现范围
- 生产仅auth.ts、Axios service.ts、ProfileWorkbench.vue：区分新登录与续期缓存边界；修正无refresh令牌401与登录页拒绝；阻止退出/卸载后的工作台请求和旧响应提交。
- 长期保留受控行为回归profile-session-boundaries-behavior.spec.cjs，并同步原auth刷新静态合同。
- docs/login-access.md合并可复用会话经验；任务记录为本目录5项核心文件。没有后端、业务数据库或运行配置改动。

## 授权集成
用户回复“授权”，对应提交本次修复、推送修复分支并合入本地int_main；只提交本任务文件，不含主工作区并行改动，不推送int_main已有其他提交。全量类型检查原有3项错误如实保留为验证限制，不扩展修复。未启动服务，槽位7已释放；D:/IntRuoyiTaskRuntime/profile-session-boundaries/verification保留审计证据。任务专有pnpm-virtual-store缓存删除被自动审批以“blocked by policy”拒绝，未执行删除或绕过；该外部缓存不在workspace清理计划内，当前保留，不影响已完成的代码融合与worktree清理。

## Cleanup Keep
- doc/tasks/20261008-profile-session-boundary-fixes/task-state.json
- doc/tasks/20261008-profile-session-boundary-fixes/review-report.md
