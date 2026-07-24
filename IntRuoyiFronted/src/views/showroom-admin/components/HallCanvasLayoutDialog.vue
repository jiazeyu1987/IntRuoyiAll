<template>
  <el-dialog
    v-model="dialogVisible"
    class="showroom-hall-canvas-dialog"
    destroy-on-close
    title="画布布局"
    width="1040px"
  >
    <div v-if="hallRecord" class="showroom-hall-canvas-dialog__body">
      <el-alert
        v-if="loadError"
        :closable="false"
        show-icon
        type="error"
        :title="loadError"
      />

      <div class="showroom-hall-canvas-dialog__toolbar">
        <div class="showroom-hall-canvas-dialog__title">
          <strong>{{ hallRecord.name }}</strong>
          <span>{{ hallRecord.hallCode }}</span>
          <span>{{ blocks.length }} 个展项块</span>
        </div>
        <div class="showroom-hall-canvas-dialog__actions">
          <div class="showroom-hall-canvas-dialog__background-tools">
            <el-upload
              :action="uploadUrl"
              accept="image/jpeg,image/png,image/gif"
              :before-upload="beforeUploadCanvasBackground"
              :disabled="backgroundUploading || backgroundSaving || loadingOptions || Boolean(loadError)"
              :http-request="httpRequest"
              :on-error="handleCanvasBackgroundUploadError"
              :on-success="handleCanvasBackgroundUploadSuccess"
              :show-file-list="false"
            >
              <el-button
                :loading="backgroundUploading || backgroundSaving"
                :disabled="loadingOptions || Boolean(loadError)"
                type="primary"
                plain
              >
                <Icon icon="ep:upload" class="mr-5px" />
                上传背景图
              </el-button>
            </el-upload>
            <el-button
              :disabled="!canvasBackgroundImageUrl || backgroundUploading || backgroundSaving || loadingOptions || Boolean(loadError)"
              :loading="backgroundSaving"
              @click="handleClearCanvasBackground"
            >
              <Icon icon="ep:close" class="mr-5px" />
              清除背景图
            </el-button>
            <el-tag effect="plain" size="small" type="info">仅辅助布局，不发布</el-tag>
          </div>
          <el-segmented
            v-model="previewMode"
            :options="previewModeOptions"
            class="showroom-hall-canvas-dialog__mode-toggle"
          />
          <el-select
            v-model="selectedProductId"
            class="showroom-hall-canvas-dialog__select"
            :disabled="loadingOptions || Boolean(loadError)"
            filterable
            placeholder="选择展项"
          >
            <el-option
              v-for="option in availableProductOptions"
              :key="option.itemKey"
              :label="`${option.itemType === 'AWARD' ? '奖项' : '产品'} · ${option.productCode} · ${option.nameCn}`"
              :value="option.itemKey"
            />
          </el-select>
          <el-button
            :disabled="!selectedProductId || loadingOptions || Boolean(loadError)"
            type="primary"
            @click="handleAddProductBlock"
          >
            <Icon icon="ep:plus" class="mr-5px" />
            新增展项块
          </el-button>
          <el-button
            :disabled="loadingOptions || Boolean(loadError) || blocks.length === 0"
            :loading="buLayoutCalculating"
            @click="handleCalculateBuCanvasLayout"
          >
            <Icon icon="ep:grid" class="mr-5px" />
            按 BU 排布
          </el-button>
          <el-button
            :disabled="!selectedBlockId || blocks.length <= 1"
            type="danger"
            @click="handleDeleteSelectedBlock"
          >
            <Icon icon="ep:delete" class="mr-5px" />
            删除展项块
          </el-button>
        </div>
      </div>

      <div v-loading="loadingOptions" class="showroom-hall-canvas-dialog__canvas-shell">
        <div
          ref="canvasRef"
          class="showroom-hall-canvas-dialog__canvas"
          @dragover.prevent
        >
          <img
            v-if="previewMode === 'editor' && canvasBackgroundImageUrl"
            class="showroom-hall-canvas-dialog__background"
            :src="canvasBackgroundImageUrl"
            alt=""
            draggable="false"
          />
          <button
            v-for="block in blocks"
            :key="block.blockId"
            class="showroom-hall-canvas-dialog__block"
            :class="[
              `is-${previewMode}`,
              { 'is-selected': selectedBlockId === block.blockId, 'is-missing-cover': previewMode === 'website' && !block.previewImageUrl }
            ]"
            draggable="true"
            type="button"
            :style="blockStyle(block)"
            @click="selectedBlockId = block.blockId"
            @dragstart="handleBlockDragStart(block.blockId)"
            @drop.prevent="handleBlockDrop(block.blockId)"
          >
            <template v-if="previewMode === 'website'">
              <div class="showroom-hall-canvas-dialog__block-glow"></div>
              <div class="showroom-hall-canvas-dialog__block-art">
                <img
                  v-if="block.previewImageUrl"
                  class="showroom-hall-canvas-dialog__block-image"
                  :src="block.previewImageUrl"
                  :alt="block.productName"
                  draggable="false"
                />
                <div v-else class="showroom-hall-canvas-dialog__block-placeholder">
                  <strong>缺封面</strong>
                  <span>{{ block.productCode }}</span>
                </div>
              </div>
              <span class="showroom-hall-canvas-dialog__block-label" data-preview-mode="website">
                {{ block.productName }}
              </span>
            </template>
            <template v-else>
              <span>{{ block.productName }}</span>
              <small>{{ block.productCode }}</small>
            </template>
          </button>

          <div
            v-for="boundary in canvasBoundaries"
            :key="boundary.key"
            class="showroom-hall-canvas-dialog__resize-handle"
            :class="`is-${boundary.orientation}`"
            :style="boundaryStyle(boundary)"
            @pointerdown.prevent="handleResizeStart(boundary, $event)"
          ></div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button
        type="primary"
        :disabled="loadingOptions || Boolean(loadError) || blocks.length === 0"
        :loading="saving"
        @click="handleSaveCanvasLayout"
      >
        保存布局
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { useUpload } from '@/components/UploadFile/src/useUpload'
import {
  normalizeHallProductCandidateOptions,
  normalizeHallRecord,
  normalizeProductOptions,
  type HallProductOption
} from '@/views/showroom-admin/hall/contracts'
import {
  assertCanvasIntegrity,
  buildCanvasLayoutPayload,
  clampCanvasBoundaryDelta,
  createCanvasBlock,
  deleteCanvasBlock,
  resizeCanvasBoundary,
  splitCanvasBlock,
  swapCanvasBlockProducts,
  type CanvasBlock,
  type CanvasBoundary
} from '@/views/showroom-admin/hall/canvasLayout'
import type { UploadProps } from 'element-plus'

