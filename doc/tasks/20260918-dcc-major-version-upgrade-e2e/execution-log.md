# 20260918 DCC 大版本升级完整 E2E Execution Log

## Rule Reads

- 已读取 `AGENTS.md`。
- 已读取 `docs/e2e-rules.md`、`docs/database-rules.md`、`docs/task-closeout-rules.md`、`docs/local-runtime.md`、`docs/branch-runtime-ports.md`、`docs/worktree-restrictions.md`、`docs/login-access.md`。
- E2E 仅通过真实 Playwright 页面完成业务动作；API/数据库只用于网络证据、schema 迁移和最终只读核验。

## BDD / RED / GREEN

- BDD 已记录于 `task.md`：ACTIVE A/1 经真实页面检出、检入、DCC 审批/签名后形成 B/1 ACTIVE，旧版 SUPERSEDED。
- RED-1：`48061` 初始缺少 EDHR 进程配置，后端按 fail-fast 规则启动失败。
- GREEN-1：通过任务脚本只向任务自有 `int_qms` 后端子进程注入临时 issuer 和随机 signing secret，health HTTP 200，真实登录成功。
- RED-2：真实受控浏览查询发现正式 DCC 生命周期字段/表未执行，先后暴露缺少 `predecessor_controlled_file_id`、`system_electronic_signature`、GxP 策略表、发布后续表和关联文件表。
- GREEN-2：在用户授权下执行正式迁移；每条迁移均通过 MySQL stdin 执行并做真实 schema 只读复核。
- RED-3：盖章 PDF 上传返回业务码 `1080000108`，消息为 `DCC upload size policy is missing or invalid`。
- GREEN-3：增加任务专用幂等 `APPROVAL_PDF` 上传策略种子，真实页面上传返回 HTTP 200/业务码 0。
- RED-4：大版本脚本遇到 Element Plus radio/字段定位和详情路由守卫问题。
- GREEN-4：仅修正任务 Playwright 定位和真实按钮同等路由参数；未绕过页面业务动作。
- GREEN：真实页面完成 A/1 -> B/1 检出、检入、上传、提交、四节点审批/签名，最终详情与受控浏览均确认 B/1 ACTIVE。

## Runtime And Scope

- 分支：`int_qms`。
- 前端：`http://127.0.0.1:8061`；后端：`http://127.0.0.1:48061`。
- `48081` 未使用、未停止、未重启。
- 运行态使用既有 QMS 前后端；本任务未重启既有后端。
- 真实账号：租户 `芋道源码`，用户 `admin`。密码只通过当前进程环境注入，未写入报告或 artifact。
- 未启用子 Agent，未执行 Git commit/push，未操作远程服务器。

## Environment And Task-Owned Data

- 任务自有 DCC 项目：项目 ID `256`，项目代码 `CODEX-DCC-PROD-20260918234417`，产品/项目名称 `CODEX DCC产品E2E 20260918234417`。
- 任务自有模板：`CODEX-DCC-MAJOR-20260918234457.docx`。
- 类别/目录：`技术调研报告` -> `质量管理/4.Ohter`；证据见 `artifacts/category-directory-binding.json`。
- 临时上传权限：仅测试租户 admin 的 GLOBAL UPLOAD 规则，备注 `CODEX临时E2E上传授权`；通过真实配置包导入页面完成。
- 临时四节点审批路线：文控审核、审核会签、批准、文控批准，当前节点均解析到测试用户；证据见 `artifacts/temp-four-stage-route-real.json`。
- 临时 admin 电子签名图片通过真实签名治理页面上传并启用；证据见 `artifacts/temp-admin-signature-real.json`。
- 临时 EDHR 值仅为进程环境变量，不持久化。

## Formal Migrations And Authorized Seeds

- 正式执行并复核：
  - `20260906_dcc_new_file_lifecycle_p1.sql`
  - `20260906_dcc_new_file_lifecycle_p2.sql`
  - `20260906_dcc_new_file_lifecycle_p3.sql`
  - `20260906_dcc_new_file_lifecycle_p4.sql`
  - `20260719_business_approval_policy.sql`
  - `20260719_dcc_upload_form_policy_seed.sql`
  - `20260720_dcc_publish_form_policy_seed.sql`
  - `20260908_system_electronic_signature_t3.sql`
  - `20260908_system_electronic_signature_subject_id_capacity.sql`
  - `20260908_system_electronic_signature_t7.sql`
  - `20260908_system_electronic_signature_t8.sql`
  - `20260908_gxp_audit_trail_core.sql`
  - `20260903_dcc_controlled_file_related_file.sql`
  - `20260907_dcc_publication_followup.sql`
  - `20260907_dcc_publication_impact_assessment.sql`
  - `20260907_dcc_publication_notification.sql`
