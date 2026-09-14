<!-- dhtmlx-gantt Vue 3 封装组件：基于 dhtmlx-gantt 实现甘特图展示和拖拽编辑 -->
<template>
  <div
    ref="ganttContainer"
    class="production-gantt-chart"
    :style="{ width: '100%', height: height + 'px' }"
  ></div>
</template>

<script setup lang="ts">
import { gantt } from 'dhtmlx-gantt'
import 'dhtmlx-gantt/codebase/dhtmlxgantt.css'
import { BarcodeBizTypeEnum } from '@/views/mes/utils/constants'
import {
  READABLE_DAY_COLUMN_WIDTH,
  READABLE_SHIFT_COLUMN_WIDTH,
  READABLE_SHORT_TASK_HOURS,
  escapeGanttHtml,
  getGanttWorkOrderProcessLabel,
  getGanttTaskDurationHours,
  normalizeGanttTasksForReadability
} from './ganttReadability'

/**
 * GanttChart - 甘特图组件
 *
 * 功能：
 * 1. 按工单分组展示生产任务，工单为 project 行，任务为子行
 * 2. 支持只读预览和拖拽编辑两种模式
 * 3. 拖拽后触发 task-update 事件，通知父组件批量保存
 * 4. 时间刻度：只读态按日期间隔展示，编辑态保留周 → 日 → 8 小时
 */

defineOptions({ name: 'GanttChart' })

const props = withDefaults(
  defineProps<{
    tasks: any[] // 甘特图任务数据
    links?: any[] // 甘特图依赖线数据
    readonly?: boolean // 是否只读
    height?: number // 甘特图高度
    dateIntervalDays?: number // 只读甘特图日期间隔：每格 N 天
  }>(),
  {
    tasks: () => [],
    links: () => [],
    readonly: false,
    height: 350,
    dateIntervalDays: 1
  }
)

const emit = defineEmits<{
  'task-update': [task: any]
  'task-click': [id: string | number]
}>()

const ganttContainer = ref<HTMLElement>()
const ganttInited = ref(false)
const SIDE_LABEL_VIEWPORT_PADDING = 380
const COLLAPSED_PROJECT_SUMMARY_MAX_WIDTH = 360
const COLLAPSED_PROJECT_SUMMARY_EDGE_GAP = 8
const SUPPORTED_GANTT_DATE_INTERVAL_DAYS = Array.from({ length: 15 }, (_, index) => index + 1)
const GANTT_ORDER_COLOR_CLASS_PREFIX = 'gantt-order-color-'
const GANTT_ORDER_COLOR_PALETTE = [
  '#2563eb',
  '#0f766e',
  '#9333ea',
  '#c2410c',
  '#be123c',
  '#047857',
  '#7c3aed',
  '#b45309',
  '#0369a1',
  '#a21caf'
]

let pendingGanttTextRefresh = false
let pendingCollapsedProjectOverflowSync = false
let collapsedProjectOverflowLayer: HTMLElement | null = null
let collapseVisibilityEventIds: string[] = []
let requestCollapsedProjectOverflowSync = () => {}

const normalizeGanttDateIntervalDays = (value: number | undefined) => {
  const parsed = Number(value || 1)
  return SUPPORTED_GANTT_DATE_INTERVAL_DAYS.includes(parsed) ? parsed : 1
}

const getGanttScaleMode = () => {
  const scaleMode = props.readonly ? 'day' : 'shift'
  return scaleMode
}

const weekScaleTemplate = (date: Date) => {
  const dateToStr = gantt.date.date_to_str('%M %d')
  const endDate = gantt.date.add(gantt.date.add(date, 1, 'week'), -1, 'day')
  return dateToStr(date) + ' - ' + dateToStr(endDate)
}

const monthScaleTemplate = (date: Date) => {
  return gantt.date.date_to_str('%Y年 %M')(date)
}

const dayTemplate = (date: Date) => {
  return gantt.date.date_to_str('%M %d')(date)
}

const dateIntervalScaleTemplate = (date: Date) => {
  const intervalDays = normalizeGanttDateIntervalDays(props.dateIntervalDays)
  if (intervalDays === 1) {
    return dayTemplate(date)
  }
  const endDate = gantt.date.add(gantt.date.add(date, intervalDays, 'day'), -1, 'day')
  return `${dayTemplate(date)} - ${dayTemplate(endDate)}`
}