defineOptions({ name: 'HallCanvasLayoutDialog' })

const CANVAS_BACKGROUND_UPLOAD_DIRECTORY = 'showroom/hall/canvas-background'
const CANVAS_BACKGROUND_ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif'] as const
const CANVAS_BACKGROUND_MAX_SIZE_MB = 5

interface CanvasBoundaryView extends CanvasBoundary {
  key: string
}

const props = defineProps<{
  modelValue: boolean
  hall?: unknown | null
  products?: unknown[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [payload: { hallId: number }]
}>()

const message = useMessage()
const canvasRef = ref<HTMLElement | null>(null)
const loadingOptions = ref(false)
const saving = ref(false)
const buLayoutCalculating = ref(false)
const backgroundUploading = ref(false)
const backgroundSaving = ref(false)
const loadError = ref('')
const previewMode = ref<'editor' | 'website'>('editor')
const productOptions = ref<HallProductOption[]>([])
const blocks = ref<CanvasBlock[]>([])
const canvasBackgroundImageUrl = ref('')
const selectedProductId = ref<string | null>(null)
const selectedBlockId = ref('')
const draggingBlockId = ref('')
const activeResize = ref<{
  boundary: CanvasBoundaryView
  baseBlocks: CanvasBlock[]
  startClientX: number
  startClientY: number
} | null>(null)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const { uploadUrl, httpRequest } = useUpload(CANVAS_BACKGROUND_UPLOAD_DIRECTORY)
const previewModeOptions = [
  { label: '布局编辑', value: 'editor' },
  { label: 'Website 预览', value: 'website' }
] as const

const hallRecord = computed(() => (props.hall ? normalizeHallRecord(props.hall) : null))
const productOptionMap = computed(() => {
  return new Map<string, HallProductOption>(
    productOptions.value.map((option) => [option.itemKey, option])
  )
})
const usedProductIds = computed(() => new Set(blocks.value.map((block) => block.itemKey)))
const availableProductOptions = computed(() => {
  return productOptions.value.filter((option) => !usedProductIds.value.has(option.itemKey))
})

const canvasBoundaries = computed<CanvasBoundaryView[]>(() => {
  const boundaries = new Map<string, CanvasBoundaryView>()
  blocks.value.forEach((left) => {
    blocks.value.forEach((right) => {
      if (left.blockId === right.blockId) {
        return
      }
      const leftRight = left.x + left.width
      if (sameEdge(leftRight, right.x)) {
        const y1 = Math.max(left.y, right.y)
        const y2 = Math.min(left.y + left.height, right.y + right.height)
        if (y2 - y1 > 0.000001) {
          const key = `v-${leftRight}-${y1}-${y2}`
          boundaries.set(key, { key, orientation: 'vertical', x: leftRight, y1, y2, delta: 0 })
        }
      }
      const leftBottom = left.y + left.height
      if (sameEdge(leftBottom, right.y)) {
        const x1 = Math.max(left.x, right.x)
        const x2 = Math.min(left.x + left.width, right.x + right.width)
        if (x2 - x1 > 0.000001) {
          const key = `h-${leftBottom}-${x1}-${x2}`
          boundaries.set(key, { key, orientation: 'horizontal', y: leftBottom, x1, x2, delta: 0 })
        }
      }
    })
  })
  return Array.from(boundaries.values())
})

const mergeProductOptions = (target: Map<string, HallProductOption>, source: HallProductOption[]) => {
  source.forEach((option) => target.set(option.itemKey, option))
}

const loadAllProductOptions = async () => {
  loadingOptions.value = true
  loadError.value = ''
  try {
    const merged = new Map<string, HallProductOption>()
    mergeProductOptions(merged, normalizeProductOptions(props.products || []))
    mergeProductOptions(
      merged,
      normalizeHallProductCandidateOptions(await ShowroomAdminApi.getHallItemOptions())
    )
    productOptions.value = Array.from(merged.values())
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    productOptions.value = []
    loadError.value = resolved.message
    message.error(resolved.message)
  } finally {
    loadingOptions.value = false
  }
}

const resetBlocksFromHall = () => {
  if (!hallRecord.value) {
    blocks.value = []
    return
  }
  blocks.value = createBlocksFromMappings(hallRecord.value.productMappings)
  selectedBlockId.value = blocks.value[0]?.blockId || ''
}

const createBlocksFromMappings = (mappings: Array<{
  itemType: 'PRODUCT' | 'AWARD'
  itemId: number
  displayOrder: number
  layoutX: number | null
  layoutY: number | null
  layoutWidth: number | null
  layoutHeight: number | null
}>) => {
  const nextBlocks = [...mappings]
    .sort((left, right) => left.displayOrder - right.displayOrder)
    .map((mapping, index) => {
      const product = productOptionMap.value.get(`${mapping.itemType}:${mapping.itemId}`)
      if (!product) {
        throw new Error(`展柜画布展项不存在于候选列表：${mapping.itemType}:${mapping.itemId}`)
      }
      if (
        mapping.layoutX === null ||
        mapping.layoutY === null ||
        mapping.layoutWidth === null ||
        mapping.layoutHeight === null
      ) {
        throw new Error(`展柜画布展项缺少布局：${mapping.itemType}:${mapping.itemId}`)
      }
      return createCanvasBlock(product, index, {
        x: mapping.layoutX,
        y: mapping.layoutY,
        width: mapping.layoutWidth,
        height: mapping.layoutHeight
      })
    })
  assertCanvasBlocksInBounds(nextBlocks)
  return nextBlocks
}

const assertCanvasBlocksInBounds = (nextBlocks: CanvasBlock[]) => {
  nextBlocks.forEach((block, index) => {
    const values = [block.x, block.y, block.width, block.height]
    if (values.some((value) => typeof value !== 'number' || !Number.isFinite(value))) {
      throw new Error(`第 ${index + 1} 个展项块坐标必须是有效数字`)
    }
    if (block.x < 0 || block.y < 0 || block.width <= 0 || block.height <= 0) {
      throw new Error(`第 ${index + 1} 个展项块坐标必须在画布内`)
    }
    if (block.x + block.width > 1.000001 || block.y + block.height > 1.000001) {
      throw new Error(`第 ${index + 1} 个展项块超出画布范围`)
    }
  })
}

watch(
  () => [props.modelValue, props.hall] as const,
  async ([visible]) => {
    if (!visible || !hallRecord.value) {
      return
    }
    try {
      await loadAllProductOptions()
      if (!loadError.value) {
        previewMode.value = 'editor'
        canvasBackgroundImageUrl.value = hallRecord.value.canvasBackgroundImageUrl
        resetBlocksFromHall()
      }
    } catch (error) {
      const resolved = error instanceof Error ? error : new Error(String(error))
      loadError.value = resolved.message
      message.error(resolved.message)
    }
  },
  { immediate: true }
)

const handleAddProductBlock = () => {
  if (!selectedProductId.value) {
    message.warning('请选择展项')
    return
  }
  const product = productOptionMap.value.get(selectedProductId.value)
  if (!product) {
    throw new Error(`展柜画布展项不存在于候选列表：${selectedProductId.value}`)
  }
  if (blocks.value.length === 0) {
    blocks.value = [
      createCanvasBlock(product, 0, {
        x: 0,
        y: 0,
        width: 1,
        height: 1
      })
    ]
  } else {
    const targetBlockId = selectedBlockId.value || largestBlockId()
    blocks.value = splitCanvasBlock(blocks.value, targetBlockId, product)
  }
  selectedProductId.value = null
  selectedBlockId.value = blocks.value.find((block) => block.itemKey === product.itemKey)?.blockId || ''
}

const handleDeleteSelectedBlock = async () => {
  if (!selectedBlockId.value) {
    return
  }
  blocks.value = deleteCanvasBlock(blocks.value, selectedBlockId.value)
  selectedBlockId.value = blocks.value[0]?.blockId || ''
}

const handleBlockDragStart = (blockId: string) => {
  draggingBlockId.value = blockId
}

const handleBlockDrop = (targetBlockId: string) => {
  if (!draggingBlockId.value) {
    return
  }
  blocks.value = swapCanvasBlockProducts(blocks.value, draggingBlockId.value, targetBlockId)
  selectedBlockId.value = targetBlockId
  draggingBlockId.value = ''
}

const handleResizeStart = (boundary: CanvasBoundaryView, event: PointerEvent) => {
  activeResize.value = {
    boundary,
    baseBlocks: blocks.value.map((block) => ({ ...block })),
    startClientX: event.clientX,
    startClientY: event.clientY
  }
  window.addEventListener('pointermove', handleResizeMove)
  window.addEventListener('pointerup', handleResizeEnd, { once: true })
}

const handleResizeMove = (event: PointerEvent) => {
  if (!activeResize.value || !canvasRef.value) {
    return
  }
  const rect = canvasRef.value.getBoundingClientRect()
  const boundary = activeResize.value.boundary
  const delta =
    boundary.orientation === 'vertical'
      ? (event.clientX - activeResize.value.startClientX) / rect.width
      : (event.clientY - activeResize.value.startClientY) / rect.height
  const clampedDelta = clampCanvasBoundaryDelta(activeResize.value.baseBlocks, { ...boundary, delta })
  blocks.value = resizeCanvasBoundary(activeResize.value.baseBlocks, { ...boundary, delta: clampedDelta })
}

const handleResizeEnd = () => {
  activeResize.value = null
  window.removeEventListener('pointermove', handleResizeMove)
}

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', handleResizeMove)
})

