# 共享 Word 表格解析服务设计

## Task Goal

为“表单模板解析 Word”和“批记录表单 Word 导入”设计共享 Word 解析服务方案：保留各自业务接口，抽出公共解析能力，避免 BPM 与 MES 模块形成循环依赖。

## Milestones

- [x] 确认现有表单中心与批记录 Word 导入接口、后端服务和解析器边界。
- [x] 读取系统设计文档技能、任务收尾规则、PowerShell/编码规则和命中的 Word 表格解析经验门禁。
- [x] 编写共享 Word 解析服务系统设计文档。
- [x] 执行文档结构与 UTF-8 读取验证。
- [x] 记录验证结果并更新任务状态。

## Expected Verification

- `python -X utf8` 能读取新增 Markdown 文档。
- `rg` 能定位新增设计文档的关键章节、接口名和保留接口约束。
- 不修改生产代码、不触碰既有未提交改动。

## Current Status

ready_for_closeout

## Blockers

- Git final closeout is blocked by pre-existing shared-branch state: `git status --short --branch` reports `int_main...origin/int_main [ahead 8]` plus unrelated dirty/untracked files outside this task. This task did not stage, commit, push, or modify unrelated files.

## 经验门禁

### eDHR 批记录 Word 表格解析门禁

- Trigger: 本设计涉及批记录 Word 导入、共享 parser、表格结构识别、packed 物料矩阵、说明块和截图位置一致性。
- Preflight check: 设计必须要求真实源 DOC 与最小合成表格双重验证，且将结构偏差定位到共享 parser/calibrator/row-type 规则。
- Blocker: 缺少真实源 DOC、缺少稳定 RED 复现或测试 fixture 不存在时，后续实现不得宣称完成。
- Verification: 后续实现回归必须同时覆盖合成 RED/GREEN、用户指定真实 DOC 样本、packed 括号续行、物料错位和说明块边界。
- Forbidden action: 禁止按表单名、工序名、文件名或模板名写硬编码特例；禁止只靠截图人工判断完成。
- Evidence: `docs/backend-development.md#edhr-批记录-word-表格解析门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；设计明确要求缺少解析器、文件类型不支持、解析失败和缺少真实 fixture 时 fail fast。
- `是否从根因和长期维护角度解决`：是；通过新增下层共享解析模块解决 BPM/MES 解析能力重复和接口误合并风险。
- `是否存在临时补丁或绕过`：否；不直接把表单模板前端改打 MES 批记录业务接口，不复制 MES 业务 parser 到 BPM。
