const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const detailPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

const readStyleBlock = (selector) => {
  const start = detailPage.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = detailPage.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return detailPage.slice(start, end)
}

assert.match(
  detailPage,
  /const releaseStageViewModel = computed/,
  '放行详情页必须用 releaseStageViewModel 集中计算当前阶段 UI。'
)

assert.match(
  detailPage,
  /const releaseStageActionItems = computed/,
  '放行详情页必须只渲染当前阶段允许显示的动作列表。'
)

assert.match(
  detailPage,
  /const batchCurrentPositionViewModel = computed/,
  '放行详情页红框必须用 batchCurrentPositionViewModel 单独计算批次当前位置，不能复用右侧放行推进模型。'
)

assert.match(
  detailPage,
  /const releaseFlowStepsViewModel = computed/,
  '放行详情页主区域必须用 releaseFlowStepsViewModel 集中计算放行流程指示图。'
)

assert.match(
  detailPage,
  /v-for="action in releaseStageActionItems"/,
  '右侧放行操作栏必须由当前阶段动作列表驱动，而不是一次性展示所有按钮。'
)

const specialTaskEntriesStart = detailPage.indexOf('const specialTaskEntries = computed')
const specialTaskEntriesEnd = detailPage.indexOf('const preProcessSpecialTaskEntries = computed', specialTaskEntriesStart)
assert.ok(specialTaskEntriesStart > 0 && specialTaskEntriesEnd > specialTaskEntriesStart, '必须能定位左侧特殊节点列表过滤逻辑。')
const specialTaskEntriesBlock = detailPage.slice(specialTaskEntriesStart, specialTaskEntriesEnd)
assert.match(specialTaskEntriesBlock, /isSpecialNode\(task\)/, '左侧特殊节点列表必须来源于特殊节点任务。')
assert.doesNotMatch(
  specialTaskEntriesBlock,
  /hasActiveWorkTask\(task\) \|\| !task\.executionId/,
  '左侧特殊节点列表不得因为已有 executionId 且暂无活跃待办而隐藏只上传节点。'
)
assert.doesNotMatch(
  specialTaskEntriesBlock,
  /EDHR_BATCH_TASK_STATUS_APPROVED|EDHR_BATCH_TASK_STATUS_SKIPPED/,
  '左侧特殊节点是流程导航节点，不得因为已完成或已跳过而从左侧列表消失。'
)
assert.doesNotMatch(
  specialTaskEntriesBlock,
  /!isOptionalTask\(task\)|isOptionalTask\(task\)/,
  '左侧特殊节点列表不得复用普通表单可选/待办过滤逻辑。'
)
assert.match(detailPage, /EDHR_BATCH_NODE_STERILIZATION_REPORT[\s\S]*'灭菌报告'/, '左侧列表必须支持灭菌报告特殊节点。')
assert.match(detailPage, /EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_REPORT[\s\S]*'成品检报告'/, '左侧列表必须支持成品检报告特殊节点。')
assert.match(detailPage, /EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_RECORD[\s\S]*'成品检记录'/, '左侧列表必须支持成品检记录特殊节点。')

const mainReleaseSummaryStart = detailPage.indexOf('aria-label="批次当前位置摘要"')
const rightRailStart = detailPage.indexOf('<aside class="edhr-batch-detail__review-rail"')
assert.ok(mainReleaseSummaryStart > 0, '红框必须声明为批次当前位置摘要。')
assert.ok(rightRailStart > mainReleaseSummaryStart, '右侧放行栏必须位于批次当前位置摘要之后。')

