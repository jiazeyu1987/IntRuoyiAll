# Execution Log

BDD: 前端编译检查 -> Given 需要确认 `yudao-ui-admin-vue3` 前端是否可构建 When 执行仓库规定的构建命令 Then 应记录 PASS 或精确的失败原因
RED: `node node_modules\\vue-tsc\\bin\\vue-tsc.js --noEmit --pretty false` -> FAIL, Node heap out of memory before type-check finished
GREEN: `node --max-old-space-size=8192 node_modules\\vue-tsc\\bin\\vue-tsc.js --noEmit --pretty false` -> PASS
GREEN: `node --max-old-space-size=8192 node_modules\\vite\\bin\\vite.js build --mode prod` -> PASS
