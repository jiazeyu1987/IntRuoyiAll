# 任务：带数据发布补齐 DCC 对象清单门禁

## 任务目标

修复发布包构建与部署链路，使 `publishScope=with-data` 不只是同步数据库和 `yudao` 桶，而是显式生成并校验 DCC 相关文件对象清单。缺少 DCC 引用文件、对象快照或关键元数据时必须 fail fast，不得把带数据发布误判为 ready。

## 上一任务检查

- 后端仓库最近任务 `doc/tasks/20260604-backend-runtime-control-state-commit/task.md` 状态为 `completed`。
- 当前后端工作区存在既有 `runtime/runtime-control/runtime-ops/alerts.json` 和 `capacity-status.json` 修改，不属于本任务范围，不能回退或覆盖。

## BDD 场景

- BDD: 带数据发布必须包含 DCC 对象清单 -> Given 构建发布包选择 `publishScope=with-data` 且源环境存在 DCC 文件引用 / When 生成 release package / Then 发布包必须包含 DCC 对象清单或可复核的对象索引，且 DCC 引用文件缺失时构建失败。
- BDD: 带数据发布缺少 DCC 引用对象必须阻断 -> Given DCC 数据库记录引用的 `infra_file` 或 `yudao` 对象在源环境中不存在 / When 构建带数据发布包 / Then 构建必须失败并报告缺失对象，而不是静默成功。
- BDD: code-only 不应误带数据门禁 -> Given 构建发布包选择 `publishScope=code-only` / When 执行构建 / Then 不应生成 DCC 对象清单门禁结果，也不应误判为 with-data。

## 里程碑

- [x] M1：确认前置任务状态和当前工作区脏改动边界。
- [x] M2：为发布包构建与 DCC 对象清单补充 RED 测试。
- [x] M3：实现带数据发布的 DCC 对象清单生成与缺失校验。
- [x] M4：补充/更新 GREEN 回归测试。
- [x] M5：记录验证证据并收尾。

## 预期验证

- RED：新增测试应先证明当前带数据发布未断言 DCC 对象清单门禁。
- GREEN：`publishScope=with-data` 在源环境缺少 DCC 引用对象或对象快照时必须失败。
- GREEN：`publishScope=code-only` 不受 DCC 对象清单门禁影响。
- VERIFY：发布包产物中应能复核 DCC 对象清单或索引文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺对象必须直接阻断构建，不做降级成功。
- `是否从根因和长期维护角度解决`：是。把 DCC 对象完整性前置到发布包构建契约，避免后续才发现带数据包缺少关键对象。
- `是否存在临时补丁或绕过`：否。不手写成功状态，不允许以旧包或手工清单替代真实校验。

## 完成内容

- `script/deploy/publish-int-ruoyi.ps1` 在 `build-release` 且未跳过数据同步时，导出 MinIO `yudao` 桶后查询 DCC 文件引用并生成 `manifest/dcc-object-inventory.json`。
- DCC 对象清单覆盖受控文件、盖章、外来文件评审、受控副本签收、审批模板、临时文件等 DCC 文件引用字段。
- 构建阶段校验 `infra_file.path`、对象路径安全性、快照文件存在性，并记录 SHA256、字节数和引用来源；缺失时以 `DCC_OBJECT_*` 错误 fail fast。
- `deploy-release` 的 `with-data` 包校验新增 DCC 清单存在性、JSON/schema 校验、清单列出文件存在性与 SHA256 校验，防止被篡改或不完整发布包继续部署。
- `script/tests/test_publish_int_ruoyi_to_test_tooling.py` 增加带数据发布 DCC 对象清单门禁测试。

## 验证结果

- RED：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k with_data_release_package_requires_dcc_object_inventory -q` 初始失败，原因是脚本尚无 `Write-DccObjectInventoryForReleasePackage` 与 DCC 对象清单门禁。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k with_data_release_package_requires_dcc_object_inventory -q` 通过，`1 passed, 58 deselected`。
- REGRESSION：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` 通过，`59 passed`。
- REGRESSION：PowerShell `[scriptblock]::Create(...)` 解析 `script/deploy/publish-int-ruoyi.ps1` 通过，输出 `POWERSHELL_PARSE_OK`。
- 额外扩展测试：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_publish_int_ruoyi_deploy_services.py -q` 存在既有 OnlyOffice 等待条件测试冲突，`test_onlyoffice_readiness_is_waited_only_when_compose_declares_it` 失败；该失败与本次 DCC 对象清单改动无关，未扩大本任务范围修复。

## 当前状态

completed

## 阻塞

- 无本任务阻塞。