const mainReleaseSummary = detailPage.slice(mainReleaseSummaryStart, rightRailStart)
assert.match(mainReleaseSummary, /当前位置：\{\{ batchCurrentPositionViewModel\.blockerLabel \}\}/, '红框标题必须使用中性的当前位置描述。')
assert.match(mainReleaseSummary, /状态说明/, '红框必须展示中性的状态说明，不能把所有阶段都写成阻塞原因。')
assert.match(mainReleaseSummary, /处理要求/, '红框必须展示正式的处理要求。')
assert.match(mainReleaseSummary, /责任人/, '红框必须展示这一步的责任人。')
assert.match(mainReleaseSummary, /当前执行步骤/, '红框必须展示当前执行步骤。')
assert.match(mainReleaseSummary, /当前任务\/表单/, '红框必须展示当前任务或表单。')
assert.match(detailPage, /当前待处理事项/, '批次顶部待处理列表标题必须使用中性描述。')
assert.doesNotMatch(detailPage, /当前阻塞项/, '批次顶部待处理列表不得与“当前位置”描述混用。')
assert.match(mainReleaseSummary, /batchCurrentPositionViewModel\.blockerReason/, '红框必须绑定阻塞原因视图模型。')
assert.match(mainReleaseSummary, /batchCurrentPositionViewModel\.nextActionText/, '红框必须绑定处理要求视图模型。')
assert.match(mainReleaseSummary, /batchCurrentPositionViewModel\.ownerLabel/, '红框必须绑定责任人视图模型。')
assert.doesNotMatch(mainReleaseSummary, /当前卡点|卡住原因|卡住|卡在|推进联系人/, '红框不得出现口语化或职责重复的描述。')
assert.doesNotMatch(mainReleaseSummary, /当前阻塞：|阻塞原因/, '红框标题和字段不得把所有阶段都描述为阻塞。')
assert.doesNotMatch(mainReleaseSummary, /下一步联系/, '红框不得再展示右侧推进区的“下一步联系”卡片。')
assert.doesNotMatch(mainReleaseSummary, /放行状态/, '红框不得重复右侧放行状态。')
assert.doesNotMatch(mainReleaseSummary, /归档状态/, '红框不得重复右侧归档状态。')

const releaseFlowStart = detailPage.indexOf('aria-label="放行流程指示图"')
assert.ok(releaseFlowStart > mainReleaseSummaryStart, '放行流程指示图必须位于红框阻塞说明之后。')
assert.ok(releaseFlowStart < rightRailStart, '放行流程指示图必须位于主区域、右侧操作区之前。')

const releaseMainWorkspaceStart = detailPage.indexOf(
  'class="edhr-batch-detail__release-main-workspace"',
  mainReleaseSummaryStart
)
assert.ok(
  releaseMainWorkspaceStart > mainReleaseSummaryStart && releaseMainWorkspaceStart < rightRailStart,
  '放行流程必须包在中间主区域剩余空间容器内，不能漂移到右侧当前阶段操作区。'
)

const rightRailEnd = detailPage.indexOf('</aside>', rightRailStart)
assert.ok(rightRailEnd > rightRailStart, '必须能定位右侧当前阶段操作区结束位置。')
const rightRailBlock = detailPage.slice(rightRailStart, rightRailEnd)
assert.doesNotMatch(
  rightRailBlock,
  /放行流程|releaseFlowStepsViewModel|edhr-batch-detail__release-flow-card/,
  '右侧当前阶段操作区不得包含放行流程图标题、模型或卡片。'
)

const releaseFlowEnd = detailPage.indexOf('<el-empty v-else-if="!selectedProcessContext"', releaseFlowStart)
assert.ok(releaseFlowEnd > releaseFlowStart, '必须能定位放行流程图结束位置。')
const releaseFlowBlock = detailPage.slice(releaseFlowStart, releaseFlowEnd)
const releaseFlowStepsModelStart = detailPage.indexOf('const releaseFlowStepsViewModel = computed')
const releaseFlowStepsModelEnd = detailPage.indexOf('const terminalReleaseActionItems', releaseFlowStepsModelStart)
assert.ok(
  releaseFlowStepsModelStart > 0 && releaseFlowStepsModelEnd > releaseFlowStepsModelStart,
  '必须能定位放行流程图 ViewModel 文案。'
)
const releaseFlowStepsModelBlock = detailPage.slice(releaseFlowStepsModelStart, releaseFlowStepsModelEnd)

