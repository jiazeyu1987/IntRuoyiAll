# 任务：展柜画布候选产品补充封面图字段

## 任务目标

为展柜画布候选产品接口补充可直接访问的封面图 URL，供管理端 `Website 预览` 模式显示封面卡片。数据来源使用产品当前展示版本的 `cover_image` 字段，不修改布局保存协议和发布包结构。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260607-dcc-preview-detail-panel/task.md`
- 状态：`blocked`
- 处理：当前线程已显式切换，上一任务阻塞已记录，不与本任务混提。

## BDD 场景

- BDD: 候选产品返回封面图 -> Given 展柜画布请求候选产品 / When 后端返回 `HallProductOptionRespVO` / Then 响应包含可直接访问的 `previewImageUrl`。
- BDD: 无封面图返回空字符串 -> Given 产品当前展示版本没有 `cover_image` / When 后端返回候选产品 / Then `previewImageUrl` 返回空字符串，由前端显式占位。
- BDD: 现有布局接口不变 -> Given 用户保存展柜画布布局 / When 请求 `updateHallCanvasLayout` / Then 请求响应契约不增加任何封面相关字段。

## 里程碑

- [x] M1：建立任务文档并确认前一任务状态。
- [ ] M2：补后端 RED 测试，覆盖候选产品封面字段。
- [ ] M3：扩展候选产品模型和接口返回。
- [ ] M4：运行后端目标测试。
- [ ] M5：记录证据、收尾预览并提交本任务后端改动。
 - [x] M2：补后端 RED 测试，覆盖候选产品封面字段。
 - [x] M3：扩展候选产品模型和接口返回。
 - [x] M4：运行后端目标测试。
 - [x] M5：记录证据、收尾预览并提交本任务后端改动。

## Expected Verification

- RED/GREEN：`mvn -pl yudao-module-showroom -am -Dtest=ShowroomApiRuntimeTest -Dsurefire.failIfNoSpecifiedTests=false test`
- GREEN：接口静态检查或定向单测证明 `previewImageUrl` 存在且缺图时为空字符串。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是，仅限缺封面时返回空字符串，交由前端显式占位；不伪造默认图片。
- `是否从根因和长期维护角度解决`：是。将封面图字段纳入正式候选产品模型，而不是前端额外拼装或重复查产品详情。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260607-showroom-hall-canvas-website-preview/task.md`
- `doc/tasks/20260607-showroom-hall-canvas-website-preview/execution-log.md`
- `doc/tasks/20260607-showroom-hall-canvas-website-preview/backend-api-evidence.md`
