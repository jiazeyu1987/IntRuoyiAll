<template>
  <div class="hall-list-table">
    <div class="hall-list-table__toolbar">
      <el-input
        v-model="keyword"
        class="hall-list-table__search"
        clearable
        placeholder="搜索展柜名称 / 编码"
        @keyup.enter="emitSearch"
      />
      <div class="hall-list-table__actions">
        <el-button type="primary" @click="emit('create')">
          <Icon icon="ep:plus" class="mr-5px" />
          新增展柜
        </el-button>
        <el-button
          v-if="manageConfigPackage"
          :loading="exportingConfigPackage"
          plain
          type="primary"
          @click="emit('export-config-package')"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出数据包
        </el-button>
        <el-button
          v-if="manageConfigPackage"
          :loading="importingConfigPackage"
          plain
          type="warning"
          @click="triggerImport"
        >
          <Icon icon="ep:upload" class="mr-5px" />
          导入数据包
        </el-button>
        <input
          ref="configPackageInputRef"
          accept=".zip,application/zip"
          class="hall-list-table__config-input"
          type="file"
          @change="handleConfigPackageChange"
        />
        <el-button
          :disabled="!canGenerateAudio"
          :loading="batchGeneratingAudio"
          type="success"
          @click="emit('batchGenerateAudio')"
        >
          <Icon icon="ep:microphone" class="mr-5px" />
          一键语音
        </el-button>
        <el-button :loading="loading" @click="emitSearch">
          <Icon icon="ep:search" class="mr-5px" />
          查询
        </el-button>
        <el-button @click="resetSearch">
          <Icon icon="ep:refresh-left" class="mr-5px" />
          重置
        </el-button>
      </div>
    </div>

    <div class="hall-list-table__body">
      <el-table
        v-loading="loading"
        :data="filteredHalls"
        row-key="hallId"
        class="hall-list-table__grid"
        empty-text="暂无展柜数据"
      >
        <el-table-column label="展柜名称" min-width="180" prop="name" show-overflow-tooltip />
        <el-table-column label="英文名称" min-width="180" prop="nameEn" show-overflow-tooltip />
        <el-table-column label="展柜编码" width="160" prop="hallCode" show-overflow-tooltip />
        <el-table-column label="描述" min-width="240" prop="description" show-overflow-tooltip />
        <el-table-column label="展项数量" width="112" prop="productCount" />
        <el-table-column label="中文语音" min-width="220">
          <template #default="{ row }">
            <audio
              v-if="hasReadyAudio(row.zhNarration)"
              class="hall-list-table__audio"
              controls
              preload="none"
              :src="row.zhNarration.audioUrl"
            ></audio>
            <span v-else class="hall-list-table__audio-empty">未生成</span>
          </template>
        </el-table-column>
        <el-table-column label="英文语音" min-width="220">
          <template #default="{ row }">
            <audio
              v-if="hasReadyAudio(row.enNarration)"
              class="hall-list-table__audio"
              controls
              preload="none"
              :src="row.enNarration.audioUrl"
            ></audio>
            <span v-else class="hall-list-table__audio-empty">未生成</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="408">
          <template #default="{ row }">
            <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>
            <el-button link type="primary" @click="emit('open-canvas', row)">画布布局</el-button>
            <el-button
              class="hall-list-table__mapping-action"
              link
              type="primary"
              @click="emit('open-mapping', row)"
            >
              维护展项
            </el-button>
            <el-button
              :disabled="!canGenerateAudio || batchGeneratingAudio"
              :loading="generatingAudioHallId === row.hallId"
              link
              type="primary"
              @click="emit('generateAudio', row)"
            >
              语音
            </el-button>
            <el-button
              :loading="publishingPreviewHallId === row.hallId"
              link
              type="primary"
              @click="emit('publishPreviewAsset', row)"
            >
              发布预览图
            </el-button>
            <el-button link type="danger" @click="emit('delete', row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'HallListTable' })

type RawHallRow = Record<string, unknown>
type RawHallProductMapping = Record<string, unknown>

interface HallNarrationSummary {
  narrationVersionId: number
  language: string
  audienceType: string
  status: string
  live: boolean
  audioReady: boolean
  audioUrl: string
  voice: string
}

