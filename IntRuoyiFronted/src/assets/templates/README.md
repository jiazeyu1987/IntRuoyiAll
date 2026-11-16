# 生产工序 Word 模板

`production-process.docx` 是“生产解析 → 下载为Word集合”的唯一版式来源。
导出读取当前 JSON 编辑内容，每道工序从该模板创建一个 `.docx`，并把同一份模板原样放入 ZIP 的 `模板/生产工序模板.docx`。
文件名前的工序序号保证同名工序不会覆盖。

模板使用 Docxtemplater 标签，普通字段包括 `productName`、`productCode`、`sourceFileName`、`processNumber`、`processCount`、`processName`、`criticalProcess` 和 `schemaVersion`。
三张明细表分别使用 `{#materials}...{/materials}`、`{#equipment}...{/equipment}`、`{#parameters}...{/parameters}` 行循环；保留标签即可调整字体、列宽、边框与页眉页脚。
字段映射见 `src/views/form-center/parser/productionWordExport.ts`。

修改模板使用全局 OfficeCLI。首次构建脚本为 `scripts/create-production-process-word-template.py`，已存在模板时拒绝覆盖；不要在浏览器中重新拼装版式。
模板随前端资源打包，修改后需要重新构建前端。模板丢失、损坏、标签未提供或 JSON 无效时导出直接报错，不生成部分下载包。

验证命令：`node --test tests/unit/production-word-export.test.cjs`。
