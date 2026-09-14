# 执行日志

## 用户意图

- 用户要求确认已连接的目标金蝶账套能否查看生产订单数据。

## 范围

- 登录目标账套后只调用 `DynamicFormService.ExecuteBillQuery.common.kdsvc`。
- 表单为 `PRD_MO`，最多读取 5 行，只请求项目现有客户端已使用的基础字段。
- 不保存配置，不调用 ERP 写入接口，不同步数据到本地数据库。

## 经验前置

- 已读取 `docs/experience-index.md` 与 `docs/login-access.md` 的金蝶账套登录门禁。
- 已核对 `ErpKingdeeProductionOrder.FORM_ID=PRD_MO` 及 `ErpKingdeeProductionOrderClientImpl` 的查询端点、URL 归一化和字段键。
- 中文用户名通过 UTF-8 请求体直接发送；不沿用未指定 `utf8mb4` 的旧探针读取方式。

## 命令意图

- 使用已确认可登录的凭据建立金蝶会话。
- 使用同一会话查询生产订单最近 5 行，并只输出查询状态与允许展示的业务字段，不输出密码或 Cookie。

## 里程碑记录

- M1：完成。生产订单表单和正式只读查询契约已确认。
- M2：完成。目标账套 `ValidateUser` 登录成功，`PRD_MO ExecuteBillQuery` 返回 HTTP 200 和 5 行数组数据。
- M3：完成。查询结果、范围边界和安全约束已写入 `verification-report.md`。

## 验证证据

- 登录：`LoginResultType=1`、`IsSuccessByAPI=true`，并返回会话 Cookie。
- 查询：`FormId=PRD_MO`、`Limit=5`、`OrderString=FID DESC`；HTTP 200，返回 5 行数组，无权限或字段错误对象。
- 字段：`FID`、`FBillNo`、`FDocumentStatus`、`FDate`、`FMaterialId.FNumber`、`FMaterialId.FName`、`FQty` 均成功返回。
- 安全：未输出或写入密码、Cookie、签名或完整配置；未调用保存、提交、审核等写入接口。
- 经验沉淀：在既有 `docs/login-access.md` 金蝶门禁中补充“登录成功不等于业务对象读取权限，必须用目标表单最小只读查询单独验证”的规则，并更新 `docs/experience-index.md` 路由；未新建长期经验文档。
- 收尾清理：`task-closeout-cleanup` preview 与 apply 均通过；保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、阻塞项或警告。
