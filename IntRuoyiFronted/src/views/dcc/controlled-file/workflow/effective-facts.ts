import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'

export type EffectiveFacts = Pick<ControlledFileVO, 'effectiveMode' | 'effectiveDate' | 'effectiveTime' | 'activatedTime'> & {
  delayedEffectiveTime?: string | number | null; delayedEffectiveConfirmedAt?: string | number | null
}

export const formatDelayedEffectiveFact = (value: string | number | null | undefined): string | undefined => {
  if(value == null)return undefined
  if(typeof value==='number') {
    if(!Number.isSafeInteger(value) || !Number.isFinite(new Date(value).getTime()))throw new Error('延迟生效时间戳无效')
    const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'}).formatToParts(new Date(value))
    const field=(type: string)=>parts.find(part=>part.type===type)!.value
    return `${field('year')}-${field('month')}-${field('day')} ${field('hour')}:${field('minute')}:${field('second')}`
  }
  const match=/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?$/.exec(value)
  if(!match)throw new Error('延迟生效时间格式无效')
  const [year,month,day,hour,minute,second]=match.slice(1,7).map(Number),check=new Date(0)
  check.setUTCFullYear(year,month-1,day);check.setUTCHours(hour,minute,second,0)
  if(year<1 || check.getUTCFullYear()!==year || check.getUTCMonth()!==month-1 || check.getUTCDate()!==day || check.getUTCHours()!==hour || check.getUTCMinutes()!==minute || check.getUTCSeconds()!==second)throw new Error('延迟生效时间不是有效日历时间')
  return value
}

/** Displays saved facts only; unknown historical mode is independent of its original calendar date. */
export const effectiveFacts = (facts: EffectiveFacts) => {
  const mode = facts.effectiveMode
  if (mode != null && mode !== 'AFTER_STANDARDIZATION_SIGNATURE' && mode !== 'PRESET_DATE' && mode !== 'DELAYED') {
    throw new Error('生效方式未识别，请核对正式版本记录')
  }
  return {
    modeText: mode === 'AFTER_STANDARDIZATION_SIGNATURE' ? '文控标准化签名后生效'
      : mode === 'DELAYED' ? '延迟生效' : mode === 'PRESET_DATE' ? '按预设日期生效' : '未记录',
    presetDateVisible: mode !== 'AFTER_STANDARDIZATION_SIGNATURE' && mode !== 'DELAYED',
    delayedEffectiveText: formatDelayedEffectiveFact(facts.delayedEffectiveTime) ?? '尚未确认',
    delayedConfirmedText: formatDelayedEffectiveFact(facts.delayedEffectiveConfirmedAt) ?? '尚未签名确认',
    effectiveText: facts.effectiveTime || (mode == null ? '未记录' : '尚未生效'),
    activatedText: facts.activatedTime || '尚未日常启用'
  }
}