const beforeUploadCanvasBackground: UploadProps['beforeUpload'] = (rawFile) => {
  const allowedType = (CANVAS_BACKGROUND_ALLOWED_TYPES as readonly string[]).includes(rawFile.type)
  const allowedSize = rawFile.size / 1024 / 1024 < CANVAS_BACKGROUND_MAX_SIZE_MB
  if (!allowedType) {
    message.warning('背景图仅支持 JPG、PNG、GIF 格式')
  }
  if (!allowedSize) {
    message.warning(`背景图大小不能超过 ${CANVAS_BACKGROUND_MAX_SIZE_MB}M`)
  }
  if (!allowedType || !allowedSize) {
    return false
  }
  backgroundUploading.value = true
  return true
}

const handleCanvasBackgroundUploadSuccess: UploadProps['onSuccess'] = async (response) => {
  backgroundUploading.value = false
  try {
    const uploadedUrl = requireUploadResponseUrl(response)
    await persistCanvasBackground(uploadedUrl)
    message.success('背景图已保存')
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  }
}

const handleCanvasBackgroundUploadError: UploadProps['onError'] = (error) => {
  backgroundUploading.value = false
  const messageText = resolveUploadErrorMessage(error)
  message.error(messageText)
}

const handleClearCanvasBackground = async () => {
  try {
    await persistCanvasBackground('')
    message.success('背景图已清除')
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  }
}

