const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const repoRoot = path.resolve(__dirname, '../../../../..');
const backendRoot = path.join(repoRoot, 'IntRuoyiBackend', 'yudao-module-mes', 'src', 'main', 'java');
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted', 'src');

const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');

const controller = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java');
const simulationService = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationService.java');
const activeOrderService = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java');
const activeOrderApi = read('IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts');
const workbench = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue');

assert.match(controller, /\/active-order\/simulate-completion/);
assert.match(controller, /MesTeamLeaderActiveOrderSimulationService/);
assert.match(simulationService, /EVENT_TYPE_PRODUCTION_SUBMIT/);
assert.match(simulationService, /createInitialAllocation/);
assert.match(simulationService, /TASK_STATUS_CONFIRMED/);
assert.match(simulationService, /insertBatch/);
assert.doesNotMatch(simulationService, /setProductionProgressPercent|setInspectionProgressPercent/);
assert.doesNotMatch(simulationService, /taskProcessIdentities\.containsAll\(formalIdentitySet\)/);
assert.doesNotMatch(simulationService, /PQC 任务未覆盖全部正式生产工序/);
assert.doesNotMatch(simulationService, /PQC 任务确认后仍未覆盖全部工序/);
assert.match(simulationService, /calculateInspectionProgressPercent\(activeOrder, formalIdentitySet, pqcTasks/);
assert.match(activeOrderService, /pqcTasksByActiveOrderId/);
assert.match(activeOrderService, /calculateInspectionProgressPercent\(pqcTasks/);
assert.match(activeOrderApi, /simulateTeamLeaderActiveOrderCompletion/);
assert.match(workbench, /data-team-leader-simulate-active-order-completion/);
assert.match(workbench, /simulateTeamLeaderActiveOrderCompletion\(/);
assert.match(workbench, /ElMessageBox\.confirm/);
assert.match(workbench, /确认使用模拟数据完成当前活跃订单/);
assert.match(workbench, /系统会模拟一线生产提交、生产组长复核、一线 PQC 提交和 PQC 组长复核/);
assert.match(workbench, /确认模拟/);
assert.match(workbench, /生产进度 \$\{formatActiveOrderProgressPercent\(result\.productionProgressPercent\)\}/);
assert.match(workbench, /检验进度 \$\{formatActiveOrderProgressPercent\(result\.inspectionProgressPercent\)\}/);
assert.match(workbench, /loadActiveOrders\(\)/);

console.log('mes-active-order-simulation-complete-static: PASS');
