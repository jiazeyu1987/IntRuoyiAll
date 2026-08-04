# 审批中心申请人独立列

## Task Goal

在审批中心待办、已办、我发起的和抄送列表中，将正式申请人信息作为“申请人”独立列展示；DCC 业务摘要不再重复展示申请人。

## Milestones

- [x] M1：保存任务开始前既有脏工作区基线，确认申请人字段和用户列配置契约。
- [x] M2：新增申请人列聚焦静态合同并记录 RED。
- [x] M3：实现申请人独立列、默认列配置与表格键升级并记录 GREEN。
- [x] M4：完成相邻回归、类型检查、证据校验、提交、推送与收尾。

## Expected Verification

- 聚焦静态合同证明四个审批视图均使用独立“申请人”列。
- “申请人”列读取正式 `initiatorUserId`，缺失时保持现有空值显示语义。
- DCC 业务摘要不再重复展示“申请人”字段。
- 四个审批视图升级稳定 table key，已有用户加载新的默认列集合。
- 运行四个审批中心标准列表静态合同、中文文案合同和 `pnpm ts:check`。
- 运行 frontend-feature-delivery evidence validator 和 `git diff --check`。

## Applicable Experience Gates

- `docs/frontend-development.md#前端列表跨账号默认列布局统一门禁`：新增默认列且保留“显示字段”时必须升级稳定 table key，避免历史用户列配置隐藏新列。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小静态合同完成 RED/GREEN，不以无关历史失败替代当前行为证据。
- 保持审批中心现有 API、权限、错误、加载、空态、分页和审批动作契约不变。
- 不引入 mock、fallback、降级、吞异常或申请人来源猜测。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；复用正式 `initiatorUserId` 合同并纳入统一用户列配置。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

实现、定向验证、evidence validator、cleanup apply、本地提交和 `git -c http.https://github.com.proxy=http://127.0.0.1:8902 push origin int_main` 均已完成；`int_main` 已同步到 `origin/int_main`，任务完成。