const daysStyle = (date: Date) => {
  return date.getDay() === 0 || date.getDay() === 6 ? 'weekend' : ''
}

const applyGanttScaleConfig = () => {
  const scaleMode = getGanttScaleMode()
  gantt.config.min_column_width =
    scaleMode === 'day' ? READABLE_DAY_COLUMN_WIDTH : READABLE_SHIFT_COLUMN_WIDTH
  gantt.config.scales =
    scaleMode === 'day'
      ? [
          {
            unit: 'day',
            step: normalizeGanttDateIntervalDays(props.dateIntervalDays),
            format: dateIntervalScaleTemplate,
            css: daysStyle
          },
          { unit: 'month', step: 1, format: monthScaleTemplate }
        ]
      : [
          { unit: 'week', step: 1, format: weekScaleTemplate },
          { unit: 'day', step: 1, format: dayTemplate, css: daysStyle },
          { unit: 'hour', step: 8, format: '%H:%i' }
        ]
  gantt.config.scale_height = scaleMode === 'day' ? 54 : 58
}

const getGanttOrderCode = (task: any) => {
  const workOrderCode = String(task?.workOrderCode ?? '').trim()
  if (!workOrderCode) {
    throw new Error(`production gantt task missing workOrderCode: ${task?.id || ''}`)
  }
  return workOrderCode
}

const hashGanttOrderCode = (workOrderCode: string) => {
  let hash = 0
  for (let index = 0; index < workOrderCode.length; index += 1) {
    hash = (hash * 31 + workOrderCode.charCodeAt(index)) >>> 0
  }
  return hash
}

const getGanttOrderColorClass = (task: any) => {
  const workOrderCode = getGanttOrderCode(task)
  const index = hashGanttOrderCode(workOrderCode) % GANTT_ORDER_COLOR_PALETTE.length
  return `${GANTT_ORDER_COLOR_CLASS_PREFIX}${index}`
}

const refreshGanttTextAfterTimelineSettles = () => {
  if (pendingGanttTextRefresh) {
    return
  }
  pendingGanttTextRefresh = true
  window.requestAnimationFrame(() => {
    pendingGanttTextRefresh = false
    if (ganttInited.value) {
      gantt.render()
    }
  })
}

