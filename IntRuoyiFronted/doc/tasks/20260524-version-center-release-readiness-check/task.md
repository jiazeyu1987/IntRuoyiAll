# 任务：版本中心合并后发布就绪性核查

## 任务目标

- 核查 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 当前 `int_main` 在版本中心合并后是否具备直接发布前置条件。
- 只做核查与结论输出，不执行真实发布、不改线上环境。

## 非目标

- 不执行测试服/正式服发布。
- 不修改代码或脚本，除非发现必须记录的阻塞。
- 不用 fallback 掩盖发布阻塞。

## 前序任务检查

- 已检查主线融合任务：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260523-showroom-version-center-impl\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-version-center-int-main-merge\task.md`
- 状态：已完成并已快进合入 `int_main`

## 里程碑

- [x] M1：建立核查任务记录。
- [x] M2：核对主仓工作树、分支头和最近提交。
- [x] M3：核对发布相关验证、脚本前置条件与已知阻塞。
- [x] M4：给出前端发布就绪结论。

## 预期验证

- `git status --short`
- 最近发布相关验证结果复核
- 发布脚本/发布前置条件核查

## 当前状态

- 状态：已完成

## 结论

- 当前前端侧已具备按现有发布脚本直接发布的条件。
- 已确认：
  - 当前前端主仓 `int_main` 头提交为 `2029bcd2`
  - 版本中心相关脚本回归仍通过：`48 PASS`
  - 按发布脚本真实口径模拟：
    - `NODE_OPTIONS=--max-old-space-size=8192`
    - `VITE_BASE_URL=http://172.30.30.58:48081`
    - `VITE_BASE_PATH=/`
    - `VITE_OUT_DIR=dist-intruoyi-test`
    - `pnpm exec vite build --mode test`
    - 连续两次 PASS
- 当前没有新的前端发布硬阻塞。

## 说明

- Website 仓 `npm run build` -> PASS
- 裸命令 `pnpm exec vite build --mode test` 与 `pnpm build:test` 在当前机器上仍可能因 `EMFILE` 失败；但这不等于发布脚本阻塞，因为发布脚本真实口径会先清理 `.vite` 并显式注入 `NODE_OPTIONS/VITE_*` 环境变量，本任务已验证这条实际发布路径恢复。
