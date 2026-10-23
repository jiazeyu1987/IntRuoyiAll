# 最终本地 Git 收口范围

2026-10-03只读预检：主目录与整合目录当前HEAD均为 `a801dc8b9`；主目录实际分支 `int_qms`，整合分支 `codex/20261001-dcc-integration`。没有因目录名切分支，尚未提交或合入。

## 不纳入当前任务的改动

- 两树 `AGENTS.md` 是用户提供的分支/本机规则改动，当前目标要求只提交确认属于任务的资产，故保持原样并排除DCC实现提交，不用“全部脏改动基线”规则扩大纳入。
- 主目录infra `FileController.java` / `FileControllerTest.java` 当前git status有M，而正常git diff/stat无语义差异；无论是换行还是并行修改，未证明属于本目标。原始字节和状态单独保留，不重写、不删除、不加入DCC提交。
- 其他路径若不在正式交付manifest/必要整合修复/目标文档范围，也不默认收进提交。

## 当前任务候选资产

- 原A/B/C/D冻结并接收的DCC后端、前端、测试、前向SQL及主管理组合修复。
- 跨模块必须变更：BPM FormCenter真实会签义务与签名动作guard；System AdminUser DTO正式tenantId及其实际映射验证；GxP候选审计policy及合同。须逐个Review影响/测试，不因模块名不同就漏收，也不顺带收无关改动。
- 公共上传/详情/项目浏览/工作台接线与API、精确Long、关联/引用和生命周期展示，绑定最终types/lint/build及真实页面证据。
- 必须保留的主任务与子任务记录/最终报告/冻结指纹/复现脚本。临时日志、截图、dist/target等按cleanup规则归档摘要再清理，不能当作源码资产盲目stage。
- 最新v1.4需求与共同合同/归属/Review记录；主目录文档的最新内容覆盖整合树旧副本需要明确指纹和差异，不用整分支单侧覆盖。

## 提交和合入顺序

1. 子 Agent 停写且正式交付，核对最终资产指纹和所有Review/验收状态，完整目标未过不得宣告完成。
2. 只对已确认任务资产执行独立本地基线/实现/收尾提交，按用户最新目标保护无关脏改动。保存 staged 清单和提交号，不包含用户AGENTS或上述infra改动；不stash/drop/reset无关资产。
3. 主目录先保存确认属于主任务的文档资产，保留无关修改；核对目标分支、共同基线/最新origin只读状态与候选文件冲突，采用正常本地merge/必要人工冲突解决。用户明确本目标不要求push，不能将未推送列作阻塞。
4. 合入后执行来源祖先验证、冲突路径行为回归、类型/构建和branch-runtime-port-guard；记录本地int_qms最终提交号与剩余无关脏状态。
5. 只有全部业务及真实页面门禁、迁移配置执行状态和合入后证据齐全，才标主任务completed并update_goal complete。预算和反复接续不改变完成定义。

该文件是具体范围预检，不等于已经提交、推送或合并。原四worktree无需删除；没有获准删除其他任务资产。

## G36最新交付补充

当前G33／G34／G35及浏览入口夹具修正共17份源码／测试已对当前原始SHA核验：4份tracked修改，13份untracked，普通精确git add可纳入，git add -u会遗漏新增文件。无ignored生产／测试源码。此清单仅覆盖最新增量，不替代此前ABCD完整基线和共享测试SQL的归属证明。

7份既有永久真实UI runner／合同测试／共享helper被doc/tasks/**/*.cjs忽略，未来只对已审精确路径forceadd。3份G34延后、未验证脚本草稿即使在Cleanup Keep中仍排除实现提交；Keep仅保护不被清理，不代表通过Review。class／Jar／dist／target／原始日志及6份非任务资产排除，不能整目录add扩大范围。具体路径见整合后端子任务g36-current-git-source-scope.md；Root独立17项收据为g36-latest-source-scope-root-receipt.json。

两个工作区HEAD仍a801dc8、暂存0，端口守卫分别8061／48061与8067／48067通过。没有执行stage／commit／merge；新项目首次权限与实际运行验收尚未收口，不将有限Git范围检查作为合入条件完成。