const persistCanvasBackground = async (nextUrl: string) => {
  if (!hallRecord.value) {
    throw new Error('背景图保存入口缺少展柜数据')
  }
  backgroundSaving.value = true
  try {
    const updatedHall = await ShowroomAdminApi.updateHallCanvasBackground({
      hallId: hallRecord.value.hallId,
      canvasBackgroundImageUrl: nextUrl
    })
    canvasBackgroundImageUrl.value = requireUpdatedCanvasBackgroundImageUrl(updatedHall)
    emit('saved', { hallId: hallRecord.value.hallId })
  } finally {
    backgroundSaving.value = false
  }
}

const requireUploadResponseUrl = (response: unknown) => {
  if (!response || typeof response !== 'object' || Array.isArray(response)) {
    throw new Error('背景图上传响应缺少文件 URL')
  }
  const record = response as Record<string, unknown>
  const data = record.data
  if (typeof data === 'string' && data.trim().length > 0) {
    return data
  }
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    const dataRecord = data as Record<string, unknown>
    if (typeof dataRecord.url === 'string' && dataRecord.url.trim().length > 0) {
      return dataRecord.url
    }
  }
  throw new Error('背景图上传响应缺少文件 URL')
}

const requireUpdatedCanvasBackgroundImageUrl = (value: unknown) => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('背景图保存响应缺少展柜数据')
  }
  const record = value as Record<string, unknown>
  if (typeof record.canvasBackgroundImageUrl !== 'string') {
    throw new Error('背景图保存响应缺少 canvasBackgroundImageUrl 字段')
  }
  return record.canvasBackgroundImageUrl
}

const resolveUploadErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (error && typeof error === 'object' && !Array.isArray(error)) {
    const record = error as Record<string, unknown>
    if (typeof record.msg === 'string' && record.msg.trim().length > 0) {
      return record.msg
    }
    if (typeof record.message === 'string' && record.message.trim().length > 0) {
      return record.message
    }
  }
  return String(error || '背景图上传失败')
}

const buildCanvasLayoutCalculationPayload = (hallId: number, currentBlocks: CanvasBlock[]) => {
  if (!Array.isArray(currentBlocks) || currentBlocks.length === 0) {
    throw new Error('画布布局至少需要一个展项块')
  }
  assertCanvasBlocksInBounds(currentBlocks)
  return {
    hallId,
    items: currentBlocks.map((block, index) => ({
      itemType: block.itemType,
      itemId: block.itemId,
      displayOrder: index + 1,
      layoutX: block.x,
      layoutY: block.y,
      layoutWidth: block.width,
      layoutHeight: block.height
    }))
  }
}

const normalizeCalculatedCanvasMappings = (value: unknown) => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('按 BU 排布响应缺少对象数据')
  }
  const record = value as Record<string, unknown>
  const rawItems = Array.isArray(record.items)
    ? record.items
    : Array.isArray(record.itemMappings)
      ? record.itemMappings
      : null
  if (!rawItems) {
    throw new Error('按 BU 排布响应缺少 itemMappings/items 数组')
  }
  return rawItems.map((item, index) => {
    if (!item || typeof item !== 'object' || Array.isArray(item)) {
      throw new Error(`按 BU 排布响应第 ${index + 1} 个展项不是对象`)
    }
    const mapping = item as Record<string, unknown>
    const itemType = mapping.itemType
    if (itemType !== 'PRODUCT' && itemType !== 'AWARD') {
      throw new Error(`按 BU 排布响应第 ${index + 1} 个展项类型非法`)
    }
    const readNumber = (fieldName: string) => {
      const raw = mapping[fieldName]
      if (typeof raw !== 'number' || !Number.isFinite(raw)) {
        throw new Error(`按 BU 排布响应第 ${index + 1} 个展项缺少 ${fieldName}`)
      }
      return raw
    }
    return {
      itemType: itemType as 'PRODUCT' | 'AWARD',
      itemId: readNumber('itemId'),
      displayOrder: readNumber('displayOrder'),
      layoutX: readNumber('layoutX'),
      layoutY: readNumber('layoutY'),
      layoutWidth: readNumber('layoutWidth'),
      layoutHeight: readNumber('layoutHeight')
    }
  })
}

