# 任务：smart-release 发布方案设计与严格评审

## 任务目标

根据本次讨论重新设计 IntRuoyi 发布方案，使构建更快，并确保表结构、必要数据、资源引用变化不会导致测试服或备份服发布频繁失败；同时明确大文件、多文件场景中发布、备份、恢复的职责边界和增量策略。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；方案必须 fail fast，缺少 manifest、SQL、数据包、资源索引、目标配置或校验失败时不得静默降级为全量构建或继续发布。
- `是否从根因和长期维护角度解决`：是；方案必须以发布契约、变更清单、执行历史、资源索引和目标环境预检为核心，而不是依赖人工记忆。
- `是否存在临时补丁或绕过`：否；本任务只产出正式方案和评审结论，不做临时发布绕过。

## BDD 场景

- BDD: 小改动快速构建 -> Given 后端、前端、Website、SQL、数据、资源中只有一个组件变化 / When 构建 `smart-release` / Then 只重建变化组件，未变化组件复用已验证产物并校验 checksum/digest。
- BDD: 表结构变化不导致部署失败 -> Given 本地代码依赖新的表或字段 / When 构建发布包 / Then 发布包必须包含版本化、幂等的数据库迁移及执行顺序，目标环境部署前能预检并在缺失时 fail fast。
- BDD: 必要数据变化可随发布到测试服和备份服 -> Given 菜单权限、字典、配置或业务基础数据变化 / When 构建发布包 / Then 必要数据被声明为数据迁移包并带 checksum、执行历史和租户范围，禁止覆盖目标环境非发布数据。
- BDD: 发布不搬运海量文件但检查引用一致性 -> Given 发布包不包含 DCC/展厅真实文件 / When 部署到测试服或备份服 / Then 发布前后必须校验数据库文件引用、文件配置和目标 MinIO 对象一致，不因引用正式服域名或缺对象而放行。
- BDD: 备份恢复支持大文件增量 -> Given DCC 模块有 10000 个文件且只新增少量文件 / When 执行备份或恢复 / Then 只同步新增或变化文件，数据库关系和资源 manifest 保持一致，并通过 size/sha256 校验。

## 里程碑

- [x] M1：建立任务文档和验收标准。
- [x] M2：子 agent 调研当前发布、备份、恢复链路并产出候选方案。
- [x] M3：主 agent 按更快构建、更稳发布、大文件增量、无 fallback 原则严格评审。
- [x] M4：落盘最终方案、评审结论和后续实施拆分。

## 预期验证

- 子 agent 输出必须覆盖构建增量、数据库迁移、数据包、资源引用一致性、备份恢复大文件增量、目标环境预检、回滚和执行历史。
- 主 agent 评审必须明确 pass/fail、阻塞问题、必须修改项和最终放行结论。
- 最终方案必须写入任务目录，且不修改生产发布脚本。

## 完成证据

- 子 agent Mendel 完成当前发布/备份/恢复事实调研，确认现有链路已有 `code-only/with-data`、release manifest、required SQL、MinIO snapshot、DCC object inventory 和目标环境门禁。
- 子 agent Bohr 产出 `proposal-worker-draft.md`，第一轮 review 未放行，已按阻塞项修订。
- 主 agent 最终方案：`final-design.md`。
- 主 agent 放行单：`review-report.md`，最终结论 `final_decision=pass`。

## 当前状态

completed

## Current Status

completed
