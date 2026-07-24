# 任务：展厅版本中心融合到 int_main

## 任务目标

- 将 `task/20260523-showroom-version-center-impl` 后端版本中心实现融合到 `int_main` 基线。
- 在不回滚、不覆盖主工作区现有未提交改动的前提下，先完成可安全复用的代码层融合与回归验证。
- 若主工作区脏状态阻止移动 `int_main` 指针，必须显式记录阻塞、影响与下一步。

## 非目标

- 不处理与版本中心无关的主工作区未提交改动。
- 不通过 stash、reset、checkout 覆盖用户当前在 `int_main` 工作区的本地修改。
- 不引入 fallback、mock 成功或静默跳过验证。

## 前序任务检查

- 已检查前一任务 `doc/tasks/20260523-showroom-version-center-implementation/task.md`
- 前一任务状态：`已完成`
- 当前可直接基于已放行实现继续做融合处理

## 里程碑

- [x] M1：建立融合任务包与执行日志。
- [x] M2：在后端 worktree 分支吸收 `int_main` 最新已提交历史并解决冲突。
- [x] M3：完成后端受影响验证并记录证据。
- [ ] M4：尝试推进后端 `int_main`，若阻塞则记录精确原因与影响。

## 预期验证

- `BDD: <scenario> -> Given/When/Then`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `REGRESSION: <command> -> PASS`
- 至少覆盖：
  - `int_main` 已提交历史吸收后的编译/回归
  - 版本中心关键后端 focused tests
  - 若无法推进 `int_main`，记录具体未提交冲突文件与影响

## 当前状态

- 状态：阻塞
- 已完成：
  - 已确认主工作区 `int_main` 存在与本任务重叠的未提交改动，不能直接安全合并
  - 已确认当前分支相对 `int_main` 非快进：`git rev-list --left-right --count int_main...task/20260523-showroom-version-center-impl -> 9 1`
  - 已在后端 worktree 分支完成 `int_main` 已提交历史吸收，并解决 `ShowroomNarrationVersionMapper` 冲突
  - 已继续吸收后续新增的 `int_main` 提交（包括 `a0afc5b6f9`、`d01a1bddd9`），并解决 `ShowroomSchemaMapperContractTest` 冲突
  - 已修正版本中心在当前主系统 release 逻辑下的 `currentPublicRevisionId` 推导，兼容产品 release 直接走 `cover_image` 而非 preview asset 的场景
  - 已对齐主系统测试基座变化：
    - 共享 release fixture 恢复产品 preview asset 前置条件
    - 公司历史 `cover_image` 断言改为当前 infra 正式编码 URL 契约
  - 已完成后端验证：
    - `mvn -pl yudao-module-showroom -DskipTests compile` -> PASS
    - `javac @yudao-module-showroom/target/javac-version-center.args && java @yudao-module-showroom/target/java-version-center.args` -> PASS（30 tests）
    - `python -m pytest script/tests/test_showroom_version_center_sql.py -q` -> PASS（2 tests）
- 待完成：
  - 在主工作区 `int_main` 清理或提交重叠未提交改动后，再执行真正的分支前推
- 阻塞与影响：
  - 主工作区 `int_main` 脏状态与本任务存在重叠文件；若直接在主工作区 merge，会污染用户未提交改动或触发冲突
  - 当前只能完成“代码层融合 + 已验证 merge 提交”；不能在不处理主工作区未提交改动的前提下安全移动 `int_main` 指针
