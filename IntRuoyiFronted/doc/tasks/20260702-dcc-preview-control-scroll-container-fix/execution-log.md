# 执行日志：DCC 预览控制栏滚动容器修复

- BDD: PDF 后续页仍可操作控制栏 -> Given 用户打开多页 PDF 受控预览 / When 在预览区内滚动到第 2 页或后续页面 / Then 缩放、放大、左旋转、右旋转控制栏仍在预览区顶部可见。
- BDD: 可变换预览使用内部滚动视口 -> Given 用户打开 PDF 或图片预览 / When 内容高度超过预览区 / Then 鼠标滚轮滚动预览框内部内容，而不是把控制栏从视口内推走。
- BDD: 非可变换类型不受影响 -> Given 用户打开文本、音频、视频、OnlyOffice 或仅下载类型 / When 页面渲染 / Then 不显示缩放旋转控制栏，也不强加 PDF / 图片的滚动视口契约。
- GREEN: experience-preflight -> PASS，已读取经验索引、PowerShell 记忆、前端风格与 bug 回归修复契约。
- RED: node tests/e2e/dcc-common-file-preview-source.spec.js -> FAIL，生产代码缺少 protected-viewer-frame--transformable 内部滚动视口契约。
- GREEN: implementation -> PASS，PDF / 图片预览框已加入 protected-viewer-frame--transformable，并设置 max-height 与 overscroll-behavior。
- GREEN: node tests/e2e/dcc-common-file-preview-source.spec.js -> PASS。
- GREEN: node tests/e2e/dcc-controlled-file-protection.contract.test.js -> PASS。
- GREEN: git diff --check scoped to DCC files -> PASS。
- GREEN: local Vite served source/style markers -> PASS，运行态源码包含 transformable frame，样式包含 max-height 与 overscroll containment。
- GREEN: pnpm ts:check -> PASS，全量类型检查通过。
- GREEN: task-closeout-cleanup -> PASS，完成任务收尾清理预览与应用。
