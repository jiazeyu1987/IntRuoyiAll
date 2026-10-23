# int_qms 提交与推送

## Goal
按用户“提交推送int_qms里的代码”，保存当前文控修复并推送 origin/int_qms。

## Milestones
1. 核对分支、远端、源码范围与本机私有产物。
2. 核验现有测试证据，执行提交静态校验，保存当前源码基线。
3. 独立记录收尾并推送，确认远端与本地一致。

## Expected Verification
分支 int_qms；精确暂存源码、测试、迁移和必要文档；排除日志、凭据、原始抓包、编译缓存。核对源码指纹与已有验证记录，执行适用静态/单元检查、diff-check、正常 Git hooks；确认 push 成功且远端 HEAD 一致。不运行 E2E、数据库写入、服务重启或发布。

## BDD
Given 当前 int_qms 有两笔未推送提交和后续文控改动；When 审核范围并提交推送；Then 源码保存在 origin/int_qms，运行产物保持本地，未闭环 E2E 不被改记为通过。

Given 详情生产组件已引入类型选项接口和审批进度提示 ref；When 旧单元测试装载真实组件或抽取真实方法；Then 测试宿主提供对应依赖，原有权限、迟到响应与提交断言全部实际执行，不修改生产行为、不删除断言。
RED: 54 个改动单元测试文件运行 496 项，477 PASS / 19 FAIL；三个测试宿主缺少 fileTypeTaxonomies 依赖或 approvalProgressNotice ref。完整输出保留本机 frontend-unit.log。后端 package（含 testCompile）和前端 vue-tsc 均退出 0。

## Current Status
completed — 源码、测试修正和组件测试入口已推送 origin/int_qms；实时远端与本地 1b7ef98ba 一致，未提交前后端源码为零。清理 preview/apply 退出 0；本任务收尾记录独立提交推送，最终 hash 以 Git 历史为准。

## 设计约束检查
- 用户当轮明确授权 commit/push；目标仅 int_qms，禁止 force push 或历史重写。
- 本任务不改业务实现；现有可提交代码作为独立基线保存，收尾文档另行提交。
- 历史/并行资产不删除、不回滚；不暂存含凭据的私有证据或运行日志。
- 经验沉淀优先复核已有 docs/experience-index.md 和 dcc-business-integration-experience.md；本机未找到 project-experience-consolidation 技能，按仓库现有经验门禁人工核对并记录，不伪称运行技能。

## Cleanup Keep
- doc/tasks/20261021-int-qms-commit-push/baseline-files.json
- doc/tasks/20261021-int-qms-commit-push/baseline-commit.json
- doc/tasks/20261021-int-qms-commit-push/fixture-fix-files.json
- doc/tasks/20261021-int-qms-commit-push/fixture-commit.json
- doc/tasks/20261021-int-qms-commit-push/fixture-entry-commit.json
- doc/tasks/20261021-int-qms-commit-push/backend-final-summary.json
- doc/tasks/20261021-int-qms-commit-push/final-source-fingerprints.json
- doc/tasks/20261021-int-qms-commit-push/whitespace-files.json
- doc/tasks/20261021-int-qms-commit-push/backend-package.log
- doc/tasks/20261021-int-qms-commit-push/backend-unit.log
- doc/tasks/20261021-int-qms-commit-push/backend-font-green.log
- doc/tasks/20261021-int-qms-commit-push/frontend-unit.log
- doc/tasks/20261021-int-qms-commit-push/frontend-fixture-green.log
- doc/tasks/20261021-int-qms-commit-push/frontend-fixture-green-2.log
- doc/tasks/20261021-int-qms-commit-push/frontend-fixture-green-3.log
- doc/tasks/20261021-int-qms-commit-push/frontend-regression.log
- doc/tasks/20261021-int-qms-commit-push/frontend-typecheck.log
- doc/tasks/20261021-int-qms-commit-push/baseline-commit.log
- doc/tasks/20261021-int-qms-commit-push/fixture-commit.log
- doc/tasks/20261021-int-qms-commit-push/fixture-entry-commit.log
- doc/tasks/20261021-int-qms-commit-push/cleanup-preview.log
- doc/tasks/20261021-int-qms-commit-push/cleanup-apply.log
- doc/tasks/20261021-int-qms-commit-push/source-push.log
- doc/tasks/20261021-int-qms-commit-push/closeout-commit.log
- doc/tasks/20261021-int-qms-commit-push/closeout-push.log

日志仅本地保留，用于区分初始失败与最终通过；不暂存、不推送。收尾只清理本目录的重复候选清单，不清理其他任务资产。
