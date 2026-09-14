# 球囊扩张压力泵 QA 草稿清洗/精洗拆分

## Task Goal

将产品“球囊扩张压力泵”的 QA 规程草稿中工序“清洗/精洗”拆为两条独立记录：“清洗”与“精洗”；两条记录后续检验项目、适用检验类型、接受标准、检验方法、检验器具及设备、抽样方案等内容保持一致。

## Milestones

- [x] 定位草稿数据、表结构和目标产品/规程记录。
- [x] 记录 BDD 场景并用只读查询形成 RED 证据。
- [x] 执行最小数据修正，避免引入 fallback、默认值或静默降级。
- [x] 复核目标草稿仅形成“清洗”、“精洗”两条一致记录，并记录验证结果。

## Expected Verification

- 只读查询证明修正前存在“清洗/精洗”合并工序。
- 修正后只读查询证明同一产品草稿存在“清洗”与“精洗”两条记录。
- 两条记录除工序名称外的关键内容一致。
- 若运行态或凭据不可用，记录精确 blocker 与影响。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；按用户指定草稿数据源做正式记录拆分，不用前端文案或默认展示掩盖。
- 是否存在临时补丁或绕过：否。

## Experience Gate

- 命中 docs/backend-development.md 的 “QA 多工序正式发布与退役夹具唯一键必须隔离” 与 “QA 规程配置状态必须来自产品级规程记录” 门禁。
- 命中 docs/frontend-development.md 的 “前端静态契约隔离门禁”；本次使用产品专用静态合同完成 RED/GREEN。
- project-experience-consolidation 搜索后确认：本次是既有 QA 规程草稿模板的单点业务修正，没有新的可复用长期经验需要合并或新建。

## Cleanup Keep

- doc/tasks/20260810-split-pressure-pump-cleaning-qaspec-draft/task.md
- doc/tasks/20260810-split-pressure-pump-cleaning-qaspec-draft/execution-log.md
- doc/tasks/20260810-split-pressure-pump-cleaning-qaspec-draft/verification-report.md
