# DCC项目代码关联文件数回归修复

## 任务目标

修复 DCC 项目代码列表“关联文件数”全部显示 0 的回归。实际存在文件已关联到部分项目代码时，列表必须按真实关联关系显示非 0 数量，并继续支持按关联文件数排序。

## 经验门禁

- PowerShell/Windows 命令：已读取 `docs/powershell-memory.md`，本轮命令设置 UTF-8 输入输出，不使用 `&&`，中文文本读写使用 `python -X utf8` 或 `apply_patch`。
- 缺陷回归修复：已读取 `bug-regression-fix-loop` 与证据契约，必须先用失败回归测试证明问题，再最小修复并记录 RED/GREEN。
- 任务收尾：已读取 `task-closeout-cleanup`，完成后先运行 preview，保留 `task.md` 与 `execution-log.md`。
- 真实 E2E/登录：本轮若升级到真实 E2E，必须先读取 `docs/login-access.md` 并跑登录前置；当前先做本地后端回归测试。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修正关联文件数统计的真实数据源口径。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 项目代码列表统计主表已识别关联文件 -> Given 受控文件主表记录已关联 DCC 项目代码但明细历史记录未回填, When 用户打开 DCC 项目代码列表, Then 关联文件数应统计该主表关联并显示非 0。
- BDD: 项目代码列表按真实关联文件数排序 -> Given 不同项目代码拥有不同主表关联文件数, When 用户按关联文件数排序, Then 列表按真实统计值升序或降序排列。

## 里程碑

1. 建立任务记录与经验门禁。- 已完成
2. 定位主表/明细表关联字段与现有统计口径。- 已完成
3. 添加失败回归测试覆盖识别账本已关联但列表显示 0。- 已完成
4. 最小修复关联文件数统计口径。- 已完成
5. 运行定向验证、更新日志、收尾预览并提交。- 已完成

## 预期验证

- 后端定向测试：`DccProjectCodeServiceImplTest` 新增/更新用例先 RED 后 GREEN。
- 回归验证：关联文件数排序已有用例继续通过。
- 证据记录：`execution-log.md` 记录 BDD、RED、GREEN 与根因。

## 当前状态

- 状态：completed
- 当前里程碑：已完成。

## 根因记录

- 当前列表统计原先只读取 `dcc_controlled_file.dcc_project_code_id`。
- 真实链路中部分文件已有成功识别账本 `dcc_controlled_file_recognition_record.matched_project_code_id`，但文件字段可能尚未回填，因此列表显示 0。
- 修复方式：统计时使用文件字段优先，文件字段为空时取每个文件最新成功识别账本的 `matched_project_code_id`，并按有效项目代码聚合，避免同一文件重复计数。

## 验证结果

- RED：`mvn -pl yudao-module-dcc "-Dtest=cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapperTest#selectAssociatedFileCounts_includesSuccessfulRecognitionLedgerWhenFileFieldMissing" test` 首次失败，实际只返回 `[1002]`，漏掉账本关联 `[1001]`。
- GREEN：同一命令修复后通过。
- GREEN：`mvn -pl yudao-module-dcc "-Dtest=cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapperTest#selectAssociatedFileCounts_mapsGroupedCountRows,cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeServiceImplTest#pageShouldIncludeAssociatedFileCountAndSortByCount" test` 通过，2 tests。

## 完成记录

- 状态：completed。
- 最终验证：新增账本口径 RED 用例修复后通过，既有直接字段统计和项目代码排序回归通过。
- 收尾：已在后端仓库内执行 task-closeout preview。
