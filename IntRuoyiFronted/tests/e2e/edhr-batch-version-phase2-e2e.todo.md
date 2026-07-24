# 阶段二真实 E2E 待启用清单

> TODO(PHASE2_WAIT_PHASE1): 阶段一版本快照、迁移证据和审批门禁 API 合入后，把本清单拆成真实 Playwright 脚本。当前文件只记录路径，不声明通过。

## 通用门禁

- Base URL：`http://127.0.0.1:8096`
- Backend：`http://127.0.0.1:48096`
- 写入租户：本机 `测试租户/aoteman`
- 只读复验：本机 `芋道源码/admin`
- 禁止：
  - `page.route`
  - `route.fulfill`
  - `.skip`
  - 默认密码硬编码
  - API 绕过前端业务路径
  - 外部服务器访问

## E2E-01 结构化 diff

- Given 测试租户已存在 V1.0 当前批记录。
- When `aoteman` 从“导入 Word”导入同名主 Word 生成 V2.0 草稿。
- Then 页面展示 `TABLE`、`PROCESS`、`FIELD`、`SIGNATURE_CELL`、`ATTACHMENT_RULE`、`CELL_RULE` 六类差异。
- And 每类至少展示 `sourceLogicalKey`、`targetLogicalKey`、`matchConfidence`、`matchEvidenceJson`。
- And `admin` 登录后只读打开同一版本详情，不发生 MES 写请求。

## E2E-02 CONFIRM_REQUIRED 授权确认

- Given V2.0 迁移差异存在 `CONFIRM_REQUIRED` 项。
- When 有权限用户填写确认意见并确认。
- Then 页面刷新显示确认人、确认时间、确认意见。
- And 无权限用户确认失败，显示后端权限错误。
- And `BLOCKER` 项不出现确认按钮。
- And `admin` 只读复验确认审计，不发生写请求。

## E2E-03 草稿重新上传

- Given V2.0 版本状态为 `DRAFT` 或 `PRECHECK_FAILED`。
- When `aoteman` 执行重新上传。
- Then 旧草稿被作废，新草稿生成新的 `versionId`，重新生成迁移证据。
- And 已提交审批或已批准版本不能重新上传。
- And `admin` 只读复验版本链路，不发生写请求。

## E2E-04 迁移证据展示

- Given V2.0 存在签名位、附件规则和单元格规则迁移项。
- When `aoteman` 打开差异详情。
- Then 页面展示填写人来源、附件规则、单元格约束和匹配证据 JSON。
- And `admin` 只读复验同一证据，不出现确认或重传按钮。