/** 初始化甘特图配置 */
const initGantt = () => {
  if (!ganttContainer.value) {
    return
  }

  // 中文国际化
  gantt.i18n.setLocale('cn')

  // 基础配置
  gantt.config.readonly = props.readonly
  gantt.config.date_format = '%Y-%m-%d %H:%i:%s'
  gantt.config.duration_unit = 'hour' // 使用小时作为持续时间单位，配合 duration_step 实现工作日单位
  gantt.config.duration_step = 8 // 1 工作日 = 8 小时
  gantt.config.row_height = 42 // 行高略放大，避免短任务和依赖线堆叠
  gantt.config.bar_height = 26
  gantt.config.fit_tasks = true // 时间范围自动适应
  gantt.config.grid_width = 420
  gantt.config.grid_resize = true
  ;(gantt.config as any).show_unscheduled = true
  gantt.config.auto_scheduling = false // 显式关闭防意外
  gantt.config.drag_links = false // 禁止拖动任务关系
  gantt.config.details_on_create = true // 单击显示添加详情
  gantt.config.details_on_dblclick = true // 双击显示明细
  gantt.config.show_progress = true // 确保进度条显示
  gantt.config.open_tree_initially = true // 初始展开树结构
  gantt.config.auto_types = false // 禁止自动升级为 project
  gantt.config.drag_move = !props.readonly // 编辑态允许直接拖动任务
  gantt.config.drag_resize = !props.readonly // 编辑态允许直接调整任务持续时间
  gantt.config.drag_progress = false // 禁止拖动进度条

  // lightbox 弹窗配置：只保留时间编辑，去掉描述编辑和删除按钮
  gantt.config.lightbox.sections = [{ name: 'time', type: 'duration', map_to: 'auto' }]
  gantt.config.buttons_left = ['gantt_save_btn']
  gantt.config.buttons_right = ['gantt_cancel_btn']

  // 时间刻度：只读页默认日粒度，编辑页保留班次/8小时粒度
  applyGanttScaleConfig()
  gantt.config.show_task_cells = true // 显示任务单元格边框

  // 左侧列配置
  gantt.config.columns = [
    {
      name: 'text',
      label: '生产工单编码 / 工序',
      tree: true,
      width: 420,
      resize: true,
      template: (task: any) => {
        const label = getGanttWorkOrderProcessLabel(task)
        return `<span class="gantt-grid-cell-text" title="${escapeGanttHtml(label)}">${escapeGanttHtml(label)}</span>`
      }
    }
  ]

  // 今天标记线 + tooltip 插件
  gantt.plugins({ marker: true, tooltip: true })
  gantt.addMarker({
    start_date: new Date(),
    css: 'today',
    text: '今天'
  })

  // 甘特条上的文本
  const isShortVisibleTask = (task: any) =>
    !task.unscheduled &&
    task.type !== 'project' &&
    (task.readabilityCompact || getGanttTaskDurationHours(task) <= READABLE_SHORT_TASK_HOURS)
  const getVisibleTimelineBounds = () => {
    const scrollState = gantt.getScrollState() as { x?: number; inner_width?: number }
    const taskElement = (gantt as any).$task as HTMLElement | undefined
    const visibleWidth = Number(taskElement?.clientWidth || scrollState.inner_width || 0)
    if (!visibleWidth) {
      return null
    }
    const visibleLeft = Number(scrollState.x || 0)
    return {
      left: visibleLeft,
      right: visibleLeft + visibleWidth
    }
  }
  const getTaskTimelineBounds = (task: any) => {
    const startPosition = gantt.posFromDate(task.start_date)
    const endPosition = gantt.posFromDate(task.end_date)
    if (!Number.isFinite(startPosition) || !Number.isFinite(endPosition)) {
      throw new Error(`production gantt task missing timeline position: ${task.id || ''}`)
    }
    return {
      left: Math.min(startPosition, endPosition),
      right: Math.max(startPosition, endPosition)
    }
  }
  const isCollapsedProjectSummaryCandidate = (task: any) =>
    !task?.unscheduled && task.type === 'project' && task.$open === false
  const isRenderedProjectBarVisible = (task: any) => {
    const taskData = (gantt as any).$task_data as HTMLElement | undefined
    const getTaskNode = (gantt as any).getTaskNode as ((id: string | number) => HTMLElement | null) | undefined
    const taskNode = typeof getTaskNode === 'function' ? getTaskNode.call(gantt, task.id) : null
    if (!taskData || !taskNode || !taskNode.isConnected) {
      return false
    }
    const style = window.getComputedStyle(taskNode)
    if (style.display === 'none' || style.visibility === 'hidden') {
      return false
    }
    const taskRect = taskNode.getBoundingClientRect()
    const dataRect = taskData.getBoundingClientRect()
    return (
      taskRect.width > 0 &&
      taskRect.height > 0 &&
      taskRect.right > dataRect.left &&
      taskRect.left < dataRect.right &&
      taskRect.bottom > dataRect.top &&
      taskRect.top < dataRect.bottom
    )
  }
  const getCollapsedProjectSummaryPlacement = (task: any) => {
    if (!isCollapsedProjectSummaryCandidate(task)) {
      return null
    }
    if (isRenderedProjectBarVisible(task)) {
      return null
    }
    const visibleBounds = getVisibleTimelineBounds()
    if (!visibleBounds) {
      return null
    }
    const taskBounds = getTaskTimelineBounds(task)
    const safeLeft = visibleBounds.left + COLLAPSED_PROJECT_SUMMARY_EDGE_GAP
    const safeRight = visibleBounds.right - COLLAPSED_PROJECT_SUMMARY_EDGE_GAP
    const availableWidth = safeRight - safeLeft
    if (availableWidth <= 0) {
      return null
    }
    const width = Math.min(COLLAPSED_PROJECT_SUMMARY_MAX_WIDTH, availableWidth)
    const overlapsViewport =
      taskBounds.right >= visibleBounds.left && taskBounds.left <= visibleBounds.right
    const hasStartedBeforeViewport = taskBounds.left < visibleBounds.left
    const endsAfterViewport = taskBounds.right > visibleBounds.right
    const edge = overlapsViewport
      ? hasStartedBeforeViewport
        ? 'left'
        : endsAfterViewport
          ? 'right'
          : 'inside'
      : hasStartedBeforeViewport
        ? 'left'
        : 'right'
    const preferredLeft = overlapsViewport
      ? Math.max(taskBounds.left, safeLeft)
      : hasStartedBeforeViewport
        ? safeLeft
        : safeRight - width
    const left = Math.min(Math.max(preferredLeft, safeLeft), safeRight - width)
    const rowTop = gantt.getTaskTop(task.id)
    const rowHeight = Number(gantt.getTaskHeight(task.id) || gantt.config.row_height || 0)
    const barHeight = Number(gantt.config.bar_height || rowHeight)
    return {
      edge,
      left,
      top: rowTop + Math.max(0, (rowHeight - barHeight) / 2),
      width,
      height: barHeight
    }
  }
  const updateCollapsedProjectSummaryNode = (task: any, node: HTMLElement) => {
    const placement = getCollapsedProjectSummaryPlacement(task)
    if (!placement) {
      node.style.display = 'none'
      return
    }
    const label = getGanttWorkOrderProcessLabel(task)
    node.style.display = ''
    node.style.left = `${placement.left}px`
    node.style.top = `${placement.top}px`
    node.style.width = `${placement.width}px`
    node.style.height = `${placement.height}px`
    node.style.lineHeight = `${placement.height}px`
    node.className = `gantt-collapsed-project-overflow gantt-collapsed-project-overflow--${placement.edge} ${getGanttOrderColorClass(task)}`
    node.title = label
    node.innerHTML = `<span class="gantt-collapsed-project-overflow__text">${escapeGanttHtml(label)}</span>`
  }
  const renderCollapsedProjectSummaryNode = (task: any) => {
    const placement = getCollapsedProjectSummaryPlacement(task)
    if (!placement) {
      return false
    }
    const node = document.createElement('div')
    updateCollapsedProjectSummaryNode(task, node)
    return node
  }
  const ensureCollapsedProjectOverflowLayer = () => {
    const taskData = (gantt as any).$task_data as HTMLElement | undefined
    if (!taskData) {
      return null
    }
    if (collapsedProjectOverflowLayer?.parentElement !== taskData) {
      collapsedProjectOverflowLayer?.remove()
      collapsedProjectOverflowLayer = document.createElement('div')
      collapsedProjectOverflowLayer.className = 'gantt-collapsed-project-overflow-layer'
      taskData.appendChild(collapsedProjectOverflowLayer)
    }
    return collapsedProjectOverflowLayer
  }
  const syncCollapsedProjectOverflowLayer = () => {
    const layer = ensureCollapsedProjectOverflowLayer()
    if (!layer) {
      return
    }
    layer.innerHTML = ''
    gantt.eachTask((task: any) => {
      if (!isCollapsedProjectSummaryCandidate(task) || !gantt.isTaskVisible(task.id)) {
        return
      }
      const node = renderCollapsedProjectSummaryNode(task)
      if (node) {
        layer.appendChild(node)
      }
    })
  }
  requestCollapsedProjectOverflowSync = () => {
    if (pendingCollapsedProjectOverflowSync) {
      return
    }
    pendingCollapsedProjectOverflowSync = true
    window.requestAnimationFrame(() => {
      pendingCollapsedProjectOverflowSync = false
      if (ganttInited.value) {
        syncCollapsedProjectOverflowLayer()
      }
    })
  }
  const shouldRenderSideLabel = (task: any) => {
    const visibleBounds = getVisibleTimelineBounds()
    if (!visibleBounds) {
      return false
    }
    const taskBounds = getTaskTimelineBounds(task)
    return taskBounds.right >= visibleBounds.left && taskBounds.left <= visibleBounds.right
  }
  const shouldPlaceSideLabelOnLeft = (task: any) => {
    const visibleBounds = getVisibleTimelineBounds()
    if (!visibleBounds) {
      return false
    }
    const taskBounds = getTaskTimelineBounds(task)
    const visibleLeft = visibleBounds.left
    const visibleRight = visibleBounds.right
    const taskLeft = taskBounds.left
    const taskRight = taskBounds.right
    const leftSpace = taskLeft - visibleLeft
    const rightSpace = visibleRight - taskRight
    if (leftSpace < SIDE_LABEL_VIEWPORT_PADDING && rightSpace >= leftSpace) {
      return false
    }
    if (rightSpace < SIDE_LABEL_VIEWPORT_PADDING && leftSpace > rightSpace) {
      return true
    }
    return leftSpace > rightSpace
  }
  const getSideLabelPlacement = (task: any) => {
    if (!isShortVisibleTask(task) || !shouldRenderSideLabel(task)) {
      return ''
    }
    return shouldPlaceSideLabelOnLeft(task) ? 'left' : 'right'
  }
  const buildSideLabel = (task: any) => {
    const label = getGanttWorkOrderProcessLabel(task)
    return `<span class="gantt-task-side-label" title="${escapeGanttHtml(label)}">${escapeGanttHtml(label)}</span>`
  }
  gantt.templates.task_text = (_start: any, _end: any, task: any) => {
    if (task.unscheduled) {
      return ''
    }
    const label = getGanttWorkOrderProcessLabel(task)
    if (task.type === 'project') {
      return `<span class="gantt-task-label gantt-task-label--project"><span class="gantt-task-label__text">${escapeGanttHtml(label)}</span></span>`
    }
    if (
      task.readabilityCompact ||
      getGanttTaskDurationHours(task) <= READABLE_SHORT_TASK_HOURS
    ) {
      return ''
    }
    return `<span class="gantt-task-label"><span class="gantt-task-label__text">${escapeGanttHtml(label)}</span></span>`
  }
  gantt.templates.leftside_text = (_start: any, _end: any, task: any) => {
    return getSideLabelPlacement(task) === 'left' ? buildSideLabel(task) : ''
  }
  gantt.templates.rightside_text = (_start: any, _end: any, task: any) => {
    return getSideLabelPlacement(task) === 'right' ? buildSideLabel(task) : ''
  }

  // 鼠标悬浮提示
  gantt.templates.tooltip_text = (_start: any, _end: any, task: any) => {
    const label = getGanttWorkOrderProcessLabel(task)
    if (task.unscheduled) {
      return `<div class="gantt-readable-tooltip">
        <div class="gantt-readable-tooltip__title">${escapeGanttHtml(label)}</div>
        <div class="gantt-readable-tooltip__row"><span>状态</span><strong>未排产</strong></div>
        <div class="gantt-readable-tooltip__row"><span>原因</span><strong>${escapeGanttHtml(task.readabilityMissingScheduleReason || '-')}</strong></div>
      </div>`
    }
    const processRow =
      task.type === 'project'
        ? ''
        : `<div class="gantt-readable-tooltip__row"><span>工序</span><strong>${escapeGanttHtml(task.process)}</strong></div>`
    return `<div class="gantt-readable-tooltip">
      <div class="gantt-readable-tooltip__title">${escapeGanttHtml(label)}</div>
      <div class="gantt-readable-tooltip__row"><span>工单编码</span><strong>${escapeGanttHtml(task.workOrderCode)}</strong></div>
      ${processRow}
    </div>`
  }

  // 任务颜色模板
  gantt.templates.task_class = (_start: any, _end: any, task: any) => {
    if (task.unscheduled) {
      return 'gantt-unscheduled-task'
    }
    if (task.type === gantt.config.types.project) {
      return `gantt-project-bar ${getGanttOrderColorClass(task)}`
    }
    const classes: string[] = []
    classes.push(getGanttOrderColorClass(task))
    if (task.scheduleSource === 'AUTO') {
      classes.push('gantt-auto-task')
    }
    if (task.scheduleSource === 'MANUAL') {
      classes.push('gantt-manual-task')
    }
    if (task.locked) {
      classes.push('gantt-locked-task')
    }
    if (task.riskStatus === 'BLOCKED') {
      classes.push('gantt-risk-task')
    }
    if (
      task.readabilityCompact ||
      getGanttTaskDurationHours(task) <= READABLE_SHORT_TASK_HOURS
    ) {
      classes.push('gantt-short-task')
    }
    return classes.join(' ')
  }
  gantt.templates.timeline_cell_class = () => '' // 防止 gantt 添加默认样式类
  gantt.templates.task_row_class = (_start: any, _end: any, task: any) =>
    task?.unscheduled ? 'gantt-unscheduled-row' : ''
  gantt.templates.grid_row_class = (_start: any, _end: any, task: any) =>
    task?.unscheduled ? 'gantt-unscheduled-row' : ''

  collapseVisibilityEventIds = [
    gantt.attachEvent('onTaskClosed', () => {
      refreshGanttTextAfterTimelineSettles()
      requestCollapsedProjectOverflowSync()
      return true
    }),
    gantt.attachEvent('onTaskOpened', () => {
      refreshGanttTextAfterTimelineSettles()
      requestCollapsedProjectOverflowSync()
      return true
    }),
    gantt.attachEvent('onGanttScroll', () => {
      requestCollapsedProjectOverflowSync()
      return true
    }),
    gantt.attachEvent('onGanttRender', () => {
      requestCollapsedProjectOverflowSync()
      return true
    })
  ]

  // 编辑事件监听（通过 lightbox 弹窗编辑后触发）
  if (!props.readonly) {
    gantt.attachEvent('onAfterTaskUpdate', (id: string | number) => {
      const task = gantt.getTask(id)
      // 生产排产只允许回写 task 节点，避免把工单(project)节点当成任务保存
      if (task.type !== gantt.config.types.task || !task.originalId) {
        return
      }
      // 触发 task-update 事件，通知父组件保存修改
      emit('task-update', {
        id: task.originalId,
        startTime: task.start_date,
        endTime: task.end_date,
        duration: task.duration
      })
    })
  }

  // 点击任务事件
  gantt.attachEvent('onTaskClick', (id: string | number) => {
    emit('task-click', id)
    return true
  })

  // 初始化
  gantt.init(ganttContainer.value)
  ganttInited.value = true
}

