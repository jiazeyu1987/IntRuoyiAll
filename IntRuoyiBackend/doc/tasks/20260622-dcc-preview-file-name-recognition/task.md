# 任务：DCC 受控预览文件名识别回写（发布隔离 worktree）

## 任务目标

让 DCC 受控预览里的“识别基础信息”能够把识别到的 DCC 基础数据项目名称同步回写为受控文件 `fileName`，并保持 `title` 与主链 `dcc_controlled_file_master.file_name` 一致；同时保证测试服后端最小发布链路可正式承载该修复。

## 用户要求与执行边界

- 用户要求：
  - DCC 受控预览里可以识别文件名。
  - 识别出的文件名必须是 DCC 基础数据里的项目名称。
  - 本机没有 DCC 文件，可以结合测试服务器真实文件情况选择最适合的方式。
  - 可以通过识别编码间接识别项目名称，并先采样几十个文件寻找规律。
- 本任务边界：
  - 仅处理本任务 DCC 后端修复与测试服最小后端发布链路。
  - 不新增 fallback、mock 成功值或静默降级。
  - 不触碰正式服。
  - 若发布脚本或真实环境前置缺失，必须明确记录 blocker，不得伪装为功能已完成。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- 本任务强制门禁摘录：
  - 测试服高风险动作仅限 `172.30.30.58`，不得误触正式服。
  - worktree 发布必须使用隔离 worktree，不能混入主工作区未提交改动。
  - 真实发布、服务器写入与联调前，必须在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS` 或明确 blocker。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。文件名唯一命中项目编码或项目名称时都走正式识别规则；发布脚本缺失依赖 SQL 时补正式发布清单，不绕过门禁。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 项目编码识别后文件名同步为项目名称 -> Given 源文件内容或源文件名能稳定命中 DCC 基础数据中的项目编码 / When 文控在 DCC 详情或受控预览执行基础信息识别 / Then 后端必须把匹配到的项目名称同步回写到受控文件 fileName、title 和主链 file_name。`
- `BDD: 源文件名仅包含唯一项目名称时直接回写项目名称 -> Given 源文件名未命中任何项目编码但唯一命中 DCC 基础数据项目名称 / When 文控执行基础信息识别 / Then 后端必须在读取文件内容或调用 Codex 之前直接锁定该项目并回写 fileName、title、productName、productCode 与主链 file_name。`
- `BDD: 测试服最小后端发布不得因漏带依赖 SQL 被误阻塞 -> Given 本次发布为测试服 backend code-only 发布 / When 发布包清单包含 showroom 显式 SQL / Then 迁移策略门禁必须能解析显式依赖链并允许继续构建发布包。`

## 里程碑

1. 为隔离 worktree 建立任务文档并记录发布前置。`DONE`
2. 修复 DCC 识别服务与回归测试。`DONE`
3. 修复发布脚本所需正式 SQL 清单并通过静态验证。`DONE`
4. 用隔离 worktree 完成测试服最小后端发布。`DONE`
5. 真实接口/页面复验项目名称回写并收尾。`DONE`

## 预期验证

- `mvn -pl yudao-module-dcc -am -Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccProjectCodeCodexCliClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q`
- 测试服真实接口：`POST /admin-api/dcc/controlled-files/2054545668044050095/recognize-project-code`
- 必要时真实页面复验受控预览显示结果。

## 当前状态

COMPLETED：隔离 worktree 已完成“文件名唯一项目名称直连识别”修复，并以 `20260622_dcc_preview_codex_release_fix_3` 成功重新构建、发布到测试服 backend。真实接口 `POST /admin-api/dcc/controlled-files/2054545668044050095/recognize-project-code` 已返回 `code=0`，命中 `dcc_project_code.id=1 / projectCode=PTCABC / projectName=PTCA球囊扩张导管 / matchType=PROJECT_NAME`；随后测试服真实数据库回读已确认 `dcc_controlled_file.file_name`、`title`、`product_name`、`product_code` 与 `dcc_controlled_file_master.file_name` 全部同步为正确项目名称/编码。进一步地，本任务提交已成功迁入新的 `int_main` 干净集成 worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean` 并完成回归验证，随后又已把原始主工作区 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 改挂到 holding 分支 `codex/20260622-ruoyi-vue-pro-int-main-hold`，从而让真正的 `int_main` 分支在干净 worktree 上快进到本任务结果。当前这次 DCC 修复已经按 `int_main` 方向完成收口；原始主工作区剩余未提交内容继续保留在 holding 分支上，待后续单独整理。本任务未触碰正式服。
