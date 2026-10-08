# UM-05 密码重置与旧会话撤销

## Task Goal

按重要性排列用户管理修复，仅在独立 worktree 开发第一个 UM-05；静态代码验证、定向回归和真实前端 E2E 全通过才合入 int_main。

## Scope And Authorization

- 用户当轮明确授权独立 worktree 开发、测试、真实 E2E 及通过后的主代码融合；必要的任务实现/收尾本地提交及合并属于融合工作，不推送、不发布、不重启主服务。
- 只修 UM-05/AC-05-01～04 和必需 S-01 会话安全前提；不实施 UM-02～04/06～10，不修改历史身份 UM-01、不做 schema 迁移。
- 本地任务后台独立端口；真实页面只修改任务自有账号数据，不直接 API/DB 写入、不改共享管理员密码或权限基线。
- 来源为 doc/tasks/20261006-user-management-remediation-plan v3。基线 f68e333e418e0873759a943ff681f8410b5aabb1；独立 worktree 为 E:/IntRuoyiWorktree/user-password-session/IntRuoyi，分支 codex/user-password-session，slot9，8090/48090。用户已委托技术选择，正式库权威的共同校验取舍记录于实施合同，不以此前文档评审代替本次代码放行。

## Priority Ranking

| 优先级 | 问题 | 理由 |
| --- | --- | --- |
| 1 | UM-05 密码泄露/重置未撤销旧会话 | 直接凭据与会话安全风险，明确业务口径 |
| 2 | UM-02 导入绕过生命周期停用 | 可重新启用应停用账号 |
| 3 | UM-01 历史身份展示依赖当前账号 | 受控事实风险，治理/保留/来源批准未齐 |
| 4 | UM-08 钉钉组织权限 | 跨组织写入风险，权限口径仍待确认 |
| 5 | UM-04 停用岗位绑定隐式删除 | 资料保存改变正式关联 |
| 6 | UM-03 查询依赖管理权限/N+1 | 查询角色无法正常使用列表 |
| 7 | UM-07 导入自身联系方式误冲突 | 正常覆盖被拒 |
| 8 | UM-06 解锁缺原因 | 功能不可执行 |
| 9 | UM-10 字段/筛选体验 | 非阻塞体验 |
| — | UM-09 转岗停用规则 | 非缺陷，只保留回归 |

## Milestones

- [x] M0：独立 worktree、端口/依赖/工具前置与原问题核验（业务运行待 M2）。
- [x] M1：UM-05/S-01 最小代码及对应定向回归。
- [x] M2：静态验证、代码评审、真实页面 E2E。
- [ ] M3：仅通过后清理、任务提交、融合主代码及最终验证。

## Expected Verification

- 重置输入遮蔽/成功无秘密，LogRecord 无明文或哈希/完整用户对象。
- 重置与自助改密密码/历史/凭据状态/全部 access+refresh DB 撤销同事务，缓存残留不能授权；旧认证与刷新并发不能复活会话。
- ADMIN 正式租户/人员与机器/MEMBER 边界、登录返回改密标记保持，错误不伪成功。
- 重置他人保留操作者；重置本人/自助成功本地清理后重新登录；取消、失败与成功后刷新失败区分。
- 静态检查、实际生产 handler、后端定向回归与真实页面 E2E；不要求无关全量/其它修复/E2E，不将未执行记 PASS。
- E2E 全部业务动作由 Playwright 真实前端完成，接口仅只读核验，不通过 API 生成 fixture 或操作。

## 设计约束检查

- 已读 AGENTS、task-closeout-rules、worktree/branch-port、local-runtime、E2E/login、前后端规则；最近根规则优先，BDD/TDD非强制默认，仍做对应回归。
- Windows PowerShell/UTF-8，无 &&，Maven -D 单个完整参数；无 fallback/降级/吞异常。
- 保留并行未提交改动，不提交/清理无关资产；不启动发布/远端操作。

## Current Status

ready_for_closeout — M2全部实际PASS：后端257、前端50、Java17标准31模块package、静态及完整ESLint、真实Playwright密码/双会话/唯一提交、MySQL旧RR生产服务。测试账号已UI删除且活动令牌0，临时产物及任务运行实例清理已完成。M3提交/融合/归档被失效Git锁阻塞；用户已授权清理，但自动工具策略仍拒绝确切文件删除，需外部移除锁后继续。不push、不启停主48081。

## Cleanup Keep

- doc/tasks/20261006-user-password-session-fix/task-state.json
- doc/tasks/20261006-user-password-session-fix/implementation-contract.md
- doc/tasks/20261006-user-password-session-fix/integration-file-manifest.json
- doc/tasks/20261006-user-password-session-fix/latest-backend-regression.json
- doc/tasks/20261006-user-password-session-fix/runtime-build-evidence.json
- doc/tasks/20261006-user-password-session-fix/runtime-readiness-evidence.json
- doc/tasks/20261006-user-password-session-fix/e2e-manifest.json
- doc/tasks/20261006-user-password-session-fix/e2e-runtime-evidence.json
- doc/tasks/20261006-user-password-session-fix/mysql-service-rr-evidence.json
- doc/tasks/20261006-user-password-session-fix/ui-cleanup-readonly-evidence.json
- IntRuoyiFronted/tests/e2e/system-user-password-session-behavior.spec.cjs