/** 加载数据到甘特图 */
const loadData = (tasks: any[], links: any[]) => {
  if (!ganttInited.value) {
    return
  }
  gantt.clearAll()
  // 后端 type 使用 MesBizTypeConstants 整数，需映射为 gantt 类型字符串
  const TYPE_MAP: Record<number, string> = {
    [BarcodeBizTypeEnum.WORKORDER]: 'project',
    [BarcodeBizTypeEnum.TASK]: 'task'
  }
  const parentIds = new Set(
    (tasks || []).map((item: any) => item.parent).filter((item: any) => !!item)
  )
  const filteredTasks = (tasks || []).filter((item: any) => {
    const mappedType = TYPE_MAP[item.type] || item.type
    if (mappedType !== 'project') {
      return true
    }
    return parentIds.has(item.id)
  })
  const mappedTasks = filteredTasks.map((item: any) => ({
    ...item,
    type: TYPE_MAP[item.type] || item.type
  }))
  const transformed = {
    data: normalizeGanttTasksForReadability(mappedTasks),
    links: links || []
  }
  gantt.parse(transformed)
  refreshGanttTextAfterTimelineSettles()
}

const refreshGanttScaleAfterDateIntervalChange = () => {
  if (!ganttInited.value) {
    return
  }
  const scrollState = gantt.getScrollState() as { x?: number }
  const visibleDate =
    Number.isFinite(Number(scrollState.x)) && typeof gantt.dateFromPos === 'function'
      ? gantt.dateFromPos(Number(scrollState.x))
      : null
  applyGanttScaleConfig()
  gantt.render()
  if (visibleDate instanceof Date && !Number.isNaN(visibleDate.getTime())) {
    window.requestAnimationFrame(() => {
      if (ganttInited.value) {
        gantt.showDate(visibleDate)
        requestCollapsedProjectOverflowSync()
      }
    })
  } else {
    requestCollapsedProjectOverflowSync()
  }
}

