<template>
  <ContentWrap data-common-regulation-library>
    <div class="common-library__toolbar">
      <div>
        <h2>已发布通用规程</h2>
        <p>查看通过简易通用解析发布的规程及历史版本，未加入规程套的规程也会显示。</p>
      </div>
      <div class="common-library__actions">
        <el-button
          :loading="loading"
          :disabled="deletingVersionId !== undefined"
          @click="loadVersions"
          >刷新</el-button
        >
        <el-button @click="emit('manage-sets')">规程套管理</el-button>
      </div>
    </div>
    <el-form inline @submit.prevent>
      <el-form-item label="规程查询">
        <el-input
          v-model="keyword"
          clearable
          placeholder="文件编号、规程名称或版本"
          aria-label="查询已发布通用规程"
          class="common-library__search"
        />
      </el-form-item>
      <el-form-item label="版本状态">
        <el-select v-model="status" aria-label="通用规程版本状态" class="common-library__status">
          <el-option label="全部" value="" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="历史版本" value="RETIRED" />
        </el-select>
      </el-form-item>
    </el-form>
    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />
    <el-alert v-if="deleteError" :title="deleteError" type="error" show-icon :closable="false" />
    <el-table
      v-if="!loadError"
      v-loading="loading"
      :data="pagedVersions"
      row-key="commonRegulationVersionId"
      border
      empty-text="暂无符合条件的已发布通用规程"
      data-common-regulation-library-table
    >
      <el-table-column prop="commonRegulationCode" label="文件编号" min-width="160" />
      <el-table-column prop="commonRegulationName" label="规程名称" min-width="220" />
      <el-table-column prop="versionNo" label="版本" width="100" />
      <el-table-column label="版本状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.lifecycleStatus === 'PUBLISHED' ? 'success' : 'info'">
            {{ row.lifecycleStatus === 'PUBLISHED' ? '已发布' : '历史版本' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效日期" min-width="130">
        <template #default="{ row }">{{ formatEffectiveDate(row.effectiveDate) }}</template>
      </el-table-column>
      <el-table-column label="发布时间" min-width="180">
        <template #default="{ row }">{{ formatPublishedAt(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="detailLoading" @click="openDetail(row)">
            查看内容
          </el-button>
          <el-button
            v-hasPermi="['mes:qc-template:update']"
            link
            type="danger"
            :loading="deletingVersionId === row.commonRegulationVersionId"
            :disabled="loading || deletingVersionId !== undefined"
            @click="deleteVersion(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="!loadError"
      v-model:current-page="pageNo"
      v-model:page-size="pageSize"
      :total="filteredVersions.length"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="mt-16px"
    />

    <el-dialog
      v-model="detailVisible"
      title="通用规程已发布内容"
      width="min(1200px, 95vw)"
      destroy-on-close
      @closed="closeDetail"
    >
      <div v-loading="detailLoading" class="common-library__detail">
        <el-alert
          v-if="detailError"
          :title="detailError"
          type="error"
          show-icon
          :closable="false"
        />
        <template v-else-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="文件编号">{{
              detail.regulationCode
            }}</el-descriptions-item>
            <el-descriptions-item label="规程名称">{{
              detail.regulationName
            }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ detail.versionNo }}</el-descriptions-item>
            <el-descriptions-item label="版本状态">
              {{ detail.lifecycleStatus === 'PUBLISHED' ? '已发布' : '历史版本' }}
            </el-descriptions-item>
            <el-descriptions-item label="生效日期">
              {{ formatEffectiveDate(detail.effectiveDate) }}
            </el-descriptions-item>
            <el-descriptions-item label="发布时间">
              {{ formatPublishedAt(detail.publishedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="末检适用性" :span="2">
              {{ detail.finalInspectionApplicable ? '适用' : '不适用' }}
              {{ detail.finalInspectionNotApplicableReason }}
            </el-descriptions-item>
          </el-descriptions>
          <h3>检验规则</h3>
          <el-table :data="detail.inspectionTypeRules" border>
            <el-table-column prop="label" label="检验类型" min-width="120" />
            <el-table-column prop="roundLabel" label="轮次" min-width="100" />
            <el-table-column prop="taskRule" label="执行规则" min-width="220" />
            <el-table-column prop="releaseGate" label="放行要求" min-width="220" />
          </el-table>
          <section
            v-for="process in detail.processes"
            :key="process.qaProcessId || process.processCode"
          >
            <h3>{{ process.processCode }} / {{ process.processName }}</h3>
            <el-table :data="process.items" border>
              <el-table-column prop="itemName" label="检验项目" min-width="140" />
              <el-table-column prop="standardText" label="接受标准" min-width="220" />
              <el-table-column prop="inspectionMethod" label="检验方法" min-width="200" />
              <el-table-column prop="inspectionTool" label="检验器具" min-width="140" />
              <el-table-column prop="samplingPlanText" label="抽样要求" min-width="160" />
              <el-table-column label="绑定设备" min-width="180">
                <template #default="{ row }">
                  <div v-for="equipment in row.equipmentOptions" :key="equipment.equipmentId">
                    {{ equipment.equipmentCode }} / {{ equipment.equipmentName }}
                  </div>
                  <span v-if="row.equipmentUnboundReason">{{ row.equipmentUnboundReason }}</span>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </template>
      </div>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  formatDate,
  formatDateTimeValue,
  toDateTimeValue,
  type DateTimeDisplayValue
} from '@/utils/formatTime'
import {
  QcTemplateApi,
  type QaCommonRegulationVersionOptionVO,
  type QaInspectionRegulationPublishedVersionVO
} from '@/api/mes/qc/template'

const emit = defineEmits<{ 'manage-sets': [] }>()
const versions = ref<QaCommonRegulationVersionOptionVO[]>([])
const loading = ref(false)
const loadError = ref('')
const deleteError = ref('')
const deletingVersionId = ref<number>()
const keyword = ref('')
const status = ref('')
const pageNo = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<QaInspectionRegulationPublishedVersionVO>()
let detailLoadSerial = 0

const filteredVersions = computed(() => {
  const query = keyword.value.trim().toLocaleLowerCase()
  return versions.value.filter(
    (version) =>
      (!status.value || version.lifecycleStatus === status.value) &&
      (!query ||
        [version.commonRegulationCode, version.commonRegulationName, version.versionNo].some(
          (value) => value.toLocaleLowerCase().includes(query)
        ))
  )
})
const pagedVersions = computed(() =>
  filteredVersions.value.slice((pageNo.value - 1) * pageSize.value, pageNo.value * pageSize.value)
)
watch([keyword, status, pageSize], () => {
  pageNo.value = 1
})

const formatEffectiveDate = (value: DateTimeDisplayValue) => {
  if (value === undefined || value === null || value === '') return ''
  const date = toDateTimeValue(value)
  return date ? formatDate(date, 'YYYY-MM-DD') : '日期格式异常'
}
const formatPublishedAt = (value: DateTimeDisplayValue) => formatDateTimeValue(value, '')
const errorMessage = (error: unknown) => (error instanceof Error ? error.message : String(error))

async function loadVersions() {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    versions.value = await QcTemplateApi.listCommonRegulationReleasedVersions()
    pageNo.value = 1
  } catch (error) {
    loadError.value = '已发布通用规程加载失败：' + errorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openDetail(version: QaCommonRegulationVersionOptionVO) {
  const serial = ++detailLoadSerial
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = undefined
  try {
    const result = await QcTemplateApi.getCommonRegulationPublishedVersion(
      version.commonRegulationVersionId
    )
    if (serial === detailLoadSerial) detail.value = result
  } catch (error) {
    if (serial === detailLoadSerial)
      detailError.value = '通用规程内容加载失败：' + errorMessage(error)
  } finally {
    if (serial === detailLoadSerial) detailLoading.value = false
  }
}

async function deleteVersion(version: QaCommonRegulationVersionOptionVO) {
  if (deletingVersionId.value !== undefined) return
  deletingVersionId.value = version.commonRegulationVersionId
  deleteError.value = ''
  try {
    try {
      await ElMessageBox.confirm(
        `确认删除“${version.commonRegulationName}（${version.commonRegulationCode}）”的 ${version.versionNo} 版本？` +
          '仅删除此版本，其他版本保留。已被规程套、产品或检验业务引用的版本不能删除。' +
          (version.lifecycleStatus === 'PUBLISHED'
            ? '删除当前发布版本后，不会自动恢复历史版本。'
            : ''),
        '删除通用规程版本',
        {
          type: 'warning',
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          distinguishCancelAndClose: true
        }
      )
    } catch (reason) {
      if (reason === 'cancel' || reason === 'close') return
      throw reason
    }
    await QcTemplateApi.deleteCommonRegulationVersion(version.commonRegulationVersionId)
    ElMessage.success('通用规程版本已删除')
    if (detail.value?.publishedVersionId === version.commonRegulationVersionId) {
      detailVisible.value = false
      closeDetail()
    }
    await loadVersions()
  } catch (error) {
    deleteError.value = '删除失败：' + errorMessage(error)
    ElMessage.error(deleteError.value)
  } finally {
    deletingVersionId.value = undefined
  }
}

function closeDetail() {
  detailLoadSerial++
  detailLoading.value = false
  detail.value = undefined
}
onMounted(loadVersions)
onActivated(loadVersions)
</script>

<style scoped>
.common-library__toolbar,
.common-library__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.common-library__toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}
.common-library__toolbar h2 {
  margin: 0 0 8px;
  font-size: 20px;
}
.common-library__toolbar p {
  margin: 0;
  color: var(--el-text-color-secondary);
}
.common-library__search {
  width: 280px;
  max-width: 100%;
}
.common-library__status {
  width: 150px;
}
.common-library__detail {
  min-height: 160px;
}
@media (max-width: 600px) {
  .common-library__search {
    width: 200px;
  }
}
</style>
