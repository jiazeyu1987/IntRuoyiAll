# 任务：运行控制回滚兼容性证据闭环

## 任务目标

为 1+N 主控闭环补齐 `rollback-compatibility.json` 的正式生成与严格校验路径。发布测试通过的 `mark-tested` 流程负责生成可审计兼容性证据；后端回滚候选服务与回滚脚本只消费并校验该证据，不在回滚时临时生成，不修改 live NAS 数据，不执行真实远端运维动作。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-dcc-controlled-file-metadata-edit/task.md`
- 状态：`completed`
- 处理：上一任务已完成并提交；本任务只修改运行控制/发布/备份运维回滚兼容性证据相关代码与测试。

## BDD 场景

- BDD: mark-tested 生成兼容证据 -> Given 测试服当前发布包已通过真实恢复集验证且发布包 manifest 为 code-only / When 操作员执行 `mark-tested` / Then 脚本写入 `tested.json` 与 `rollback-compatibility.json status=COMPATIBLE`，包含 `packageDirectoryName`、`checkedAt`、`summary` 和恢复集证据。
- BDD: 非 app-only 证据保持阻塞 -> Given 发布包 manifest 为 `with-data` 或恢复集程序版本与发布包目录不一致 / When 操作员执行 `mark-tested` / Then 脚本写入 `BLOCKED` 兼容性证据，候选继续不可用。
- BDD: 候选校验严格消费证据 -> Given `rollback-compatibility.json` 缺少 `packageDirectoryName`、`checkedAt` 或 `summary` / When 后端候选服务或回滚脚本扫描候选 / Then 候选保持 `BLOCKED` 或被跳过，不允许真实回滚动作继续。

## Milestones

- [x] M1：确认根 1+N 主控任务与回滚兼容性缺口。
- [x] M2：补充 RED 测试覆盖生成证据和严格字段校验。
- [x] M3：实现 `mark-tested` 生成 `rollback-compatibility.json`，并收紧后端/脚本消费校验。
- [x] M4：运行目标测试、PowerShell 解析检查、空白检查和收尾预览。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_backup_ops_tooling.py::test_rollback_tag_scan_requires_complete_compatibility_evidence_contract script/tests/test_mark_tested_current_release_tooling.py -q`
- `mvn -pl yudao-module-infra -Dtest=RuntimeRollbackCandidateServiceImplTest test`
- PowerShell parser check for `script/deploy/publish-int-ruoyi.ps1` and `script/backup-ops/scripts/modules/Infra/DockerOps.psm1`
- `git diff --check`
- task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少发布包 manifest、恢复集字段、hash、程序版本或 redis 策略时 fail fast；非兼容条件写 `BLOCKED` 证据而不是伪造成功。
- `是否从根因和长期维护角度解决`：是。兼容性证据在发布测试闭环生成，回滚候选与回滚执行只做严格消费校验。
- `是否存在临时补丁或绕过`：否。不手写 live NAS 证据，不执行真实远端运维动作，不通过接口或脚本绕过前端做真实 ops。

## 当前状态

completed

## 当前证据

- RED：Python 3 个新增合同测试 -> FAIL，预期原因：发布脚本缺少正式生成函数，回滚脚本未严格校验 `packageDirectoryName`、`checkedAt`、`summary`。
- RED：`mvn -pl yudao-module-infra -Dtest=RuntimeRollbackCandidateServiceImplTest#listRollbackCandidatesShouldBlockCompatibilityEvidenceWithoutPackageDirectoryName test` -> FAIL，预期原因：后端候选服务仍允许缺 `packageDirectoryName` 的兼容性证据。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_backup_ops_tooling.py::test_rollback_tag_scan_requires_complete_compatibility_evidence_contract script/tests/test_mark_tested_current_release_tooling.py -q` -> PASS，59 passed。
- GREEN：`mvn -pl yudao-module-infra -Dtest=RuntimeRollbackCandidateServiceImplTest test` -> PASS，15 tests。
- GREEN：PowerShell parser check for `publish-int-ruoyi.ps1` and `DockerOps.psm1` -> PASS。
- GREEN：`git diff --check` -> PASS。

## 阻塞

- 代码闭环无阻塞。根 1+N 真实运维闭环仍需授权/窗口、可绑定恢复集和正式 `mark-tested` 生成的 live NAS `rollback-compatibility.json status=COMPATIBLE`。