assert.match(releaseFlowBlock, /放行流程/, '流程图必须显示“放行流程”标题。')
assert.match(releaseFlowBlock, /v-for="step in releaseFlowStepsViewModel"/, '流程图必须由 releaseFlowStepsViewModel 驱动渲染。')
assert.match(releaseFlowBlock, /:class="\[\s*`is-\$\{step\.state\}`,[\s\S]*'is-current': step\.state === 'current'/, '流程节点必须绑定当前态样式。')
assert.match(releaseFlowBlock, /'is-completed': step\.state === 'completed'/, '流程节点必须绑定已完成态样式。')
assert.match(releaseFlowBlock, /'is-pending': step\.state === 'pending'/, '流程节点必须绑定待处理态样式。')
assert.match(releaseFlowBlock, /'is-failed': step\.state === 'failed'/, '流程节点必须绑定执行失败态样式。')
assert.doesNotMatch(releaseFlowBlock, /'is-blocked': step\.state === 'blocked'|'is-terminal': step\.state === 'terminal'/, '流程节点颜色语义不得继续使用 blocked 或 terminal 作为独立颜色状态。')
assert.match(releaseFlowBlock, /step\.description/, '流程节点必须展示每一步做什么。')
assert.match(releaseFlowBlock, /责任人：\{\{ step\.ownerLabel \}\}/, '流程节点必须统一使用“责任人”标签。')
assert.match(releaseFlowBlock, /<button[\s\S]*@click="selectReleaseFlowStep\(step\.key\)"/, '流程卡片必须支持点击切换右侧阶段面板。')
assert.match(
  releaseFlowBlock,
  /class="edhr-batch-detail__release-flow-trace-action"[\s\S]*@click="openTraceRecordGroup"[\s\S]*追溯记录/,
  '追溯记录必须固定在中间放行流程区域左下角。'
)
assert.doesNotMatch(
  releaseFlowBlock,
  /openCloseDialog|openQualityRejectDialog|openReopenBatchDialog|handleReleasePrecheck|openReleaseTransactionDialog|handleGenerateArchive|router-link/i,
  '流程卡片只能做右侧面板导航，不得直接承载放行推进动作或路由跳转。'
)

for (const requiredFlowCopy of [
  '放行预检',
  '生产执行资料收齐后先执行放行预检；未通过时批次保持未关闭，可继续修订。',
  '关闭批次',
  '放行预检通过后关闭批次，冻结批记录后进入放行审批。',
  '放行审批',
  '放行审批负责人处理批准、驳回或撤回。',
  '质量已拒收',
  '质量拒收后进入已拒收分支，不继续普通放行。'
]) {
  assert.match(releaseFlowStepsModelBlock, new RegExp(requiredFlowCopy), `流程图必须包含固定文案：${requiredFlowCopy}`)
}

assert.doesNotMatch(releaseFlowStepsModelBlock, /key:\s*'archive'[\s\S]*title:\s*'归档打印'/, '流程图不得继续显示归档打印卡片。')
assert.doesNotMatch(releaseFlowStepsModelBlock, /key:\s*'archived'[\s\S]*title:\s*'已归档'/, '流程图不得继续显示已归档卡片。')
assert.doesNotMatch(releaseFlowStepsModelBlock, /质量终态/, '流程图分支不得再使用“质量终态”文字描述。')
assert.match(
  releaseFlowBlock,
  /'is-quality-terminal-branch': step\.key === 'quality-terminal'/,
  '质量已拒收必须作为普通流程外的独立分支卡片渲染。'
)
assert.match(
  releaseFlowBlock,
  /edhr-batch-detail__release-flow-branch-line/,
  '质量已拒收分支必须渲染从放行预检连过来的分支线。'
)
assert.match(
  detailPage,
  /releaseFlowStepsViewModel[\s\S]*currentKey === 'quality-terminal'[\s\S]*state: 'failed'/,
  '质量已拒收必须作为红色执行失败分支计算。'
)
assert.match(
  detailPage,
  /const normalFlowKeys[\s\S]*'precheck'[\s\S]*'close'[\s\S]*'release-approval'[\s\S]*'archive'[\s\S]*'archived'/,
  '普通流程必须按放行预检、关闭批次、放行审批、归档打印、已归档顺序计算。'
)
assert.doesNotMatch(releaseFlowStepsModelBlock, /关闭后才能进入放行预检/, '流程图不得保留先关闭后预检的旧文案。')
assert.doesNotMatch(detailPage, /收尾关闭/, '放行流程与当前阶段标题不得继续使用“收尾关闭”，应统一为“关闭批次”。')
assert.doesNotMatch(detailPage, /放行检查/, '预检阶段的用户可见入口和说明必须统一为“放行预检”。')
assert.doesNotMatch(detailPage, /质量终态拒收|终态拒收|已驳回\/质量终态/, '质量拒收动作和质量终态状态不得混写为“终态拒收”或“已驳回/质量终态”。')
assert.doesNotMatch(detailPage, /QA 放行负责人|质量负责人 \/ QA|QA 检查/, '用户可见责任角色不得混用 QA，应统一为质量负责人或质量放行责任人。')
assert.doesNotMatch(detailPage, /聚合Hash|签核证据 hash|下载打印版PDF|当前责任：/, '放行详情页不得保留明显中英混写或责任标签旧说法。')
assert.match(
  detailPage,
  /type ReleaseFlowStepState = 'completed' \| 'current' \| 'pending' \| 'failed'/,
  '流程状态枚举必须收敛为已完成、正在执行、未开始、执行失败四类颜色语义。'
)
assert.match(detailPage, /statusLabel: '已执行完成'/, '已完成节点必须显示“已执行完成”。')
assert.match(detailPage, /statusLabel: '正在执行'/, '当前节点必须显示“正在执行”。')
assert.match(detailPage, /statusLabel: '执行失败'/, '失败节点必须显示“执行失败”。')
assert.match(detailPage, /statusLabel: '未开始执行'/, '未开始节点必须显示“未开始执行”。')
assert.match(
  detailPage,
  /if \(stepKey === 'precheck'\) \{[\s\S]*releaseStatus\.value === 'PRECHECK_FAILED'[\s\S]*return 'failed'/,
  '预检失败必须进入红色执行失败态。'
)

assert.match(detailPage, /blockerLabel:/, '批次当前位置模型必须计算当前位置。')
assert.match(detailPage, /blockerReason:/, '批次当前位置模型必须计算状态说明。')
assert.match(detailPage, /nextActionText:/, '批次当前位置模型必须计算处理要求。')
assert.match(detailPage, /resolveCurrentPositionDiagnosis/, '批次当前位置模型必须集中计算阻塞诊断，避免模板硬编码阶段逻辑。')
assert.match(detailPage, /hasCurrentPositionBlockers/, '批次当前位置模型必须先判断是否存在关闭阻塞项。')
assert.match(
  detailPage,
  /batchStatus\.value === EDHR_BATCH_STATUS_READY_TO_CLOSE && hasCurrentPositionBlockers\(\)/,
  '待关闭但仍有关闭阻塞项时，红框不得直接提示关闭批次。'
)
assert.match(detailPage, /resolveCurrentBlockingStepLabel/, '待关闭但有阻塞项时，红框当前位置必须优先解析真实阻塞步骤。')
assert.match(detailPage, /成品检卷宗项未完成: PENDING/, '静态契约必须覆盖成品检卷宗项未完成这类真实阻塞文案。')
assert.match(detailPage, /blockerLabel: resolveCurrentBlockingStepLabel\(\)/, '待关闭阻塞标题必须显示具体步骤，不能固定显示关闭前阻塞。')
assert.doesNotMatch(detailPage, /blockerLabel: '关闭前阻塞'/, '待关闭但有阻塞项时不得再把红框当前位置固定为关闭前阻塞。')
assert.match(detailPage, /预检通过后才能关闭批次/, '红框必须解释关闭前必须先通过放行预检。')
assert.match(
  detailPage,
  /const canAttemptClose = computed\([\s\S]*batchStatus\.value === EDHR_BATCH_STATUS_READY_TO_CLOSE[\s\S]*detail\.value\?\.canClose === true[\s\S]*hasBatchCloseAction\.value[\s\S]*releasePrecheckPassed\.value/,
  '待关闭 READY_TO_CLOSE 状态必须先通过放行预检后才允许点击关闭批次。'
)
assert.doesNotMatch(
  detailPage,
  /const canAttemptClose = computed\([\s\S]*\(detail\.value\?\.canClose === true \|\| hasBatchCloseAction\.value\)[\s\S]*\)/,
  '关闭按钮可用性不得只依赖 canClose 或任务 CLOSE 动作。'
)
assert.doesNotMatch(detailPage, /当前卡点|卡住原因|卡住原因中的|卡在返工收尾|卡在工序收尾/, '页面源码不得继续保留放行红框相关口语化文案。')
assert.doesNotMatch(
  detailPage,
  /batchStatus\.value === EDHR_BATCH_STATUS_CLOSED && canGenerateArchive\.value\) return 'archive'/,
  '批次已关闭但放行仍待预检时，不能因为 canArchive=true 就跳到归档打印阶段。'
)
assert.match(
  detailPage,
  /releaseStatus\.value === 'RELEASED'[\s\S]*return 'archive'[\s\S]*batchStatus\.value === EDHR_BATCH_STATUS_CLOSED\) return 'precheck'/,
  '只有放行已批准后才进入归档阶段；已关闭但待预检必须先显示放行预检。'
)
assert.match(
  detailPage,
  /batchStatus\.value === EDHR_BATCH_STATUS_READY_TO_CLOSE && releasePrecheckPassed\.value\) return 'close'[\s\S]*batchStatus\.value === EDHR_BATCH_STATUS_READY_TO_CLOSE\) return 'precheck'/,
  '待关闭批次必须先停在放行预检；只有预检通过后才进入关闭批次阶段。'
)
assert.match(
  detailPage,
  /const canRunReleasePrecheck = computed\([\s\S]*batchStatus\.value === EDHR_BATCH_STATUS_READY_TO_CLOSE[\s\S]*!\['PRECHECK_PASSED', 'PENDING_APPROVAL', 'RELEASED'\]/,
  '放行预检按钮必须在待关闭状态可执行，并排除已预检通过、审批中和已放行状态。'
)
assert.match(
  detailPage,
  /const closePrecheckRequiredBeforeClose = computed\([\s\S]*!releasePrecheckPassed\.value[\s\S]*batchStatus\.value === EDHR_BATCH_STATUS_READY_TO_CLOSE/,
  '关闭批次失败提示必须能识别“尚未通过放行预检”的门禁原因。'
)
assert.match(
  detailPage,
  /请先执行并通过放行预检，再关闭批次。/,
  '关闭批次在预检未通过时必须提示先通过放行预检。'
)

const rightRail = detailPage.slice(rightRailStart)
const releaseRightRailBranchStart = rightRail.indexOf('<template v-if="isReleaseProcessSelected">')
const releaseRightRailBranchEnd = rightRail.indexOf('<template v-else>', releaseRightRailBranchStart)
assert.ok(releaseRightRailBranchStart >= 0 && releaseRightRailBranchEnd > releaseRightRailBranchStart, '必须能定位放行右侧操作区模板分支。')
const releaseRightRailBranch = rightRail.slice(releaseRightRailBranchStart, releaseRightRailBranchEnd)
assert.match(
  detailPage,
  /const resolveStageAwareReleaseStatusTitle = \(stageKey: ReleaseStageKey = resolveReleaseStageKey\(\)\) =>[\s\S]*stageKey === 'close' \? '当前阶段状态' : '当前放行状态'/,
  '阶段状态标题必须保留统一计算，供批次当前位置模型和后续扩展复用。'
)
assert.match(
  detailPage,
  /const resolveStageAwareReleaseStatusSummary = \(stageKey: ReleaseStageKey = resolveReleaseStageKey\(\)\) =>/,
  '状态文案必须通过阶段化函数计算，避免关闭批次阶段直接展示原始已放行标签。'
)
assert.match(
  detailPage,
  /if \(stageKey === 'close'\) return '关闭批次'/,
  '关闭批次阶段的状态值必须显示“关闭批次”，不能显示为“未进入正式放行”或“已放行”。'
)
assert.match(
  detailPage,
  /case 'close':[\s\S]*releaseStatusTitle,[\s\S]*releaseStatusLabel: resolveStageAwareReleaseStatusSummary\('close'\)/,
  '关闭批次阶段必须同时使用阶段化状态标题和状态文案。'
)
const closeStageCaseStart = detailPage.indexOf("case 'close':")
const closeStageCaseEnd = detailPage.indexOf('default:', closeStageCaseStart)
const closeStageCase = detailPage.slice(closeStageCaseStart, closeStageCaseEnd)
assert.doesNotMatch(
  closeStageCase,
  /releaseStatusLabel,\s*\r?\n\s*nextOwnerLabel: currentStageOwner/,
  '关闭批次阶段不得继续直接复用原始 releaseStatusLabel。'
)
assert.doesNotMatch(
  releaseRightRailBranch,
  /edhr-batch-detail__rail-summary/,
  '放行右侧操作区按钮下方不得继续渲染重复的状态/归档摘要卡片。'
)
assert.doesNotMatch(
  releaseRightRailBranch,
  /放行状态|归档状态|下一步责任人|releaseStageViewModel\.nextStepText/,
  '放行右侧操作区不得重复展示状态摘要、归档状态或推进说明，只保留当前阶段动作。'
)
assert.doesNotMatch(
  releaseRightRailBranch,
  /resolveStageAwareReleaseStatusTitle\(\)|resolveStageAwareReleaseStatusSummary\(\)|resolveArchiveStatusSummary\(\)/,
  '放行右侧操作区按钮下方不得继续调用阶段状态或归档状态摘要函数。'
)
assert.doesNotMatch(
  releaseRightRailBranch,
  /追溯记录|trace-record|openTraceRecordGroup/,
  '放行右侧阶段动作区不得重复展示追溯记录入口。'
)
assert.match(rightRail, /v-if="action\.permission"[\s\S]*v-hasPermi="action\.permission"/, '右侧操作按钮必须按 action.permission 保留权限控制。')
assert.match(rightRail, /v-else[\s\S]*@click="action\.onClick"/, '无权限元数据的右侧操作按钮仍必须正常渲染。')
assert.match(
  detailPage,
  /\.edhr-batch-detail__review-rail\s*\{[\s\S]*display: flex;[\s\S]*flex-direction: column;[\s\S]*gap: 10px;/,
  '右侧栏必须保持纵向 flex 布局，供放行动作区填充剩余空间。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-rail-actions\s*\{[\s\S]*grid-template-rows: repeat\(auto-fit, minmax\(52px, 1fr\)\);[\s\S]*flex: 1;[\s\S]*min-height: 0;/,
  '放行动作按钮区域必须按等高网格拉伸并填满剩余空间。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-rail-actions :deep\(\.el-button\)\s*\{[\s\S]*height: 100%;[\s\S]*min-height: 52px;[\s\S]*font-weight: 700;/,
  '放行动作按钮必须增大高度，保持整块可点击。'
)
const releaseMainWorkspaceStyle = readStyleBlock('.edhr-batch-detail__release-main-workspace')
for (const requiredStyle of ['flex: 1', 'min-height: 0', 'overflow: auto']) {
  assert.ok(
    releaseMainWorkspaceStyle.includes(requiredStyle),
    `放行主区域流程说明容器必须填满中间剩余空间：${requiredStyle}`
  )
}
const releaseFlowCardStyle = readStyleBlock('.edhr-batch-detail__release-flow-card')
assert.ok(releaseFlowCardStyle.includes('height: 100%'), '放行流程卡片必须在中间剩余空间内撑满高度。')
const releaseFlowTraceStyle = readStyleBlock('.edhr-batch-detail__release-flow-trace')
assert.ok(
  releaseFlowTraceStyle.includes('justify-content: flex-start'),
  '追溯记录按钮容器必须左对齐，确保按钮位于中间放行流程区域左下角。'
)

assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-step\.is-current\s*\{[\s\S]*border-color: #f59e0b;[\s\S]*background: #fff7e6;[\s\S]*box-shadow: inset 3px 0 0 #f59e0b;/,
  '正在执行的流程节点必须使用黄色背景。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-step\.is-completed\s*\{[\s\S]*border-color: #b7ebc6;[\s\S]*background: #f6fffa;/,
  '已执行完成的流程节点必须使用绿色背景。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-step\.is-failed\s*\{[\s\S]*border-color: #f6c9c9;[\s\S]*background: #fff7f7;[\s\S]*box-shadow: inset 3px 0 0 #cf1322;/,
  '执行失败的流程节点必须使用红色背景。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-step\.is-pending\s*\{[\s\S]*border-color: #dbe3ef;[\s\S]*background: #ffffff;/,
  '未开始执行的流程节点必须保持无语义颜色背景。'
)
assert.doesNotMatch(
  detailPage,
  /\.edhr-batch-detail__release-flow-step\.is-pending\s*\{[\s\S]*(#fed7aa|#fffaf0|#fff7e6)/,
  '未开始执行的流程节点不得使用黄色或橙色背景。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-steps\s*\{[\s\S]*grid-template-columns: repeat\(3, minmax\(0, 1fr\)\);[\s\S]*grid-auto-rows: minmax\(136px, auto\);/,
  '流程图必须收敛为 3 列网格，并用统一行高保证质量已拒收卡片与其它卡片同尺寸。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-step\s*\{[\s\S]*min-height: 136px;[\s\S]*height: 100%;/,
  '每张流程卡片必须使用一致的最小高度和高度拉伸。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-step\.is-quality-terminal-branch\s*\{[\s\S]*grid-column: 3;[\s\S]*grid-row: 2;/,
  '质量已拒收分支卡片必须固定放在第 3 列第 2 行，也就是截图红框位置。'
)
assert.match(
  detailPage,
  /\.edhr-batch-detail__release-flow-branch-line\s*\{[\s\S]*position: absolute;[\s\S]*border-bottom: 2px solid #cf1322;/,
  '质量已拒收分支必须用红色连线标识从第 2 步分出。'
)

assert.match(detailPage, /permission\?: string\[]/, '放行操作项必须支持按钮权限元数据。')
assert.match(
  detailPage,
  /const terminalReleaseActionItems = \(\): ReleaseStageActionItem\[] => \[[\s\S]*label: '关闭批次'[\s\S]*type: 'success'[\s\S]*permission: \['mes:pro-edhr-batch-execution:close'\][\s\S]*disabled: !canAttemptClose\.value[\s\S]*onClick: openCloseDialog/,
  '关闭批次必须直接作为右侧绿色操作按钮，并沿用关闭权限、禁用条件和弹框函数。'
)
assert.match(
  detailPage,
  /const qualityRejectActionItem = \(\): ReleaseStageActionItem => \(\{[\s\S]*key: 'quality-reject'[\s\S]*label: '质量拒收'[\s\S]*type: 'danger'[\s\S]*permission: \['mes:pro-edhr-batch-execution:quality-reject'\][\s\S]*disabled: !canQualityReject\.value[\s\S]*onClick: openQualityRejectDialog/,
  '质量拒收必须作为第 2 步放行预检的独立红色操作按钮，并沿用拒收权限、禁用条件和弹框函数。'
)
assert.match(
  detailPage,
  /const terminalReleaseActionItems = \(\): ReleaseStageActionItem\[] => \[[\s\S]*label: '申请重开'[\s\S]*type: 'danger'[\s\S]*permission: \['mes:pro-edhr-change:reopen'\][\s\S]*disabled: !canRequestReopen\.value[\s\S]*onClick: openReopenBatchDialog/,
  '申请重开必须直接作为右侧红色操作按钮，并沿用重开权限、禁用条件和弹框函数。'
)
const terminalReleaseActionItemsStart = detailPage.indexOf('const terminalReleaseActionItems = (): ReleaseStageActionItem[] => [')
const terminalReleaseActionItemsEnd = detailPage.indexOf('const qualityRejectActionItem', terminalReleaseActionItemsStart)
assert.ok(
  terminalReleaseActionItemsStart > 0 && terminalReleaseActionItemsEnd > terminalReleaseActionItemsStart,
  '必须能定位第 1 步关闭批次动作列表。'
)
const terminalReleaseActionItemsBlock = detailPage.slice(terminalReleaseActionItemsStart, terminalReleaseActionItemsEnd)
assert.doesNotMatch(
  terminalReleaseActionItemsBlock,
  /quality-reject|label: '质量拒收'/,
  '第 1 步关闭批次动作列表不得包含质量拒收按钮。'
)
assert.match(
  detailPage,
  /if \(stageKey === 'quality-terminal'\) \{\s*return terminalReleaseActionItems\(\)\s*\}/,
  '质量已拒收阶段不得重新展示质量拒收或追溯按钮，只保留终止后的阶段动作。'
)
assert.match(
  detailPage,
  /if \(stageKey === 'close'\) \{\s*return terminalReleaseActionItems\(\)\s*\}/,
  '第 1 步关闭批次阶段只能展示关闭/重开，不得展示质量拒收或追溯。'
)
assert.match(
  detailPage,
  /if \(stageKey === 'precheck'\) \{[\s\S]*label: '放行预检'[\s\S]*qualityRejectActionItem\(\)/,
  '第 2 步放行预检阶段必须展示质量拒收按钮。'
)
const traceFlowButtonMatches = detailPage.match(
  /<el-button[\s\S]*?class="edhr-batch-detail__release-flow-trace-action"[\s\S]*?>[\s\S]*?追溯记录[\s\S]*?<\/el-button>/g
)
assert.equal(traceFlowButtonMatches?.length, 1, '页面必须只保留一个追溯记录按钮。')
const releaseStageActionItemsStart = detailPage.indexOf(
  'const buildReleaseStageActionItems = (stageKey: ReleaseStageKey)'
)
const releaseStageActionItemsEnd = detailPage.indexOf(
  'const releaseStageActionItems = computed',
  releaseStageActionItemsStart
)
assert.ok(
  releaseStageActionItemsStart > 0 && releaseStageActionItemsEnd > releaseStageActionItemsStart,
  '必须能定位右侧阶段动作模型。'
)
assert.doesNotMatch(
  detailPage.slice(releaseStageActionItemsStart, releaseStageActionItemsEnd),
  /traceAction|trace-record|openTraceRecordGroup/,
  '右侧阶段动作模型不得包含追溯记录入口。'
)
assert.doesNotMatch(detailPage, /terminalActionDrawerVisible|openTerminalActionDrawer|canOpenTerminalActionDrawer/, '不得保留终态处理抽屉状态、打开函数或开关条件。')
assert.doesNotMatch(detailPage, /<el-drawer[\s\S]*title="终态处理"[\s\S]*<\/el-drawer>/, '不得继续通过终态处理抽屉承载三个终态动作。')
assert.doesNotMatch(detailPage, /label: '终态处理'/, '右侧操作区不得继续展示“终态处理”按钮。')
assert.doesNotMatch(detailPage, /打开右侧“终态处理”|右侧“终态处理”/, '页面引导文案不得继续提示打开终态处理抽屉。')

for (const requiredState of [
  'quality-terminal',
  'archived',
  'release-approval',
  'archive',
  'precheck',
  'close'
]) {
  assert.match(
    detailPage,
    new RegExp(`key: '${requiredState}'`),
    `放行阶段视图必须覆盖 ${requiredState} 状态。`
  )
}

console.log('PASS edhr batch release state ui static contract')
