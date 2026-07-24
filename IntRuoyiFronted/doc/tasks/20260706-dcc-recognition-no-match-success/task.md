# DCC 基础信息识别无匹配前端提示

## 任务目标

同步后端无匹配识别契约：文控中心“识别基础信息”接口正常返回但未匹配到产品名称时，前端提示“识别完成，未匹配到产品名称，请人工确认”，不再展示失败提示。

## 里程碑

1. [x] 建立任务文档、经验门禁和 BDD/TDD 基线。
2. [x] 前端 RED：静态契约测试要求响应类型和提示区分 `NO_MATCH`。
3. [x] 前端 GREEN：扩展响应类型并调整详情页提示。
4. [x] 运行目标前端验证并记录证据。

## 预期验证

- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 e2e:dcc:project-code-recognition:static`
- 前端仍对接口异常展示失败提示。
- 前端对 `NO_MATCH` 展示业务空结果提示。

## BDD 场景

BDD: 无匹配结果提示人工确认 -> Given 后端识别接口返回 `recognitionStatus=NO_MATCH`, When 用户点击识别基础信息, Then 页面提示“识别完成，未匹配到产品名称，请人工确认”并刷新详情。

BDD: 匹配成功仍展示产品信息 -> Given 后端识别接口返回 `recognitionStatus=SUCCESS` 和产品名称/编码, When 用户点击识别基础信息, Then 页面提示已识别基础信息和产品名称/编码。

BDD: 接口异常仍展示失败 -> Given 后端识别接口抛出系统错误, When 用户点击识别基础信息, Then 页面展示基础信息识别失败提示。

## 经验门禁

- PowerShell：已读取 `docs/powershell-memory.md`，命令显式设置 UTF-8，不使用 `&&`。
- 前端行为：只调整当前 DCC 详情页状态提示和 API 类型，不做无关视觉重构。
- BDD/TDD：先扩展静态测试，再实现最小前端行为。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。前端根据显式 `recognitionStatus` 呈现业务结果，不隐藏接口异常。
- 是否从根因和长期维护角度解决：是。前端跟随后端契约区分业务空结果与异常。
- 是否存在临时补丁或绕过：否。

## 当前状态

已完成。

## 完成结果

- `ControlledFileProjectCodeRecognitionRespVO` 新增显式 `recognitionStatus: 'SUCCESS' | 'NO_MATCH'`。
- 详情页识别基础信息时，`NO_MATCH` 提示“识别完成，未匹配到产品名称，请人工确认”。
- 接口异常仍走原失败提示，不吞异常、不降级。
- 新增 `e2e:dcc:project-code-recognition:static` 脚本，便于复用本契约检查。

## 最终验证

- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 run e2e:dcc:project-code-recognition:static` -> GREEN PASS。
- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/detail/index.vue tests/e2e/dcc-project-code-recognition-static.spec.js --format stylish` -> GREEN PASS。

## Cleanup Keep

- doc/tasks/20260706-dcc-recognition-no-match-success/frontend-feature-evidence.md