- 用户授权的任务种子：
  - `artifacts/gxp-audit-policy-seed.sql`
  - `artifacts/dcc-approval-pdf-upload-policy-seed.sql`
- GxP coverage gate：`PASS operations=8 annotations=7`。
- `APPROVAL_PDF` 策略只针对租户 1，10 MB，启用、无有效期，policy code `CODEX_DCC_APPROVAL_PDF_TEMP_V1`。
- 迁移前完整数据库备份：`output/runtime/int_qms/db-backups/dcc-major-version-upgrade-pre-20260919-021704.sql`。
- 迁移前 DCC 定向快照：`artifacts/dcc-file-directory-pre-unclassified-20260919-052100.sql`。
- 未将未完成的全库 dump 声明为成功；全库 dump 因库体量约 8.4 GB 被取消。

## Initial Version Through Real Page

- 真实页面创建并准备项目/模板后，上传唯一源文件成功。
- 文件编号：`E2E-DCC-20260918234556`。
- 初始文件 ID：`2054545668044083983`。
- 初始版本：`A/1`。
- 初始上传和三次审批中心审核均 HTTP 200/业务码 0。
- 最终文控批准页面上传唯一盖章 PDF：
  - POST `/admin-api/dcc/controlled-files/upload-preview` -> HTTP 200，业务码 0。
  - POST `/admin-api/dcc/controlled-files/2054545668044083983/approve-task` -> HTTP 200，业务码 0。
- 初始版本只读结果：`ACTIVE`，`stamped_file_id=9198354931069`，发布后续可见。
- 证据：`artifacts/initial-approval-real.json`、`artifacts/initial-approval-step-1.png`。

## Major-Version Real Page Run

- 首次完整 run：`DCC-MAJOR-20260919002601`。
- 页面步骤和真实请求：
  - 登录：POST `/admin-api/system/auth/login` -> HTTP 200/业务码 0。
  - 检出：POST `/admin-api/dcc/controlled-files/2054545668044083983/checkout` -> HTTP 200/业务码 0。
  - 新源文件预览上传：POST `/admin-api/dcc/controlled-files/upload-preview` -> HTTP 200/业务码 0，状态 `AVAILABLE`。
  - 大版本检入：POST `/admin-api/dcc/controlled-files/2054545668044083983/checkin` -> HTTP 200/业务码 0，生成 `B/1`、文件 ID `2054545668044083984`。
  - 提交 DCC 审批：POST `/admin-api/dcc/controlled-files/2054545668044083984/submit` -> HTTP 200/业务码 0。
  - 三个审批中心审核节点：POST `/admin-api/approval-center/tasks/review` -> 3 次 HTTP 200/业务码 0。
  - 最终文控签名节点盖章 PDF 上传：POST `/admin-api/dcc/controlled-files/upload-preview` -> HTTP 200/业务码 0。
  - 最终文控签名：POST `/admin-api/dcc/controlled-files/2054545668044083984/approve-task` -> HTTP 200/业务码 0，`nextStatus=ACTIVE`、`evidenceStatus=VALID`。
- DCC 审批完成后当前发布策略自动完成受控副本与发布后续；真实详情页没有单独的“生效申请”待办。
- 结果截图：`artifacts/99-final-detail.png`。
- 结果 JSON：`artifacts/real-e2e-result.json`。该文件为最后一次真实页面复验结果，复验从已完成的 B/1 状态进入详情页，避免重复制造 C/1。

## Final Read-Only Verification

- `dcc_controlled_file`：
  - ID `2054545668044083983`：`SUPERSEDED`，`A/1`，`superseded_by_file_id=2054545668044083984`。
  - ID `2054545668044083984`：`ACTIVE`，`B/1`，`predecessor_controlled_file_id=2054545668044083983`。
  - B/1 `approved_time=2026-09-19 08:26:45`，`published_time=2026-09-19 08:26:44`，`stamped_file_id=9198354931072`。
- 真实详情页：
  - 头部：`当前有效版 / ACTIVE / B/1`。
  - 版本历史：同时显示 B/1、A/1、`已替代`、`DCC-MAJOR-20260919002601 大版本升级正文变更`。
  - 完整时间线：B/1 正式生效、受控副本生成、审批流程完成；A/1 已替代。
  - 发布后续：页面显示 `已完成`。
- 真实受控浏览页：
  - 默认当前有效版本为 `B/1`。
  - 发布文件、盖章文件均显示 `已生成`。
