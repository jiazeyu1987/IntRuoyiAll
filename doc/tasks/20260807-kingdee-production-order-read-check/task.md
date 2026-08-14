# 金蝶目标账套生产订单只读检查

## 任务目标

使用已验证成功的目标金蝶账套用户名密码会话，调用 `ExecuteBillQuery` 只读查询 `PRD_MO` 生产订单的少量记录，判断当前账号是否具备生产订单数据读取权限。

## 里程碑

- [x] M1：核对当前项目生产订单表单、查询端点和字段映射。
- [x] M2：登录目标账套并执行最多 5 行的生产订单只读查询。
- [x] M3：记录脱敏结果、范围边界和收尾证据。

## 预期验证

- `ValidateUser` 返回登录成功状态并建立会话。
- `PRD_MO ExecuteBillQuery` 返回数组且不是权限或字段错误对象。
- 查询仅请求 `FID`、`FBillNo`、`FDocumentStatus`、`FDate`、物料编码、物料名称和数量，`Limit=5`。
- 不保存目标配置，不调用 `Save`、`Submit`、`Audit` 或其它写入接口。

## 适用经验门禁

- 已读取 `docs/experience-index.md`，命中 `docs/login-access.md#ERP 金蝶账套登录连通性门禁`。
- 登录和查询链路必须保持 UTF-8；中文用户名不得在 MySQL CLI、PowerShell 或 URL 编码环节变成问号或乱码。
- 成功登录要求 HTTP、金蝶业务状态和会话 Cookie 同时满足；成功查询要求返回合法数组，不能把 Cookie 或 HTTP 200 单独当作读取权限通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接使用当前项目正式 `PRD_MO ExecuteBillQuery` 契约验证真实读取权限。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

目标账套登录和 `PRD_MO` 最小只读查询均已通过：接口返回 HTTP 200 和 5 行生产订单数据，无权限或字段错误。cleanup preview/apply 均无删除项、阻塞项或警告；未保存配置、未同步本地数据、未调用 ERP 写入接口。
