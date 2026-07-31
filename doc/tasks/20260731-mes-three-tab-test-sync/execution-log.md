# Execution Log

## 2026-07-31

- USER INTENT: 将本机 `tenant_id=1 / 芋道源码` 的工序设置、工艺流程、排产工单三页签数据同步到测试服务器同租户，其他数据不同步。
- POLICY: 仅允许白名单三页签数据同步；缺失依赖、schema 差异、白名单外活动引用或校验失败必须 fail fast。
- GIT PRECHECK: `git status --short --branch --untracked-files=all` -> 当前分支 `int_main` 干净但领先 `origin/int_main` 1 个提交；最近提交 `6a1390ff` 为并发 Runner 修复基线，不属于本任务实现。
- BDD: 缺失依赖零写入 -> Given 测试服缺少三页签数据运行所需的正式依赖 / When 执行同步 preflight / Then 同步工具必须阻塞并保持测试服三页签和白名单外数据零写入。
- BDD: 外部引用零破坏 -> Given 测试服白名单外业务表仍引用旧工序、路线或排产记录 / When 用户要求只同步三页签 / Then 工具必须阻塞，不得删除或改写仍被引用的记录。
- BDD: 精确替换 -> Given 依赖完整、schema 对齐、外部引用为零且备份成功 / When 执行同步 / Then 只替换白名单表中 `tenant_id=1` 的有效三页签数据，且白名单外数据 hash 不变。
- BDD: 失败回滚 -> Given 数据替换事务中任一行数、主键、业务键或 hash 校验失败 / When 提交前校验执行 / Then 事务必须回滚并保留失败证据和恢复路径。
- GREEN: experience-preflight -> PASS，已读取 `docs/experience-index.md` 并命中测试服发布、远端 MySQL、release migration、工艺路线导入完整性、排产数据包和 Git 并发基线门禁；适用摘要已补入 `task.md`。
- RED: pending -> 尚未运行同步 preflight；预期当前测试服仍因缺失依赖和外部引用阻塞。