- 任务脚本最终复验：`PASS`，阶段 `final-verification` 与 `complete` 均通过。

## Closeout Gate

- E2E 业务验证已完成，任务状态先置为 `ready_for_closeout`。
- cleanup preview：使用 bundled Python 执行 `task_closeout.py --task-id 20260918-dcc-major-version-upgrade-e2e --mode preview`，返回 `status=ready_for_closeout`，关键任务文档/脚本/JSON/截图在 keep，临时截图、临时源文件、配置包和迁移日志在 delete，warnings/blockers 均为空。
- cleanup apply：同一脚本返回 `exit=0`，仅删除本任务目录内 preview 列出的临时产物。
- 最终只读恢复复验再次通过：`real-e2e-result.json` 为 `PASS`，`approval-tasks` 记录为 `INFO/resume-existing-completed-flow`，无 `BLOCKED` 阶段；复验使用已存在的仓库 fixture，不重复创建 C/1。
- 未执行 Git commit 或 push：工作区存在大量并行改动，本轮未获得将其作为基线提交并推送的明确授权；任务保持 `ready_for_closeout`，不伪造 `completed`。

## 2026-09-19 Rerun By Current User Request

- 用户当轮明确要求“进行E2E完整走一遍升级大版本流程”，因此重新执行真实 Playwright 页面链路。
- 前置：默认 Playwright 浏览器缓存缺少 `chromium_headless_shell-1223`，首次启动未进入页面即失败；改用本机已安装 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 作为 Playwright `executablePath`，未改变业务动作，未改用 API。
- 运行命令：`node doc\tasks\20260918-dcc-major-version-upgrade-e2e\dcc-major-version-upgrade-real.e2e.cjs`，环境变量限定 `DCC_MAJOR_VERSION_UPGRADE_E2E_BASE_URL=http://127.0.0.1:8061`、目标文件号 `E2E-DCC-20260918234556`、租户/账号 `芋道源码/admin`。
- 本轮真实页面链路：受控浏览选择当前有效 `B/1` -> 页面检出 -> 检入弹窗选择“大版本” -> 上传新版源文件 -> 填写修改说明和检入备注 -> 生成 `C/1` -> 页面提交 DCC 审批 -> 审批中心三节点审核 -> DCC 详情最终文控签名并上传盖章 PDF -> 发布后续自动完成。
- 本轮没有直接调用任何 API；以下仅为 Playwright 监听到的真实前端页面自然触发请求，用作被动证据：
  - `POST /admin-api/dcc/controlled-files/2054545668044083984/checkout` -> HTTP 200 / code 0。
  - `POST /admin-api/dcc/controlled-files/upload-preview` -> HTTP 200 / code 0。
  - `POST /admin-api/dcc/controlled-files/2054545668044083984/checkin` -> HTTP 200 / code 0，生成 `C/1`、文件 ID `2054545668044083987`。
  - `POST /admin-api/dcc/controlled-files/2054545668044083987/submit` -> HTTP 200 / code 0。
  - `POST /admin-api/approval-center/tasks/review` -> 3 次 HTTP 200 / code 0。
  - `POST /admin-api/dcc/controlled-files/upload-preview` -> HTTP 200 / code 0，最终盖章 PDF 上传。
  - `POST /admin-api/dcc/controlled-files/2054545668044083987/approve-task` -> HTTP 200 / code 0，返回 `nextStatus=ACTIVE`、`evidenceStatus=VALID`。
- 页面终态：详情页显示 `当前有效版 / ACTIVE / C/1`；版本历史显示 `C/1` 当前有效、`B/1` 已替代、`A/1` 已替代；生命周期显示 `C/1` 受控副本已生成、正式生效、审批流程已完成。
- 受控浏览终态：默认当前有效版本为 `C/1`，发布文件和盖章文件均显示已生成。
- 证据文件：`artifacts/real-e2e-result.json` 状态 `PASS`，`final-verification` 和 `complete` 均为 `PASS`；`artifacts/99-final-detail.png` 已更新为本轮最终详情截图。
- 合规边界：本轮所有业务写入、上传、检入、提交、审批、签名和最终查看均由真实前端页面完成；未使用 `fetch`、Playwright `APIRequestContext`、后端 HTTP 客户端、SQL/数据库写入或任何脚本直调 API 代替页面操作。
- 口径说明：脚本在自动发布完成后第二次调用审批中心轮询，因已无待办记录了一个 `approval-tasks BLOCKED processed=0` 中间阶段；随后详情页和受控浏览最终核验均 `PASS`，整体 `status=PASS`。该 `BLOCKED` 表示“无额外生效申请待办”，不是业务阻塞。