const collapseAllProjects = () => {
  if (!ganttInited.value) {
    return
  }
  gantt.batchUpdate(() => {
    gantt.eachTask((task: any) => {
      if (!task?.unscheduled && task.type === gantt.config.types.project && task.$open !== false) {
        gantt.close(task.id)
      }
    })
  })
  refreshGanttTextAfterTimelineSettles()
  requestCollapsedProjectOverflowSync()
}

const expandAllProjects = () => {
  if (!ganttInited.value) {
    return
  }
  gantt.batchUpdate(() => {
    gantt.eachTask((task: any) => {
      if (!task?.unscheduled && task.type === gantt.config.types.project && task.$open === false) {
        gantt.open(task.id)
      }
    })
  })
  refreshGanttTextAfterTimelineSettles()
  requestCollapsedProjectOverflowSync()
}

/** 监听 tasks 变化 */
watch(
  () => props.tasks,
  (val) => {
    if (ganttInited.value) {
      loadData(val || [], props.links || [])
    }
  },
  { deep: true }
)

watch(
  () => props.links,
  (val) => {
    if (ganttInited.value) {
      loadData(props.tasks || [], val || [])
    }
  },
  { deep: true }
)

watch(
  () => props.dateIntervalDays,
  () => {
    refreshGanttScaleAfterDateIntervalChange()
  }
)

