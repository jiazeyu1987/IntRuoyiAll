<template>
  <Dialog
    v-model="dialogVisible"
    class="scheme-d-form-control"
    :title="dialogTitle"
    width="960px"
    data-testid="dcc-project-file-template-dialog"
  >
    <div v-loading="loading">
      <div class="mb-12px flex items-center justify-between gap-12px">
        <span class="text-13px text-[var(--el-text-color-secondary)]">{{ projectLabel }}</span>
        <el-button
          type="primary"
          plain
          data-testid="dcc-project-file-template-add-item"
          :disabled="loading || Boolean(loadError)"
          @click="addItem"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增文件项
        </el-button>
      </div>

      <el-alert
        v-if="loadError"
        class="mb-12px"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
      />

      <el-table
        :data="rows"
        border
        row-key="key"
        empty-text="项目文件模板不能为空"
        data-testid="dcc-project-file-template-row"
      >
        <el-table-column label="阶段 / 文件类型" min-width="420">
          <template #default="{ row, $index }">
            <el-cascader
              v-model="row.fileTypeTaxonomyId"
              class="w-full"
              :options="taxonomyTreeOptions"
              :props="taxonomyCascaderProps"
              clearable
              filterable
              placeholder="请选择至少三级文件分类"
              @change="clearRowError($index)"
            />
            <div v-if="rowErrors[$index]?.fileTypeTaxonomyId" class="project-template-row-error">
              {{ rowErrors[$index].fileTypeTaxonomyId }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文件名称" min-width="280">
          <template #default="{ row, $index }">
            <el-input
              v-model="row.fileName"
              maxlength="255"
              show-word-limit
              placeholder="请输入模板文件名称"
              @input="clearRowError($index)"
            />
            <div v-if="rowErrors[$index]?.fileName" class="project-template-row-error">
              {{ rowErrors[$index].fileName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="152" align="center">
          <template #default="{ $index }">
            <div class="project-template-sort-actions">
              <el-tooltip content="上移" placement="top">
                <el-button
                  link
                  class="scheme-d-icon-button"
                  :disabled="$index === 0"
                  aria-label="上移模板项"
                  @click="moveItem($index, -1)"
                >
                  <Icon icon="ep:top" />
                </el-button>
              </el-tooltip>
              <el-tooltip content="下移" placement="top">
                <el-button
                  link
                  class="scheme-d-icon-button"
                  :disabled="$index === rows.length - 1"
                  aria-label="下移模板项"
                  @click="moveItem($index, 1)"
                >
                  <Icon icon="ep:bottom" />
                </el-button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  link
                  type="danger"
                  class="scheme-d-icon-button"
                  aria-label="删除模板项"
                  @click="removeItem($index)"
                >
                  <Icon icon="ep:delete" />
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button
          type="primary"
          :loading="saving"
          :disabled="loading || Boolean(loadError)"
          @click="saveTemplate"
          >保存模板</el-button
        >
        <el-button @click="dialogVisible = false">取消</el-button>
      </div>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { handleTree } from '@/utils/tree'
import {
  getProjectCodeFileTemplate,
  replaceProjectCodeFileTemplate,
  type DccProjectCodeRespVO,
  type DccProjectFileTemplateRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import type { DccFileTypeTaxonomyVO } from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import { resolveUploadErrorMessage } from '../../upload/submitter'

defineOptions({ name: 'DccProjectFileTemplateEditor' })

type EditorRow = {
  key: number
  fileTypeTaxonomyId?: number
  fileName: string
}

type EditorRowError = {
  fileTypeTaxonomyId?: string
  fileName?: string
}

const emit = defineEmits<{
  (event: 'saved', template: DccProjectFileTemplateRespVO): void
}>()

const message = useMessage()
const dialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const currentProject = ref<DccProjectCodeRespVO>()
const taxonomyRows = ref<DccFileTypeTaxonomyVO[]>([])
const rows = ref<EditorRow[]>([])
const rowErrors = ref<EditorRowError[]>([])
let rowKeySequence = 0

const dialogTitle = computed(() =>
  currentProject.value
    ? `项目文件模板 - ${currentProject.value.projectCode || currentProject.value.projectName}`
    : '项目文件模板'
)
const projectLabel = computed(() =>
  currentProject.value
    ? `${currentProject.value.projectName} / ${currentProject.value.projectCode || '-'}`
    : ''
)
const activeTaxonomyRows = computed(() =>
  taxonomyRows.value
    .filter((row) => row.id && row.active)
    .map((row) => ({ ...row, children: undefined }))
)
const taxonomyTreeOptions = computed(
  () => handleTree(activeTaxonomyRows.value.map((row) => ({ ...row }))) as DccFileTypeTaxonomyVO[]
)
const taxonomyPathDepthMap = computed(() => {
  const depthMap = new Map<number, number>()
  const sortedRows = [...activeTaxonomyRows.value].sort(
    (left, right) => (left.levelNo || 0) - (right.levelNo || 0)
  )
  sortedRows.forEach((row) => {
    if (!row.id) return
    const parentDepth = row.parentId ? depthMap.get(row.parentId) : 0
    if (row.parentId && parentDepth === undefined) return
    depthMap.set(row.id, (parentDepth || 0) + 1)
  })
  return depthMap
})
const taxonomyCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: false,
  checkStrictly: true
} as const

const toEditorRows = (template: DccProjectFileTemplateRespVO): EditorRow[] =>
  template.items.map((item) => ({
    key: ++rowKeySequence,
    fileTypeTaxonomyId: item.fileTypeTaxonomyId,
    fileName: item.fileName
  }))

const open = async (project: DccProjectCodeRespVO) => {
  currentProject.value = project
  dialogVisible.value = true
  loading.value = true
  loadError.value = ''
  rows.value = []
  rowErrors.value = []
  try {
    const template = await getProjectCodeFileTemplate(project.id)
    taxonomyRows.value = template.taxonomyOptions || []
    rows.value = toEditorRows(template)
    if (taxonomyRows.value.length === 0) {
      loadError.value = '当前没有绑定唯一启用文件类别的三级分类，无法配置项目模板'
      return
    }
    if (rows.value.length === 0) addItem()
  } catch (error) {
    taxonomyRows.value = []
    loadError.value = resolveUploadErrorMessage(error, '项目文件模板加载失败，请稍后重试')
    message.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const addItem = () => {
  rows.value.push({ key: ++rowKeySequence, fileName: '' })
  rowErrors.value = []
}

const removeItem = (index: number) => {
  rows.value.splice(index, 1)
  rowErrors.value = []
}

const moveItem = (index: number, offset: number) => {
  const targetIndex = index + offset
  if (targetIndex < 0 || targetIndex >= rows.value.length) return
  const [row] = rows.value.splice(index, 1)
  rows.value.splice(targetIndex, 0, row)
  rowErrors.value = []
}

const clearRowError = (index: number) => {
  if (rowErrors.value[index]) rowErrors.value[index] = {}
}

const validateRows = () => {
  if (rows.value.length === 0) {
    message.warning('项目文件模板不能为空')
    return false
  }
  const errors: EditorRowError[] = rows.value.map(() => ({}))
  const uniqueKeys = new Set<string>()
  let valid = true
  rows.value.forEach((row, index) => {
    if (!row.fileTypeTaxonomyId) {
      errors[index].fileTypeTaxonomyId = '请选择文件分类'
      valid = false
    } else if ((taxonomyPathDepthMap.value.get(row.fileTypeTaxonomyId) || 0) < 3) {
      errors[index].fileTypeTaxonomyId = '文件分类至少选择到第三级'
      valid = false
    }
    const fileName = row.fileName.trim()
    if (!fileName) {
      errors[index].fileName = '请输入文件名称'
      valid = false
    }
    if (row.fileTypeTaxonomyId && fileName) {
      const uniqueKey = `${row.fileTypeTaxonomyId}:${fileName.toLocaleLowerCase()}`
      if (uniqueKeys.has(uniqueKey)) {
        errors[index].fileName = '模板中存在重复的文件分类和文件名称'
        valid = false
      }
      uniqueKeys.add(uniqueKey)
    }
  })
  rowErrors.value = errors
  return valid
}

const saveTemplate = async () => {
  if (!currentProject.value?.id || loadError.value || !validateRows()) return
  saving.value = true
  try {
    const template = await replaceProjectCodeFileTemplate(currentProject.value.id, {
      items: rows.value.map((row, index) => ({
        fileTypeTaxonomyId: Number(row.fileTypeTaxonomyId),
        fileName: row.fileName.trim(),
        sortOrder: index + 1
      }))
    })
    rows.value = toEditorRows(template)
    emit('saved', template)
    message.success('项目文件模板已保存')
    dialogVisible.value = false
  } catch (error) {
    message.error(resolveUploadErrorMessage(error, '项目文件模板保存失败，请稍后重试'))
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.project-template-row-error {
  margin-top: 4px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.4;
}

.project-template-sort-actions {
  display: grid;
  grid-template-columns: repeat(3, 32px);
  justify-content: center;
  gap: 4px;
}
</style>