const handleCalculateBuCanvasLayout = async () => {
  buLayoutCalculating.value = true
  try {
    if (!hallRecord.value) {
      throw new Error('画布布局入口缺少展柜数据')
    }
    const payload = buildCanvasLayoutCalculationPayload(hallRecord.value.hallId, blocks.value)
    const response = await ShowroomAdminApi.calculateHallBuCanvasLayout(payload)
    const nextBlocks = createBlocksFromMappings(normalizeCalculatedCanvasMappings(response))
    blocks.value = nextBlocks
    selectedBlockId.value = nextBlocks[0]?.blockId || ''
    previewMode.value = 'editor'
    message.success('已按 BU 重新排布画布预览')
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
  } finally {
    buLayoutCalculating.value = false
  }
}

const handleSaveCanvasLayout = async () => {
  saving.value = true
  try {
    if (!hallRecord.value) {
      throw new Error('画布布局入口缺少展柜数据')
    }
    assertCanvasIntegrity(blocks.value)
    const payload = buildCanvasLayoutPayload(hallRecord.value.hallId, blocks.value)
    await ShowroomAdminApi.updateHallCanvasLayout(payload)
    message.success('展柜画布布局已保存')
    emit('saved', { hallId: hallRecord.value.hallId })
    dialogVisible.value = false
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    saving.value = false
  }
}

const blockStyle = (block: CanvasBlock) => ({
  left: `${block.x * 100}%`,
  top: `${block.y * 100}%`,
  width: `${block.width * 100}%`,
  height: `${block.height * 100}%`
})

const boundaryStyle = (boundary: CanvasBoundaryView) => {
  if (boundary.orientation === 'vertical') {
    return {
      left: `${(boundary.x || 0) * 100}%`,
      top: `${(boundary.y1 || 0) * 100}%`,
      height: `${((boundary.y2 || 0) - (boundary.y1 || 0)) * 100}%`
    }
  }
  return {
    top: `${(boundary.y || 0) * 100}%`,
    left: `${(boundary.x1 || 0) * 100}%`,
    width: `${((boundary.x2 || 0) - (boundary.x1 || 0)) * 100}%`
  }
}

const largestBlockId = () => {
  return blocks.value
    .slice()
    .sort((left, right) => right.width * right.height - left.width * left.height)[0].blockId
}

const sameEdge = (left: number, right: number) => Math.abs(left - right) <= 0.000001
</script>

<style scoped>
.showroom-hall-canvas-dialog__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.showroom-hall-canvas-dialog__toolbar {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-hall-canvas-dialog__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
  color: #172033;
}

.showroom-hall-canvas-dialog__title strong {
  min-width: 0;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.showroom-hall-canvas-dialog__title span {
  color: #5d6b82;
  font-size: 0.86rem;
}

.showroom-hall-canvas-dialog__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.showroom-hall-canvas-dialog__background-tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
  padding-right: 10px;
  border-right: 1px solid #dbe3ef;
}

.showroom-hall-canvas-dialog__background-tools :deep(.el-upload) {
  display: flex;
}

.showroom-hall-canvas-dialog__mode-toggle {
  flex: 0 0 auto;
}

.showroom-hall-canvas-dialog__select {
  width: 260px;
}

.showroom-hall-canvas-dialog__canvas-shell {
  padding: 14px;
  background: #f6f8fb;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-hall-canvas-dialog__canvas {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: 430px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #bac7d8;
}

.showroom-hall-canvas-dialog__background {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: fill;
  pointer-events: none;
  user-select: none;
}

.showroom-hall-canvas-dialog__block {
  position: absolute;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 5px;
  min-width: 0;
  min-height: 0;
  padding: 10px;
  overflow: hidden;
  color: #172033;
  text-align: left;
  background: #fdfefe;
  border: 1px solid #7d8da3;
  border-radius: 0;
  cursor: grab;
}

