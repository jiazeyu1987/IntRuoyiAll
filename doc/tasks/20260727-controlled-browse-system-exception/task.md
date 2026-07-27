# 测试服受控浏览系统异常分析

## Task Goal

分析并修复测试服务器上使用 `wangsiyu` 账号进入文控中心受控浏览时，文件详情页提示“系统异常”的真实原因；修复范围限定为 `preview-metadata` 不再强制要求 `fileNumber` 非空，不写入业务数据。

## Milestones

- [x] 创建任务记录并读取服务器、登录、E2E 与缺陷复现规则。
- [x] 用真实测试服页面路径复现受控浏览文件详情“系统异常”。
- [x] 捕获相关前端网络响应、控制台信息或后端只读日志证据。
- [x] 给出根因、影响范围、验证证据和后续修复建议。
- [ ] 增加空文件编号受控预览元数据回归测试，先复现 `fileNumber is required`。
- [ ] 调整预览访问与水印审计校验，使 `fileNumber` 按可选字段处理。
- [ ] 运行目标 Maven 测试并记录 RED/GREEN 证据。

## Expected Verification

- 测试服入口、账号标签和页面路径明确。
- 记录触发异常的接口、HTTP 状态、后端错误信息或日志关键字。
- 不记录密码、令牌、私钥、连接串等敏感信息。

## Current Status

in_progress

## Final Finding

测试服使用 `wangsiyu` 账号应登录 `芋道源码` 租户。截图中的文件 `血液瓶瓶体清洗验证.pdf` 对应 `dcc_controlled_file.id=2054545668044071537`，其 `file_number` 为空。详情 viewer 打开后，前端请求 `/admin-api/dcc/controlled-files/2054545668044071537/preview-metadata`，后端 `DccControlledPreviewAccessService` 在生成受控预览水印/访问审计时强制校验 `fileNumber` 非空，因此抛出 `IllegalArgumentException: fileNumber is required`，被全局异常处理成 `code=500, msg=系统异常`。

## Verification Evidence

- Playwright 真实路径：测试服 `http://172.30.30.58:8081`，`芋道源码/wangsiyu`，打开 `/dcc/controlled-file/detail/2054545668044071537?viewer=1&from=browser`。
- 前端失败接口：`GET /admin-api/dcc/controlled-files/2054545668044071537/preview-metadata` 返回 `code=500, msg=系统异常`。
- 后端日志：`DccControlledPreviewAccessService.requireNotBlank` 抛出 `fileNumber is required`。
- 数据库只读核对：目标文件 `file_number` 为空；租户 1 下 ACTIVE/SUPERSEDED 且文件编号为空的受控文件约 `15995` 条。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修复将预览元数据链路与文件编号可选的数据契约对齐。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 服务器访问门禁：仅访问用户明确指定的测试服务器 `172.30.30.58`，不发布、不重启、不修改远端服务。
- 登录与 E2E 门禁：使用 Playwright 操作真实前端页面；登录凭据仅用于会话，不写入日志。
- 受控内容门禁：若异常与受控内容生命周期、文件预览或对象权限有关，必须记录真实接口/日志原因，不用 API-only 或 mock 替代页面复现。
