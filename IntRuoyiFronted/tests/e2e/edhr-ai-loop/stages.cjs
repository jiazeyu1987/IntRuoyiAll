const STAGES = Object.freeze([
  Object.freeze({ id: 'S01', name: '无领料单加入活跃订单' }),
  Object.freeze({ id: 'S02', name: '一线生产及生产复核' }),
  Object.freeze({ id: 'S03', name: '一线PQC及PQC复核' }),
  Object.freeze({ id: 'S04', name: '领料晚到及无补料确认完成' }),
  Object.freeze({ id: 'S05', name: 'PQC生产放行及正式表单衔接' }),
  Object.freeze({ id: 'S06', name: '四份资料齐套' }),
  Object.freeze({ id: 'S07', name: '负责人最终放行' }),
  Object.freeze({ id: 'S08', name: '归档及历史追溯' })
])
function stageResults(failedStage, currentStatus = 'FAIL') {
  if (failedStage === null) {
    return STAGES.map((stage) => ({ stage: stage.id, name: stage.name, status: currentStatus }))
  }
  const failedIndex = STAGES.findIndex((stage) => stage.id === failedStage)
  if (failedIndex < 0) {
    return STAGES.map((stage) => ({ stage: stage.id, name: stage.name, status: 'BLOCKED' }))
  }
  return STAGES.map((stage, index) => ({ stage: stage.id, name: stage.name,
    status: index < failedIndex ? 'PASS' : index === failedIndex ? currentStatus : 'BLOCKED' }))
}
module.exports = { STAGES, stageResults }
