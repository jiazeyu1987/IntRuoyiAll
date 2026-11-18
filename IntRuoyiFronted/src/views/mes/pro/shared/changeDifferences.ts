const labels: Record<string, string> = {
  code: '编码', name: '名称', attention: '工序说明', sopFileUrl: 'SOP文件',
  productName: '产品名称', status: '状态', remark: '备注', brand: '品牌',
  specification: '规格型号', workshopId: '车间编号', machineryTypeId: '设备类型编号',
  processName: '工序名称', manualShiftCapacity: '人工班产能', standardHourlyCapacity: '小时产能',
  parametersJson: '工艺参数', processContents: '操作步骤', lastCheckTime: '最近点检时间', lastMaintenTime: '最近保养时间'
}
const display = (value: unknown) => value === null || value === undefined || value === '' ? '—' : String(value)
function parameters(value: unknown) {
  if (value === null || value === undefined || value === '') return '—'
  const fields: Record<string, string> = { parameterName: '参数', standardText: '标准', unit: '单位',
    valueType: '类型', lowerLimit: '下限', upperLimit: '上限', decimalScale: '精度',
    defaultValue: '默认值', defaultText: '默认文本', optionValuesJson: '选项' }
  return (JSON.parse(String(value)) as Array<Record<string, unknown>>).map(parameter =>
    Object.entries(fields).filter(([key]) => display(parameter[key]) !== '—')
      .map(([key, label]) => `${label}：${display(parameter[key])}`).join('，')).join('；') || '无工艺参数'
}
export function changeDifferences(before: Record<string, unknown>, after: Record<string, unknown>) {
  const format = (key: string, value: unknown) => {
    if (key === 'parametersJson') return parameters(value)
    if (key === 'processContents') {
      const fields: Record<string, string> = { content: '说明', device: '设备', material: '材料', docUrl: '文档', remark: '备注' }
      return ((value ?? []) as Array<Record<string, unknown>>).map(row =>
        `${row.sort}：${Object.entries(fields).filter(([field]) => display(row[field]) !== '—')
          .map(([field, label]) => `${label}：${display(row[field])}`).join('，')}`).join('；') || '无操作步骤'
    }
    return display(value)
  }
  return Object.entries(labels).map(([key, label]) => ({ label, before: format(key, before[key]), after: format(key, after[key]) }))
    .filter(row => row.before !== row.after)
}