interface HallListRow {
  [key: string]: unknown
  hallId: number
  hallCode: string
  name: string
  nameEn: string
  description: string
  descriptionEn: string
  canvasBackgroundImageUrl: string
  itemMappings: RawHallProductMapping[]
  productMappings: RawHallProductMapping[]
  productCount: number
  zhNarration: HallNarrationSummary | null
  enNarration: HallNarrationSummary | null
}

const props = withDefaults(
  defineProps<{
    halls: unknown[]
    loading?: boolean
    batchGeneratingAudio?: boolean
    canGenerateAudio?: boolean
    generatingAudioHallId?: number | null
    publishingPreviewHallId?: number | null
    exportingConfigPackage?: boolean
    importingConfigPackage?: boolean
    manageConfigPackage?: boolean
  }>(),
  {
    loading: false,
    batchGeneratingAudio: false,
    canGenerateAudio: true,
    generatingAudioHallId: null,
    publishingPreviewHallId: null,
    exportingConfigPackage: false,
    importingConfigPackage: false,
    manageConfigPackage: false
  }
)

const emit = defineEmits<{
  create: []
  edit: [row: HallListRow]
  search: [filters: { keyword: string }]
  delete: [row: HallListRow]
  'open-mapping': [row: HallListRow]
  'open-canvas': [row: HallListRow]
  generateAudio: [row: HallListRow]
  publishPreviewAsset: [row: HallListRow]
  batchGenerateAudio: []
  'export-config-package': []
  'import-config-package': [file: File]
}>()

const {
  loading,
  batchGeneratingAudio,
  canGenerateAudio,
  generatingAudioHallId,
  publishingPreviewHallId,
  exportingConfigPackage,
  importingConfigPackage
} = toRefs(props)

const keyword = ref('')
const configPackageInputRef = ref<HTMLInputElement>()

const triggerImport = () => {
  configPackageInputRef.value?.click()
}

const handleConfigPackageChange = (event: Event) => {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) {
    return
  }
  emit('import-config-package', file)
  input.value = ''
}

const normalizeRequiredString = (
  row: RawHallRow,
  fieldName: string,
  rowIndex: number,
  allowEmpty = false
) => {
  const value = row[fieldName]
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`展柜列表第 ${rowIndex + 1} 行缺少字符串字段：${fieldName}`)
  }
  return value
}

const normalizeRequiredNumber = (row: RawHallRow, fieldName: string, rowIndex: number) => {
  const value = row[fieldName]
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`展柜列表第 ${rowIndex + 1} 行缺少数字字段：${fieldName}`)
  }
  return value
}

const normalizeNullableNumber = (value: unknown) => {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

const normalizeProductMappings = (row: RawHallRow, rowIndex: number) => {
  const value = Array.isArray(row.itemMappings) ? row.itemMappings : row.productMappings
  if (!Array.isArray(value)) {
    throw new Error(`展柜列表第 ${rowIndex + 1} 行缺少展项映射数组：itemMappings`)
  }
  return value.map((mapping, mappingIndex) => {
    if (!mapping || typeof mapping !== 'object' || Array.isArray(mapping)) {
      throw new Error(`展柜列表第 ${rowIndex + 1} 行第 ${mappingIndex + 1} 个产品映射不是对象`)
    }
    const record = mapping as RawHallProductMapping
    const itemType = record.itemType === 'AWARD' ? 'AWARD' : 'PRODUCT'
    const itemId =
      typeof record.itemId === 'number' && Number.isFinite(record.itemId)
        ? record.itemId
        : normalizeRequiredNumber(record, 'productId', rowIndex)
    return {
      ...record,
      itemType,
      itemId,
      productId: itemType === 'PRODUCT' ? itemId : 0,
      displayOrder:
        typeof record.displayOrder === 'number' && Number.isFinite(record.displayOrder)
          ? record.displayOrder
          : mappingIndex + 1,
      layoutX: normalizeNullableNumber(record.layoutX),
      layoutY: normalizeNullableNumber(record.layoutY),
      layoutWidth: normalizeNullableNumber(record.layoutWidth),
      layoutHeight: normalizeNullableNumber(record.layoutHeight)
    }
  })
}

const normalizeNarrationSummary = (
  row: RawHallRow,
  fieldName: 'zhNarration' | 'enNarration',
  rowIndex: number
): HallNarrationSummary | null => {
  if (!(fieldName in row)) {
    throw new Error(`展柜列表第 ${rowIndex + 1} 行缺少语音字段：${fieldName}`)
  }
  const value = row[fieldName]
  if (value === null) {
    return null
  }
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`展柜列表第 ${rowIndex + 1} 行语音字段不是对象：${fieldName}`)
  }
  const narration = value as RawHallRow
  return {
    narrationVersionId: normalizeRequiredNumber(narration, 'narrationVersionId', rowIndex),
    language: normalizeRequiredString(narration, 'language', rowIndex),
    audienceType: normalizeRequiredString(narration, 'audienceType', rowIndex),
    status: normalizeRequiredString(narration, 'status', rowIndex),
    live: narration.live === true,
    audioReady: narration.audioReady === true,
    audioUrl: normalizeRequiredString(narration, 'audioUrl', rowIndex, true),
    voice: normalizeRequiredString(narration, 'voice', rowIndex, true)
  }
}

