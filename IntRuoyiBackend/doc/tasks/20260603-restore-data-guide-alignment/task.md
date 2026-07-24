# 任务：恢复数据向导取消演练和快照要求

## 任务目标

按用户要求，运行控制台“恢复数据”规则统一调整为不需要恢复演练报告、不需要恢复前现场快照；恢复数据向导、推荐阻断原因与后端候选门禁保持一致，仍保留备份点 manifest、checksum、镜像标签等基础恢复证据校验。

## 上一任务检查

- 上一个后端任务 `20260603-dcc-download-filename-runtime-repro` 已标记 `completed`。
- 本任务只处理运行控制台恢复数据规则对齐，不接管或回滚 DCC 下载、DCC token、责任人矩阵等无关改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。这里是用户明确提出的正式规则变更，不把缺失演练证据伪装为成功，也不吞掉 manifest、checksum、imageTag 等基础证据错误。
- `是否从根因和长期维护角度解决`：是。同步更新向导契约、后端测试和变更记录，避免 UI/接口提示与实际候选门禁不一致。
- `是否存在临时补丁或绕过`：否。不写入假的 `rehearsal-report.json` 或 `现场快照.md`。

## BDD 场景

BDD: 恢复数据向导不再要求演练报告和现场快照 -> Given 操作员选择数据异常场景 / When 后端返回恢复数据推荐 / Then `requiredEvidence` 不包含 `rehearsal-report` 和 `现场快照`。

BDD: 缺少演练证据不产生恢复推荐阻断 -> Given 存在 manifest、checksum、镜像标签完整但缺少演练报告和现场快照的备份点 / When 查询数据异常推荐 / Then 恢复候选保持 `AVAILABLE`，推荐阻断原因不包含演练或现场快照。

BDD: 基础恢复证据仍需 fail fast -> Given 备份点缺少 manifest、checksum 或镜像标签 / When 查询恢复候选或推荐 / Then 后端仍返回 `BLOCKED` 和明确阻断原因。

## 里程碑

- [x] M1：建立任务文档，确认上一任务状态。
- [x] M2：补充 RED 测试，锁定向导不再声明演练报告/现场快照为必需。
- [x] M3：最小修改运行控制台恢复数据向导契约。
- [x] M4：运行目标后端测试与证据校验。
- [x] M5：执行收尾清理预览并提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsGuideServiceImplTest,RuntimeRestoreCandidateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260603-restore-data-guide-alignment.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260603-restore-data-guide-alignment\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-data-guide-alignment --mode preview`

## 当前状态

completed

## 最终验证结果

- RED：`mvn -pl yudao-module-infra "-Dtest=RuntimeOpsGuideServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，4 tests 中 2 failures，证明向导仍把 `rehearsal-report` / `现场快照` 作为恢复数据必需证据。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeOpsGuideServiceImplTest,RuntimeRestoreCandidateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，12 tests，0 failures，0 errors。
- 变更证据校验：`validate_change_request.py` -> PASS。
- 后端证据校验：`validate_backend_api.py` -> PASS。
- 收尾清理预览：`task_closeout.py --task-id 20260603-restore-data-guide-alignment --mode preview` -> PASS，无删除项、无阻塞、无警告。

## Cleanup Keep

- doc/tasks/20260603-restore-data-guide-alignment/backend-api-evidence.md