.showroom-hall-canvas-dialog__block.is-editor {
  background: transparent;
  border-color: rgba(31, 111, 235, 0.64);
}

.showroom-hall-canvas-dialog__block.is-editor span,
.showroom-hall-canvas-dialog__block.is-editor small {
  display: inline-block;
  width: auto;
  max-width: 100%;
  padding: 2px 6px;
  border: 1px solid rgba(219, 227, 239, 0.82);
  border-radius: 5px;
}

.showroom-hall-canvas-dialog__block.is-editor span {
  background: rgba(255, 255, 255, 0.86);
}

.showroom-hall-canvas-dialog__block.is-editor small {
  background: rgba(255, 255, 255, 0.78);
}

.showroom-hall-canvas-dialog__block.is-website {
  justify-content: flex-end;
  padding: 0;
  overflow: hidden;
  border: 1px solid #d7e3f4;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(233, 243, 255, 0.9), rgba(251, 253, 255, 0.98));
  box-shadow: 0 12px 24px rgba(28, 58, 102, 0.12);
}

.showroom-hall-canvas-dialog__block.is-selected {
  z-index: 2;
  border-color: #1f6feb;
  box-shadow: inset 0 0 0 2px rgba(31, 111, 235, 0.18);
}

.showroom-hall-canvas-dialog__block.is-website.is-selected {
  box-shadow:
    inset 0 0 0 2px rgba(31, 111, 235, 0.22),
    0 14px 28px rgba(28, 58, 102, 0.16);
}

.showroom-hall-canvas-dialog__block span {
  display: block;
  width: 100%;
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.showroom-hall-canvas-dialog__block small {
  display: block;
  width: 100%;
  overflow: hidden;
  color: #5d6b82;
  font-size: 12px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.showroom-hall-canvas-dialog__block-glow {
  position: absolute;
  top: -24px;
  right: -14px;
  width: 92px;
  height: 92px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(143, 202, 255, 0.26), transparent 72%);
  pointer-events: none;
}

.showroom-hall-canvas-dialog__block-art {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.showroom-hall-canvas-dialog__block-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  pointer-events: none;
  user-select: none;
}

.showroom-hall-canvas-dialog__block-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  background:
    linear-gradient(180deg, rgba(228, 236, 248, 0.98), rgba(242, 246, 252, 0.98)),
    repeating-linear-gradient(
      135deg,
      rgba(139, 156, 178, 0.16),
      rgba(139, 156, 178, 0.16) 12px,
      rgba(255, 255, 255, 0.1) 12px,
      rgba(255, 255, 255, 0.1) 24px
    );
  color: #4f6079;
  text-align: center;
}

.showroom-hall-canvas-dialog__block-placeholder strong {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
}

.showroom-hall-canvas-dialog__block-placeholder span {
  width: 100%;
  overflow: hidden;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.showroom-hall-canvas-dialog__block-label {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1;
  display: flex;
  min-height: 35px;
  align-items: center;
  justify-content: center;
  padding: 6px 16px;
  overflow-wrap: anywhere;
  background: #2558a7;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}

.showroom-hall-canvas-dialog__resize-handle {
  position: absolute;
  z-index: 4;
  background: rgba(31, 111, 235, 0);
}

.showroom-hall-canvas-dialog__resize-handle:hover {
  background: rgba(31, 111, 235, 0.18);
}

.showroom-hall-canvas-dialog__resize-handle.is-vertical {
  width: 10px;
  margin-left: -5px;
  cursor: col-resize;
}

.showroom-hall-canvas-dialog__resize-handle.is-horizontal {
  height: 10px;
  margin-top: -5px;
  cursor: row-resize;
}

@media (max-width: 820px) {
  .showroom-hall-canvas-dialog__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-hall-canvas-dialog__actions {
    flex-wrap: wrap;
  }

  .showroom-hall-canvas-dialog__background-tools {
    flex-wrap: wrap;
    padding-right: 0;
    border-right: 0;
  }

  .showroom-hall-canvas-dialog__select {
    width: min(100%, 320px);
  }

  .showroom-hall-canvas-dialog__canvas {
    min-height: 320px;
  }
}
</style>