const normalizeHall = (value: unknown, rowIndex: number): HallListRow => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`展柜列表第 ${rowIndex + 1} 行不是对象`)
  }

  const row = value as RawHallRow
  const productMappings = normalizeProductMappings(row, rowIndex)
  return {
    hallId: normalizeRequiredNumber(row, 'hallId', rowIndex),
    hallCode: normalizeRequiredString(row, 'hallCode', rowIndex),
    name: normalizeRequiredString(row, 'name', rowIndex),
    nameEn: normalizeRequiredString(row, 'nameEn', rowIndex),
    description: normalizeRequiredString(row, 'description', rowIndex, true),
    descriptionEn: normalizeRequiredString(row, 'descriptionEn', rowIndex, true),
    canvasBackgroundImageUrl: normalizeRequiredString(row, 'canvasBackgroundImageUrl', rowIndex, true),
    itemMappings: productMappings,
    productMappings,
    productCount: productMappings.length,
    zhNarration: normalizeNarrationSummary(row, 'zhNarration', rowIndex),
    enNarration: normalizeNarrationSummary(row, 'enNarration', rowIndex)
  }
}

const normalizedHalls = computed(() => {
  if (!Array.isArray(props.halls)) {
    throw new Error('HallListTable 必须接收真实展柜数组：halls')
  }
  return props.halls.map(normalizeHall)
})

const filteredHalls = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  if (!normalizedKeyword) {
    return normalizedHalls.value
  }
  return normalizedHalls.value.filter((row) => {
    return (
      row.name.toLowerCase().includes(normalizedKeyword) ||
      row.nameEn.toLowerCase().includes(normalizedKeyword) ||
      row.hallCode.toLowerCase().includes(normalizedKeyword)
    )
  })
})

const emitSearch = () => {
  emit('search', { keyword: keyword.value.trim() })
}

const resetSearch = () => {
  keyword.value = ''
  emitSearch()
}

const hasReadyAudio = (narration: HallNarrationSummary | null) => {
  return Boolean(narration?.audioReady && narration.audioUrl)
}
</script>

<style scoped>
.hall-list-table {
  color: #172033;
}

.hall-list-table__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 46px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.hall-list-table__search {
  max-width: 360px;
}

.hall-list-table__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.hall-list-table__config-input {
  display: none;
}

.hall-list-table__body {
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.hall-list-table__grid {
  font-size: 0.9rem;
}

.hall-list-table__grid :deep(.el-table__header th) {
  height: 46px;
  padding: 7px 10px;
  background: #f7f9fc;
  color: #263247;
  font-weight: 600;
}

.hall-list-table__grid :deep(.el-table__body td) {
  height: 52px;
  padding: 7px 10px;
  border-bottom-color: #edf1f6;
}

.hall-list-table__grid :deep(.el-table__row:hover > td.el-table__cell) {
  background: #fafcff;
}

.hall-list-table__audio {
  display: block;
  width: min(100%, 210px);
  height: 32px;
}

.hall-list-table__audio-empty {
  color: #8a96a8;
  font-size: 0.86rem;
}

@media (max-width: 760px) {
  .hall-list-table__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .hall-list-table__search {
    max-width: none;
  }

  .hall-list-table__actions {
    flex-wrap: wrap;
  }
}
</style>
