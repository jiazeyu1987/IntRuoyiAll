# eDHR 批次 PDF/A 最小归档闭环

## Task Goal

在独立 worktree 中完成新生成 eDHR 批次最终归档的 PDF/A-1b 合规生成、独立校验、不可变存储、原文件下载和历史页状态展示，并在验证通过后快进融合到 `int_main`。

## Scope

- 新生成的批次最终归档必须是经过独立校验的 PDF/A-1b。
- 归档成功时必须保存生成时的原始字节、SHA-256 和受保护存储证据。
- 下载必须读取封存原文件并复核 SHA-256，禁止根据 manifest 重新生成。
- 模板布局、字体、PDF/A 校验或受保护存储任一失败时，批次不得进入已归档，归档任务不得完成。
- 历史追溯页面展示 PDF/A 类型与校验状态，并沿用现有查看、下载、打印入口。
- 历史普通 PDF 不回填、不转换、不冒充 PDF/A。

## Non-Goals

- 不实现 PDF/A-2、PDF/A-3 或附件文件内嵌。
- 不在本节点转换历史归档。
- 不在本节点完成所有产品模板与纸质文件的逐页基线比对。
- 不改变批记录放行、签名或附件上传业务规则。

## Milestones

1. `M1`：冻结数据合同、BDD 场景和 RED 测试。
2. `M2`：实现 PDF/A-1b 渲染与独立校验。
3. `M3`：实现批次归档原文件受保护存储、数据库字段和原文件下载。
4. `M4`：实现历史页 PDF/A 状态与失败反馈。
5. `M5`：完成后端、数据库、前端、PDF 格式及视觉验证。
6. `M6`：收尾、提交并快进融合到 `int_main`。

## Expected Verification

- 目标 JUnit 先 RED 后 GREEN，覆盖 PDF/A 通过与失败、状态门禁、原文件下载和哈希不一致。
- 迁移合同测试覆盖新增字段及幂等迁移结构。
- 前端静态合同与 `pnpm ts:check` 覆盖 profile/status 展示、加载态和错误透传。
- 使用 PDF/A 校验器验证生成样本。
- 使用 Poppler 将样本渲染为 PNG，检查页面非空、文字可读、无裁切或黑块。
- 运行 `git diff --check` 和分支运行端口 guard。
- 合并前检查 `int_main` 脏文件与任务改动无路径冲突，并使用 `git merge --ff-only`。

## Applicable Gates

- 新归档只在 PDF/A 校验和受保护存储证据同时通过后才能标记 `SEALED/ARCHIVED`。
- 下载内容必须来自封存文件并与数据库 SHA-256 一致。
- 运行时缺少 PDF/A 校验依赖、字体资源、受保护存储配置或正式表结构时必须明确失败。
- 历史未验证归档不得显示 PDF/A 合规标识。
- 当前任务不启动子 Agent；所有实施与验证由当前 Agent 在任务 worktree 内完成。
- PDF 相关 Maven 若出现当前模块 `target/classes` 陈旧或缺 class，先按项目经验只清理当前 MES 模块并用带 `-am` 的目标命令复验；不得把陈旧 class 当业务失败，也不得用旧 Surefire 报告冒充通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。任何 PDF/A、模板、字体、存储或哈希失败均阻断归档。
- `是否从根因和长期维护角度解决`：是。保存并下载不可变原文件，消除当前下载时重新渲染的问题。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout - 开发和任务范围验证已完成，待清理任务临时产物、提交并融合到 `int_main`。

## Cleanup Candidates

- tmp/pdfs/
- IntRuoyiFronted/dist/
