# 一键式无数据程序发布 P1

## Task Goal

在当前 `int_main` 基线上完成且仅完成 P1 发布合同收敛：固定标准程序包为 `app-release`，冻结两个 Git root 与三个 source role，明确客户端与服务端参数所有权，并建立构建前测试顺序、二次源码冻结和三语言双摘要合同。

## Milestones

- [x] P1-M1：建立维护仓与 IntRuoyi 根仓两个独立任务 worktree，并审查验证线差异。
- [x] P1-M2：先补 T01-T03/T15 失败测试并记录 RED。
- [x] P1-M3：最小实现 P1-AC1 至 P1-AC5 合同并记录 GREEN。
- [x] P1-M4：运行 P1 相关回归、端口 guard、staged 清单和 diff-check，并分别提交任务分支。

P2-P5 不在本次执行范围，不实现完整状态机、持久化 workflow、环境锁、发布 UI 或真实部署。

## Expected Verification

- Maven 定向合同：`ReleaseWorkflowContractTest`、`ReleaseSourceFreezeTest`、`ReleaseWorkflowGateOrderTest`、`ReleaseDigestVectorTest`。
- 前端静态合同：客户端请求不能提交 releaseTag、publishScope、host、NAS、MinIO、repo roots 或 LocalCacheRoot。
- 维护脚本合同：标准范围固定 `app-release`；唯一权威 `manifest.json`；构建前和打包前二次 Git 冻结；后端、前端、脚本测试先于昂贵构建。
- Python、Java、PowerShell 固定双摘要向量一致；反斜杠路径和大小写折叠冲突拒绝。
- 当前 app worktree 的端口 guard、任务 owned diff-check 和 Git staged 清单通过。

## 经验门禁

- Trigger: 标准发布合同。Preflight check: 固定 `app-release` 且数据备份/恢复不进入 workflow。Blocker: 客户端可选 `with-data` 或传基础设施参数。Verification: Java/前端/脚本合同。Forbidden action: 兼容读取第二份 manifest。Evidence: `execution-log.md`。
- Trigger: 来源冻结。Preflight check: 维护仓与 IntRuoyi 根仓分别记录批准 commit，maintenance/backend/frontend 三个 role 可追溯。Blocker: HEAD 漂移或 dirty。Verification: 构建前、打包前各校验一次。Forbidden action: 使用主工作区未提交内容。Evidence: `execution-log.md`。
- Trigger: 进入昂贵构建。Preflight check: 后端、前端、脚本测试已全部 PASS。Blocker: 任一失败或跳过。Verification: 命令顺序合同。Forbidden action: 先 Maven package、前端 build 或 Docker build。Evidence: `execution-log.md`。
- Trigger: 计算制品摘要。Preflight check: 路径规范化、大小写折叠唯一、原始 bytes SHA-256、固定 UTF-8/LF 序列。Blocker: 三语言结果不同或非法路径。Verification: 固定向量。Forbidden action: 语言/区域相关排序或路径兼容降级。Evidence: `execution-log.md`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以单一合同、服务端参数所有权和可执行门禁收敛发布输入。
- 是否存在临时补丁或绕过：否；验证线变更按当前主线逐文件融合，不整链 cherry-pick。

## Current Status

in_progress

P1 已完成。主线程后续进入 P3 后发现应用仓 `release_preflight_plan.py` 尚未接受标准 `app-release` scope，`20260629_mes_smart_scheduling_role_scope` 仍硬编码旧路线菜单 `900121/900122`，R31 包内 `20260718_mes_puhui_schedule_admin_role_visibility.sql` 缺少 target preflight 与当前 `900120` 父级兼容逻辑，R38 测试服部署在版本切换前失败于 `20260811_mes_process_pool_cleaning_process_parameter_data.sql` 的空规则硬阻断，R40 失败于光固 I/II 空来源硬阻断，R43 失败于 C00 回填旧表/派生文本排序规则冲突。本 worktree 已补齐 `app-release` 迁移预检合同、活跃路线菜单解析、璞慧排产管理员菜单父级兼容、清洗工序参数空规则 no-op、光固 I/II 空来源 no-op、C00 文本比较显式 collation 和目标只读预检合同；本机 RED/GREEN 与相邻回归已通过。R43 判废不复用，等待主线程使用新的应用提交继续生成 `release-20260915-one-button-app-r44` without-data/app-release 程序包。服务器、NAS、数据库、MinIO、服务启动和真实 E2E 仍未在本应用 worktree 内执行。