watch(
  () => props.height,
  async () => {
    if (!ganttInited.value) {
      return
    }
    await nextTick()
    gantt.setSizes()
    gantt.render()
    refreshGanttTextAfterTimelineSettles()
    requestCollapsedProjectOverflowSync()
  }
)

/** 组件挂载后初始化甘特图 */
onMounted(() => {
  initGantt()
  if (props.tasks?.length) {
    loadData(props.tasks, props.links || [])
  }
})

/** 组件卸载前清理甘特图 */
onBeforeUnmount(() => {
  if (ganttInited.value) {
    collapseVisibilityEventIds.forEach((eventId) => {
      gantt.detachEvent(eventId)
    })
    collapseVisibilityEventIds = []
    collapsedProjectOverflowLayer?.remove()
    collapsedProjectOverflowLayer = null
    gantt.clearAll()
  }
})

defineExpose({ loadData, collapseAllProjects, expandAllProjects })
</script>

<style>
.production-gantt-chart {
  border-top: 1px solid #dbe3ef;
  color: #172033;
}

.production-gantt-chart .gantt_container {
  border: 0;
  font-size: 13px;
}

.production-gantt-chart .gantt_grid_scale,
.production-gantt-chart .gantt_task_scale {
  background: #f7f9fc;
  border-color: #dbe3ef;
}

