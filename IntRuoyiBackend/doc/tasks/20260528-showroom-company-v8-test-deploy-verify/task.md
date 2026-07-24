# 任务：发布并验证公司版本 V8 查看修复

## 任务目标

- 响应浏览器仍报 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9` 的反馈。
- 确认测试服是否仍运行旧后端代码。
- 在不修改测试服数据库、不同步本地数据库、不改写 `芋道源码` 租户业务数据的前提下，将公司版本 V8 查看修复发布到测试服并验证。

## BDD 场景

- BDD: 测试服代码发布后公司 V8 可查看 -> Given 测试服 `芋道源码/admin` 查看公司历史 V8 / When 前端调用 `/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8` / Then 接口不应返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`，而应返回 V8 详情并暴露 V9 当前内容阻塞项。
- BDD: 发布不得覆盖测试服业务数据 -> Given 测试服数据库存在 V9 双语讲解重复候选 / When 发布本次代码修复 / Then 不执行数据库同步、不清空远端 MySQL、不写入 `showroom_version_bundle` 或讲解版本数据。

## 里程碑

- [x] M1：只读复现测试服当前仍报旧错误。
- [x] M2：确认发布脚本参数，采用 `-SkipDatabaseSync -SkipMinioSync` 代码发布路径。
- [x] M3：执行测试服代码发布。
- [x] M4：用真实 `芋道源码/admin` API 或前端路径验证 V8 详情不再报错。
- [x] M5：记录验证证据，执行 task-closeout-cleanup 预览并按策略提交任务记录。

## 预期验证

- RED: 发布前 API 返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`。
- GREEN: 发布后 API 返回 `code=0` 或等效成功结构，`selectedVersion.revisionId=8`，`targetSummary.currentContentRevisionId=9`，并包含 `CURRENT_CONTENT` blocker。
- GREEN: 远端 MySQL 数据未因发布被重置；不执行数据库同步命令。

## 当前状态

completed

## 当前发现

- 上一代码修复已在后端仓库提交：`c716918154 任务: 修复公司版本V8 bundle缺失查看报错`。
- 当前主仓库存在其他未提交/已暂存改动，均不属于本任务；为避免把无关 runtime-control 改动发布到测试服，本次使用干净成对 worktree `D:\ProjectPackage\Int\IntRuoyi-company-v8-deploy` 发布。
- 只读复现：测试服 `GET /admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8&siteKey=yingtai-showroom&stage=TEST` 仍返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`，说明浏览器正在命中未发布修复的后端。
- 发布脚本支持 `-SkipDatabaseSync -SkipMinioSync`，可避免同步数据库和 MinIO。
- 已按用户“帮我添加”将远端现有 `/opt/intruoyi/runtime/.env` 中的 DCC 签名证据配置写入 Windows User 环境变量；未在日志中输出密钥值。
- 本次发布命令均使用 `-SkipDatabaseSync -SkipMinioSync`，未同步数据库、未同步 MinIO、未写入 `showroom_version_bundle` 或讲解版本数据。
- 测试服最终发布镜像 tag：`20260528_company_v8_auxfix_c716918154`。
- 真实 `芋道源码/admin` 接口验证：V8 详情返回 `code=0`，`selectedVersion.revisionId=8`，`selectedVersion.revisionNo=8`，`targetSummary.currentContentRevisionId=9`，`currentContentVersion=null`；原始顶层错误 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9` 已消失。
- 当前 V9 仍有业务数据阻塞：`CURRENT_CONTENT` 返回双语讲解重复候选 blocker，`CURRENT_RELEASE` 返回 `SHOWROOM_TARGET_NOT_FOUND` blocker；这些阻塞被正常暴露，不再阻断历史 V8 查看。
- 主仓库补充了辅助快照诊断回归：当当前内容 revision 指向不存在数据时，选中的历史公司版本仍可读，缺失项进入 scoped blocker。
