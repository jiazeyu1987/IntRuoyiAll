# DCC 受控文件产品编号自动带出

## Task Goal

在受控文件提交页面中，DHF/DMR 类文件的“产品编号”字段必须从正式产品主数据或已有关联中自动带出；不得在提交页临时生成产品编号，也不得用空值、默认值或表单槽位数据替代。

## Milestones

1. [completed] 梳理当前 DCC 提交流程中产品编号字段、分类规则和接口契约。
2. [completed] 记录 BDD 场景并补充最小 RED 静态/单元测试。
3. [completed] 实现产品编号自动带出或唯一候选自动选中逻辑，保留无法唯一确认时的用户选择。
4. [completed] 运行目标验证与相邻回归，记录证据。
5. [completed] 完成收尾、经验沉淀、提交与推送。

## Expected Verification

- 目标静态合同或单元测试先 RED 后 GREEN。
- 前端类型检查或目标范围静态验证通过；若全量验证存在无关历史阻塞，记录阻塞范围。
- 后端契约若被修改，运行 DCC 目标后端测试或编译验证。

## Current Status

blocked_real_e2e_product_sample

- 实现、静态合同、类型检查和收尾提交已完成。
- 用户追加要求的真实页面 E2E 已执行到上传页；DHF/DMR 类别上传权限前置已通过本机权限数据补齐解决。
- 当前剩余阻塞是产品样本前置：`芋道源码/admin` 可见 1 个已绑定目录且可上传的 DHF/DMR 类别，但当前启用 DCC 项目无法唯一匹配正式产品主数据，因此暂不能完成页面“DCC 产品编号”自动带出断言。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是使用正式产品主数据/既有关联自动带出，不临时造号。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：前端静态契约隔离门禁；本任务新增 `dcc-upload-product-autofill-static.spec.js` 作为专用静态合同，避免被无关大契约阻塞。
- 适用门禁：DCC/DHF/DMR 产品编号必须来自正式产品主数据，不生成临时编号；实现只自动选择唯一匹配产品主数据，否则提示用户手动选择。

## Cleanup Keep

- doc/tasks/20260728-dcc-product-code-autofill/frontend-feature-evidence.md
- doc/tasks/20260728-dcc-product-code-autofill/database-schema-evidence.md
