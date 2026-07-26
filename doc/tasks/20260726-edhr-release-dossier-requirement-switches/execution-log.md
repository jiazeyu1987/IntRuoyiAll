# eDHR 放行资料限制开关 Execution Log

## User Intent

实现 eDHR 放行资料限制开关计划：个人中心配置页签新增来料检报告、灭菌报告、成品检报告、成品检记录限制 4 个金手指开关；默认关闭，开启后放行必须校验对应特殊节点已完成且有已保存 `ADD` 附件；配置缺失、非法、预检后变更、证据不完整均 fail fast。

## Workspace Baseline

- `git status --short --branch`：`int_main...origin/int_main [ahead 1]`，已有大量本任务开始前的未提交 tracked/untracked 改动。
- 本任务将避免修改无关文件；提交阶段按项目 dirty-worktree baseline 规则处理。

## BDD Scenarios

- `BDD: 金手指配置可见性 -> Given 金手指用户 / When 打开个人中心配置页签 / Then 可看到 4 个资料限制开关；普通用户不可见配置页签。`
- `BDD: 默认关闭保持现状 -> Given 四个开关默认关闭 / When 特殊节点未完成且无附件 / Then 放行预检不因这些资料阻塞。`
- `BDD: 打开后阻止无资料放行 -> Given 某资料限制打开 / When 对应特殊节点未完成或无已保存附件 / Then 放行预检生成 BLOCKER 且提交放行失败。`
- `BDD: 完成并上传后允许放行 -> Given 某资料限制打开 / When 对应特殊节点已完成且存在已保存 ADD 附件 / Then 该检查项 PASS。`
- `BDD: 配置变更后必须重跑预检 -> Given 预检后开关状态发生变化 / When 提交放行 / Then 后端拒绝提交并提示重新预检。`

## Milestone Log

- 2026-07-26：创建任务文档，记录用户计划、BDD 场景和当前脏工作区基线。
- 2026-07-26：`GREEN: experience-preflight -> PASS`，已读取 `docs/experience-index.md`、前端/后端/数据库/E2E/登录/编码/任务收尾规则；命中前端静态契约隔离、静态合同同步、全局开关 E2E 恢复、Maven reactor 兄弟模块和 PowerShell/Git 门禁。

## RED/GREEN Evidence

