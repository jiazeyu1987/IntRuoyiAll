const assert = require('assert');
const fs = require('fs');
const path = require('path');

const moduleRoot = path.resolve(__dirname, '..', '..', '..');
const repoRoot = path.resolve(moduleRoot, '..', '..');
const read = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8');

const frontlinePqcService = read('IntRuoyiBackend', 'yudao-module-mes', 'src', 'main', 'java',
  'cn', 'iocoder', 'yudao', 'module', 'mes', 'service', 'pro', 'frontline',
  'MesFrontlinePqcContextServiceImpl.java');
const frontlinePqcTest = read('IntRuoyiBackend', 'yudao-module-mes', 'src', 'test', 'java',
  'cn', 'iocoder', 'yudao', 'module', 'mes', 'service', 'pro', 'frontline',
  'MesFrontlinePqcContextServiceTest.java');
const frontendPqcPanel = read('IntRuoyiFronted', 'src', 'views', 'mes', 'pro', 'feedback',
  'FrontlineFixedTemplatePanel.vue');

assert(frontlinePqcService.includes('arrangePqcProcessDisplayOrder(processes)'),
  'Frontline PQC must pass process responses through the display-order arranger.');
assert(frontlinePqcService.includes('List<MesFrontlinePqcProcessRespVO> productProcesses')
  && frontlinePqcService.includes('List<MesFrontlinePqcProcessRespVO> commonPackagingProcesses')
  && frontlinePqcService.includes('orderedProcesses.addAll(productProcesses)')
  && frontlinePqcService.includes('orderedProcesses.addAll(commonPackagingProcesses)'),
  'Frontline PQC display order must keep product QA before common packaging.');
assert(/productMaxSort[\s\S]*process\.setQaProcessSort\(\+\+nextSort\)/.test(frontlinePqcService),
  'Common packaging processes must be renumbered after the max product QA display sort.');
assert(frontlinePqcService.includes('Objects.equals("初包装过程检验规程", normalized)')
  && frontlinePqcService.includes('return "小包装"')
  && frontlinePqcService.includes('Objects.equals("大中包装过程检验规程", normalized)')
  && frontlinePqcService.includes('return "中大包装"'),
  'Common packaging process names must display as 小包装 and 中大包装.');
assert(/activeOrderTasks\.stream\(\)[\s\S]*\.sorted\(Comparator\.comparing\(MesPqcInspectionTaskDO::getId/.test(frontlinePqcService),
  'Common QA source replay must restore frozen source order by task id before collecting version ids.');

assert(frontlinePqcTest.includes('listProcessesPlacesCommonPackagingAfterAllProductQaProcesses')
  && frontlinePqcTest.includes('List.of(1, 2, 3, 4, 5, 6, 7, 8)')
  && frontlinePqcTest.includes('"小包装", "中大包装"')
  && frontlinePqcTest.includes('"PATROL", "PATROL_AM", "AM"'),
  'Regression test must cover full 1-8 ordering, packaging names, and task-id source order.');

const formatProcessLabelStart = frontendPqcPanel.indexOf('const formatProcessLabel');
const formatEmployeeLabelStart = frontendPqcPanel.indexOf('const formatEmployeeLabel', formatProcessLabelStart);
const formatProcessLabelBody = frontendPqcPanel.slice(formatProcessLabelStart, formatEmployeeLabelStart);
assert(formatProcessLabelBody.includes('qaProcessName')
  && !formatProcessLabelBody.includes('sourceText')
  && !formatProcessLabelBody.includes('通用包装'),
  'Frontend PQC label must not append common-regulation suffix text.');