.production-gantt-chart .gantt_grid_head_cell,
.production-gantt-chart .gantt_scale_cell {
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
}

.production-gantt-chart .gantt_grid_data .gantt_cell {
  color: #263247;
  font-size: 13px;
}

.production-gantt-chart .gantt-grid-cell-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.production-gantt-chart .gantt-grid-status-tag {
  display: inline-flex;
  height: 20px;
  align-items: center;
  margin-left: 6px;
  padding: 0 6px;
  border: 1px solid #dbe3ef;
  border-radius: 5px;
  background: #f7f9fc;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  vertical-align: middle;
}

.production-gantt-chart .gantt_task_cell {
  border-color: #edf1f6;
}

.production-gantt-chart .gantt_task_row,
.production-gantt-chart .gantt_row {
  border-color: #e5ebf3;
}

/* 今天标记线 */
.gantt_marker.today {
  background-color: #ef4444;
  opacity: 0.32;
}
.gantt_marker.today .gantt_marker_content {
  color: #b91c1c;
  font-size: 12px;
  font-weight: 600;
}
/* 甘特条圆角 */
.gantt_task_line {
  min-width: 40px;
  border-radius: 6px;
  box-shadow: none;
}
.gantt_task_line.gantt-order-color-0,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-0 {
  background: #2563eb !important;
  border-color: #1d4ed8 !important;
}
.gantt_task_line.gantt-order-color-1,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-1 {
  background: #0f766e !important;
  border-color: #115e59 !important;
}
.gantt_task_line.gantt-order-color-2,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-2 {
  background: #9333ea !important;
  border-color: #7e22ce !important;
}
.gantt_task_line.gantt-order-color-3,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-3 {
  background: #c2410c !important;
  border-color: #9a3412 !important;
}
.gantt_task_line.gantt-order-color-4,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-4 {
  background: #be123c !important;
  border-color: #9f1239 !important;
}
.gantt_task_line.gantt-order-color-5,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-5 {
  background: #047857 !important;
  border-color: #065f46 !important;
}
.gantt_task_line.gantt-order-color-6,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-6 {
  background: #7c3aed !important;
  border-color: #6d28d9 !important;
}
.gantt_task_line.gantt-order-color-7,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-7 {
  background: #b45309 !important;
  border-color: #92400e !important;
}
.gantt_task_line.gantt-order-color-8,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-8 {
  background: #0369a1 !important;
  border-color: #075985 !important;
}
.gantt_task_line.gantt-order-color-9,
.production-gantt-chart .gantt-collapsed-project-overflow.gantt-order-color-9 {
  background: #a21caf !important;
  border-color: #86198f !important;
}
.gantt_task_line.gantt-project-bar {
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 18%);
}
.gantt_task_line .gantt_task_progress {
  background: rgb(15 23 42 / 30%) !important;
}
.gantt_task_line.gantt-auto-task {
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 24%);
}
.gantt_task_line.gantt-manual-task {
  box-shadow: inset 0 0 0 1px rgb(15 23 42 / 16%);
}
.gantt_task_line.gantt-locked-task {
  border-style: dashed;
}
.gantt_task_line.gantt-risk-task {
  box-shadow:
    inset 0 0 0 1px rgb(255 255 255 / 20%),
    0 0 0 2px rgb(239 68 68 / 42%);
}
.gantt-unscheduled-row {
  background: #fafcff !important;
  color: #64748b;
}
.gantt-unscheduled-row .gantt_cell {
  color: #64748b !important;
}
.gantt_task_line .gantt_task_content {
  padding: 0 8px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  line-height: 26px;
  text-shadow: none;
}
.gantt-short-task.gantt_task_line {
  min-width: 44px;
}
.gantt-short-task .gantt_task_content {
  padding: 0 6px;
}
.gantt-task-label {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.gantt-task-label__text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gantt-task-label--compact {
  justify-content: center;
  gap: 4px;
}
.gantt-task-label--project .gantt-task-label__text {
  font-weight: 700;
}
.production-gantt-chart .gantt-collapsed-project-overflow-layer {
  position: absolute;
  z-index: 6;
  inset: 0 auto auto 0;
  width: 0;
  height: 0;
  pointer-events: none;
}
.production-gantt-chart .gantt-collapsed-project-overflow {
  position: absolute;
  z-index: 6;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 0 10px;
  border: 1px solid #475569;
  border-radius: 6px;
  background: #64748b;
  box-shadow: 0 1px 2px rgb(15 23 42 / 14%);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  pointer-events: none;
  white-space: nowrap;
}
.production-gantt-chart .gantt-collapsed-project-overflow::before {
  flex: none;
  margin-right: 6px;
  color: #dbeafe;
  font-size: 12px;
}
.production-gantt-chart .gantt-collapsed-project-overflow--left::before {
  content: '←';
}
.production-gantt-chart .gantt-collapsed-project-overflow--right::before {
  content: '→';
}
.production-gantt-chart .gantt-collapsed-project-overflow__text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}
.production-gantt-chart .gantt-task-side-label {
  display: inline-block;
  max-width: 360px;
  overflow: hidden;
  color: #263247;
  font-size: 12px;
  font-weight: 600;
  line-height: 26px;
  text-overflow: ellipsis;
  vertical-align: top;
  white-space: nowrap;
}
.production-gantt-chart .gantt_side_content.gantt_left .gantt-task-side-label {
  padding-right: 6px;
}
.production-gantt-chart .gantt_side_content.gantt_right .gantt-task-side-label {
  padding-left: 6px;
}
/* 周末背景色 */
.weekend {
  background: #f7f9fc !important;
}
/* 行悬浮高亮 */
.gantt_grid_data .gantt_row:hover,
.gantt_grid_data .gantt_row.odd:hover {
  background-color: #fafcff !important;
}
/* 选中行高亮 */
.gantt_grid_data .gantt_row.gantt_selected,
.gantt_grid_data .gantt_row.odd.gantt_selected,
.gantt_task_row.gantt_selected {
  background-color: #eef6ff !important;
}

.gantt_task_link {
  opacity: 0.34;
  transition: opacity 0.16s ease;
}
.gantt_task_link:hover {
  opacity: 0.78;
}
.gantt_task_link .gantt_line_wrapper div {
  background-color: #94a3b8 !important;
}
.gantt_link_arrow_right {
  border-left-color: #94a3b8 !important;
}
.gantt_link_arrow_left {
  border-right-color: #94a3b8 !important;
}
.gantt_tooltip {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  box-shadow: 0 10px 24px rgb(15 23 42 / 12%);
}
.gantt-readable-tooltip {
  min-width: 240px;
  color: #fff;
  font-size: 12px;
}
.gantt-readable-tooltip__title {
  margin-bottom: 6px;
  color: #fff;
  font-weight: 700;
}
.gantt-readable-tooltip__row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  line-height: 22px;
}
.gantt-readable-tooltip__row span {
  flex: none;
  color: #fff;
}
.gantt-readable-tooltip__row strong {
  max-width: 180px;
  overflow: hidden;
  color: #fff;
  font-weight: 600;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