- `RED: node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js -> FAIL, src/api/mes/pro/edhr/releaseDossierRequirementSetting.ts 缺失`（来自本任务 RED 交接记录）。
- `RED: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" test -> FAIL, upstream reactor 模块无指定测试；按 PowerShell/Maven 门禁改用 "-Dsurefire.failIfNoSpecifiedTests=false"`（来自本任务 RED 交接记录）。
- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-static.spec.js -> PASS`，前端 API wrapper、Profile 配置页签、4 个 switch 文案、金手指权限、确认保存、失败回滚和放行检查展示映射通过静态合同。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile -> PASS`，2026-07-26 13:15:50 完成后端生产代码 reactor 编译。
- `GREEN: pnpm ts:check -> PASS`，`vue-tsc --noEmit -p tsconfig.relaxed.json` 通过。
- `GREEN: node tests\e2e\edhr-release-check-result-chinese-static.spec.js -> PASS`，放行检查结果中文映射保持通过。
- `GREEN: node tests\e2e\edhr-release-dialog-copy-cleanup-static.spec.js -> PASS`，放行弹窗文案相邻静态合同保持通过。
- `GREEN: git diff --check -> PASS`，仅输出 CRLF 工作区警告，无 whitespace error。
- `GREEN: node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8081 --target-path /user/profile --target-text 个人工作台 -> PASS`，使用本机默认身份标签 `芋道源码/admin` 完成真实前端登录前置；命令实际密码来自 `.env`，未写入日志。
- `GREEN: node --check tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js -> PASS`，真实 E2E 脚本语法检查通过。
- `BLOCKER: node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js -> BLOCKED`，登录成功后 GET `/admin-api/mes/pro/edhr-release-setting/dossier-requirements` 返回业务 404 `请求地址不存在:admin-api/mes/pro/edhr-release-setting/dossier-requirements`；`48081` 当前监听进程为 `java -jar D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --spring.profiles.active=local --server.port=48081`，不是本任务当前 `E:\IntRuoyi` 后端实现，且未加载新增 Controller。脚本在读取原始配置前失败，未切换任何全局开关，无恢复动作需要执行；证据：`doc/tasks/20260726-edhr-release-dossier-requirement-switches/e2e-artifacts/dossier-requirement-setting-real/result.md`。
- `BLOCKER: jar tf D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar | Select-String MesProEdhrReleaseSettingController -> EMPTY`，当前 48081 运行 jar 不包含 `MesProEdhrReleaseSettingController` / `EdhrReleaseDossierRequirement*` 类，确认 404 根因是运行态未加载本任务后端接口。
- `GREEN: git worktree add -b codex/edhr-release-dossier-e2e-20260726 D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726 HEAD -> PASS`，为真实 E2E 创建隔离 worktree；端口登记表 slot 5：前端 8086，后端 48086。
- `GREEN: powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1 -> PASS`，隔离 worktree 解析为 `int_main` profile / slot 5 / `http://127.0.0.1:8086` / `http://127.0.0.1:48086/actuator/health`。
- `BLOCKER: mvn.cmd -pl yudao-server -am -DskipTests package -> FAIL`，隔离 clean worktree 后端打包失败在当前 HEAD 既有 MES 编译漂移：`MesProEdhrApprovalTaskAdapter` 无法解析 `MesProEdhrWorkTaskRespVO` / `MesProEdhrWorkTaskDO` 的 Lombok getter（如 `getStatus()`、`getExecutionId()`、`getTaskType()`），以及 `MesProFeedbackApprovalTaskAdapter` 无法解析 `MesProFeedbackStatusEnum#getStatus()`。未生成隔离 jar，未启动 48086 后端/8086 前端，未触发全局开关切换。
- `BLOCKER: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，第一次失败于并行/无关 `MesProRouteBatchRecordAttachmentOwnerServiceTest` 缺少 route attachment owner VO；复跑时该阻塞变化为并行/无关 `yudao-module-system` 编译错误 `CodexTestRunnerServiceImpl` 未实现 `getRunnerStatus()`，导致 `yudao-module-mes` 被 reactor 跳过。
- `BLOCKER: mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" test -> FAIL`，辅助 MES-only 复验被并行/无关 route/BPM 改动阻塞：`MesProRouteFlowConfigServiceImpl` 未实现 `saveBatchRecordAttachmentOwners(...)`，以及 `BusinessApprovalPolicyDOBuilder` 缺少 `formPolicyType(String)`。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile -> PASS`（隔离 worktree），修复 `MesProRouteFlowConfigServiceImpl#parseCandidateSourceNames(Object)` 重复定义后 clean 编译通过。
- `GREEN: mvn.cmd -pl yudao-server -am "-DskipTests" package -> PASS`（隔离 worktree），生成 `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- `GREEN: jar tf ...\yudao-module-mes-2026.04-SNAPSHOT.jar | Select-String MesProEdhrReleaseSettingController -> PASS`，确认隔离 jar 内 MES 模块包含新增 Controller 与 DTO/Service 类。
- `GREEN: start-branch-backend.ps1 -> PASS`，slot 5 后端 `http://127.0.0.1:48086/actuator/health` 返回 `UP`，PID 52792，命令行指向隔离 worktree jar。
- `RED: start-branch-frontend.ps1 -> FAIL, Command "vite" not found`，隔离 worktree 前端缺少 `node_modules/.bin/vite`。
- `GREEN: pnpm install --frozen-lockfile -> PASS`，隔离 worktree 前端依赖补齐，lockfile 未变。
- `GREEN: start-branch-frontend.ps1 -> PASS`，slot 5 前端 `http://127.0.0.1:8086/` 返回 HTTP 200，Vite 进程命令行指向隔离 worktree并代理后端 48086。
- `RED: node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js with 8086/48086 -> FAIL, 配置缺失 mes.edhr.release.dossier.requirements`，后端按无 fallback 设计 fail fast，确认不是 404。
- `GREEN: sql/mysql/20260726_mes_edhr_release_dossier_requirements.sql seed equivalent -> PASS`，对本机 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` 校验 `infra_config` 表、重复键、JSON 和 4 个布尔字段后插入默认全 false 配置；未记录数据库密码。
- `RED: node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js with 8086/48086 -> FAIL, 页面必须展示当前配置 hash`，真实页面异步加载 hash，脚本立即断言导致误报。
- `GREEN: node --check tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js -> PASS`，脚本改为等待配置 hash 文案出现。
- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-static.spec.js -> PASS`，真实 E2E 等待修复后前端静态合同仍通过。
- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js with 8086/48086 -> PASS`，真实页面配置页签展示 4 个资料限制开关和 hash；通过 UI 确认打开“来料检报告”，API 复核变更成功；finally 通过 UI 恢复原始全 false 并复验。
- `GREEN: DB restore verification -> PASS`，只读查询 `infra_config` 确认 `mes.edhr.release.dossier.requirements` 已恢复为四个字段全 false。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`（隔离 worktree），24 tests / 0 failures / 0 errors。
- `GREEN: stop task-owned slot 5 runtime -> PASS`，停止 PID 52792 java、PID 26488 vite；复验 8086/48086 均已释放。
- `GREEN: authorized 48081 runtime replacement -> PASS`，用户明确授权后，停止旧 `48081` PID `18212`（命令行指向 `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\...\yudao-server-exec.jar`），并启动任务 Jar `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` 到 `48081`，新 PID `57744`，`/actuator/health` 返回 `UP`。
- `GREEN: 48081 route probe -> PASS`，未登录访问 `http://127.0.0.1:48081/admin-api/mes/pro/edhr-release-setting/dossier-requirements` 返回业务 `401 账号未登录`，不再返回 `请求地址不存在`；任务 Jar 嵌套 MES 模块确认包含 `MesProEdhrReleaseSettingController.class`。
- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js with 8081/48081 -> PASS`，真实页面配置页签展示 4 个资料限制开关；通过 UI 确认打开“来料检报告”，API 复核变更成功；finally 通过 UI 恢复原始全 false 并复验。证据：`doc/tasks/20260726-edhr-release-dossier-requirement-switches/e2e-artifacts/dossier-requirement-setting-real/result.md`。
- `GREEN: int_main credential-specific E2E -> PASS`，按用户本次指定的 `芋道源码/admin` 登录凭据执行默认 int_main `8081/48081` 真实 E2E；凭据明文未写入任务日志、命令证据或结果文件。
- `GREEN: int_main DB restore verification -> PASS`，只读查询 `infra_config` 确认 `mes.edhr.release.dossier.requirements` 在 int_main E2E 后仍恢复为四个字段全 false。
- `GREEN: project-experience-consolidation -> PASS`，已将 worktree 前端依赖缺失和 worktree 隔离 E2E 成对 URL 门禁归档到既有 `docs/worktree-memory.md`、`docs/e2e-rules.md`，并更新 `docs/experience-index.md` 关键词。

## Blockers

- 实现与验证已完成；当前只剩收尾 cleanup、经验沉淀、提交与推送未执行。
- 主工作区存在大量并行脏改动和 ahead 状态；提交前必须按 dirty-worktree baseline 与选择性暂存规则处理，不能把无关任务改动混入本任务提交。
