# Task: 批记录通用版式规则继续增强

## Goal

继续按通用规则优化批记录生成结果，重点统一：
- 按行形态/单元格形态识别表头与灰底，让“标签格灰、值格白”更稳定
- 多段工艺页保留重复子表头和分段结构，不再压平成单块
- 拉开外框、分区线、普通网格线的层级
- 统一页宽预算和说明文字留白，让不同工序页落在同一版式节奏里

## Scope

- 仅修改通用报表生成规则，不做按模板名的特例分支
- 由 4 个并行子任务分别推进不同规则点
- 变更后需要用真实 Route B 重生并复核 `精洗 / 清洗 / 清洁` 三张工序页的视觉效果

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-other-three-process-post-change-compare/task.md`
- Status before this follow-up: completed
- Impact: 上一轮已确认三张工序页“有优化但仍有通用差异”；本轮继续把这些差异收敛到通用规则里

## Milestones

- [x] M1: 4 个并行子任务完成各自责任代码与测试
- [x] M2: 真实打包并重启当前 backend
- [x] M3: 真实重生 Route B 并抓取三张工序页最新截图
- [x] M4: 逐图复核修改后是否继续优化
- [x] M5: 更新任务证据并收口

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `GET http://127.0.0.1:48081/v3/api-docs`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- 真实截图产物写入本任务目录 `artifacts/`

## Current Status

Completed. The generic rule slices are green in targeted tests, the backend package/restart path succeeded, and the latest live screenshots for `精洗 / 清洗 / 清洁` confirm the structure is closer to the source images than before.
