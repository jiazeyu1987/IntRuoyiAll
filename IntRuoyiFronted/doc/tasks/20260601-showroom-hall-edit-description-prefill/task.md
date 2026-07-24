# 任务：修复编辑展柜描述未回填

## 任务目标

修复后台 `展柜管理 -> 编辑展柜` 弹框中 `描述` 和 `英文描述` 不显示已有内容的问题，确保列表行已有的展柜描述在点击编辑时完整回填到表单。

## 前置检查

- 上一前端任务 `doc/tasks/20260601-unocss-entry-module-not-found/task.md` 状态为 `completed`。
- 当前前端仓库存在既有未提交改动，本任务只读取并保护，不纳入本次修改范围。
- 本机 MySQL `showroom_hall` 中 `tenant_id=1` 和 `tenant_id=122` 的 8 个展柜均已有 `description` 与 `description_en` 内容。

## BDD 场景

- BDD: 编辑展柜回填描述 -> Given 展柜列表接口返回某展柜的 `description` 和 `descriptionEn` / When 用户点击该展柜的编辑按钮 / Then 编辑弹框的 `描述` 与 `英文描述` 输入框必须显示接口返回的原始内容。
- BDD: 缺失描述字段不伪造内容 -> Given 展柜列表接口未返回描述文本 / When 用户点击编辑 / Then 编辑弹框保持空值并允许用户手工填写，不生成默认描述。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复现并定位编辑弹框描述未回填原因。
- [x] M3：按授权执行带前置断言的数据修复。
- [x] M4：运行接口验证。
- [x] M5：使用真实前端路径验证编辑弹框回填，并完成收尾记录。

## 预期验证

- RED：新增或更新的展柜编辑回填测试在修复前失败，证明描述未进入表单。
- GREEN：目标前端测试通过。
- REGRESSION：真实页面进入 `展柜管理` 点击编辑，`描述` 和 `英文描述` 均显示数据库中的已有内容。

## 当前状态

status: completed

## 根因与授权

- 当前 `http://127.0.0.1:48081` 后端进程使用 `jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro`。
- Windows 上 `127.0.0.1:23306` 被既有 SSH 隧道占用：`ssh -N -L 23306:192.168.48.3:3306 -L 26379:192.168.48.2:6379 root@172.30.30.58`。
- 因此当前页面实际读取的是该隧道后的运行库，不是 `int-ruoyi-mysql` 本机 Docker 库。
- 对当前后端接口只读验证：`tenant_id=1 / admin` 的 `/showroom/hall/page` 返回 `description=""`、`descriptionEn=""`；`tenant_id=122 / aoteman` 返回完整描述。
- 用户已明确选择方案 1：授权把当前运行库里 `芋道源码/admin` 的 8 个展柜描述补回去。

## 影响

- 前端编辑弹框按接口数据回填，修复前 admin 租户接口返回空描述，所以弹框为空。
- 当前任务只同步 `showroom_hall.description` 和 `showroom_hall.description_en`，不修改产品映射、展柜名称、发布资产或审批数据。

## 完成记录

- 已用同一当前运行库中 `tenant_id=122` 的 `hall_code` 对应描述，事务性补齐 `tenant_id=1` 的 8 个展柜。
- 事务前置断言：目标 8 条均为空，来源 8 条均有中英文描述。
- 事务影响行数：`UPDATED=8`，提交后 `POST_READY=8`。
- 接口验证：`tenant-id=1 / admin` 的 `/showroom/hall/page` 返回 8 个展柜中英文描述均非空。
- Playwright 真实页面验证：登录 `芋道源码/admin` 打开 `/showroom/hall`，列表显示描述；点击 `hall_01` 编辑后，`描述` 与 `英文描述` 文本框均回填。

## Cleanup Keep

- `doc/tasks/20260601-showroom-hall-edit-description-prefill/bug-regression-evidence.md`
