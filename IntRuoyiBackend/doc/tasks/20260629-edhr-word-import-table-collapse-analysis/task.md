# 任务：分析 eDHR Word 导入表格塌缩原因

- Task ID: `20260629-edhr-word-import-table-collapse-analysis`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

分析电子批记录 Route B Word 导入链路中，为什么原始横向物料表会在导入后变成纵向堆叠布局。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-batch-recognition-file-claim\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，本次只做导入链路分析，不改动生产代码。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接分析 Route B 识别与布局生成链路。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: Route B 导入保留横向表格结构 -> Given 原始 .doc 中存在 6 列横向物料表 / When Route B 识别器读取并构建 parsedTable / Then 后续报表 JSON 应保持原始列结构，而不是把右侧多列压成单列堆叠文本。`

## Milestones

1. M1：定位 Route B 导入实现入口。`completed`
2. M2：检查识别器、标题拆分规则与报表 JSON 构建器。`completed`
3. M3：结合样例文件和当前环境输出根因分析。`completed`

## Expected Verification

- `python -X utf8` 读取样例文件基础信息
- `python -X utf8` COM 打开样例 `.doc` 探测

## Final Verification Result

- 样例文件为 OLE `.doc`，文件头 `d0cf11e0a1b11ae1...`
- 本机 COM 打开该文件返回 `Kingsoft WPS: 文档打开失败`
