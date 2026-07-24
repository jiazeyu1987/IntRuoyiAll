<template>
  <doc-alert title="报表预览与设计" url="https://doc.iocoder.cn/report/" />

  <el-tabs v-model="activeTab" class="report-management-tabs">
    <el-tab-pane label="报表设计器" name="designer">
      <ContentWrap class="!mb-0">
        <div class="report-designer-toolbar">
          <div class="report-designer-toolbar__actions">
            <el-button v-if="iframeSrc !== defaultDesignerSrc" @click="resetDesignerView">
              <Icon icon="ep:back" class="mr-5px" /> 返回报表列表
            </el-button>
          </div>
          <div class="report-designer-toolbar__hint">
            {{ designerHint }}
          </div>
        </div>
      </ContentWrap>

      <ContentWrap :bodyStyle="{ padding: '0px' }" class="!mt-0 !mb-0">
        <IFrame :src="iframeSrc" />
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="六路识别" name="recognition">
      <ContentWrap class="!mb-0">
        <div class="recognition-toolbar">
          <div class="recognition-toolbar__actions">
            <el-button
              v-for="route in recognitionRoutes"
              :key="route.key"
              :type="activeRouteKey === route.key ? 'primary' : 'default'"
              :loading="recognizingRouteKey === route.key"
              @click="handleRecognize(route.key)"
            >
              {{ route.label }}
            </el-button>

            <el-button :loading="loading" @click="getList">
              <Icon icon="ep:refresh" class="mr-5px" /> 刷新
            </el-button>

            <el-button type="danger" plain :loading="deletingAll" @click="handleDeleteAll">
              <Icon icon="ep:delete" class="mr-5px" /> 删除全部批记录模板
            </el-button>
          </div>

          <el-input
            v-model="queryParams.name"
            clearable
            placeholder="按批记录名称、报表名称或编码搜索"
            class="recognition-toolbar__search"
            @keyup.enter="handleQuery"
            @clear="handleQuery"
          >
            <template #prefix>
              <Icon icon="ep:search" />
            </template>
          </el-input>
        </div>
      </ContentWrap>

      <ContentWrap class="!mt-0">
        <div class="recognition-route-banner">
          <el-tag type="primary" effect="plain">{{ activeRoute.label }}</el-tag>
          <span class="recognition-route-banner__text">{{ activeRoute.description }}</span>
        </div>

        <el-alert
          v-if="loadErrorMessage"
          :title="loadErrorMessage"
          type="error"
          :closable="false"
          show-icon
          class="mb-16px"
        />

        <el-table
          v-loading="loading"
          :data="list"
          :stripe="true"
          :show-overflow-tooltip="true"
          class="recognition-table"
        >
          <el-table-column label="序号" align="center" width="70">
            <template #default="scope">
              {{ (queryParams.pageNo - 1) * queryParams.pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="路线" align="center" prop="routeKey" width="90" />
          <el-table-column label="批记录名称" align="center" prop="batchRecordName" min-width="160" />
          <el-table-column label="来源表序号" align="center" prop="sourceTableIndex" width="110" />
          <el-table-column label="报表名称" align="center" min-width="220">
            <template #default="scope">
              <el-button link type="primary" @click="openDesigner(scope.row.reportId, scope.row.reportName)">
                {{ scope.row.reportName }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="报表编码" align="center" prop="reportCode" min-width="180" />
          <el-table-column label="来源文件名" align="center" prop="sourceFileName" min-width="260" />
          <el-table-column
            label="最近导入时间"
            align="center"
            prop="lastImportTime"
            :formatter="dateFormatter"
            width="180"
          />
          <el-table-column
            label="最近修改时间"
            align="center"
            prop="updateTime"
            :formatter="dateFormatter"
            width="180"
          />
          <el-table-column label="操作" align="center" width="140" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openDesigner(scope.row.reportId, scope.row.reportName)">
                打开
              </el-button>
              <el-button link type="danger" @click="handleDelete(scope.row.reportId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </ContentWrap>
    </el-tab-pane>
  </el-tabs>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { getRefreshToken } from '@/utils/auth'
import { BatchRecordReportApi, type BatchRecordReportVO } from '@/api/mes/pro/batchrecordreport'

defineOptions({ name: 'JimuReport' })

const message = useMessage()

const recognitionRoutes = [
  { key: 'A', label: 'A 直接 .doc', description: '直接解析 .doc 二进制表格并生成报表' },
  { key: 'B', label: 'B Word COM', description: '通过 Word COM 对象模型读取样本并生成报表' },
  { key: 'C', label: 'C 规范化解析', description: '先规范化为 .docx / HTML 再解析生成报表' },
  { key: 'D', label: 'D PDF 解析', description: '先转 PDF 再做版面解析生成报表' },
  { key: 'E', label: 'E OCR/LLM', description: '先生成图像表示，再走 OCR / LLM 结构化识别' },
  { key: 'F', label: 'F Excel 中转', description: '先转 Excel 中间表示，再导入生成报表' }
]

const activeTab = ref('designer')
const activeRouteKey = ref('A')
const recognizingRouteKey = ref('')
const deletingAll = ref(false)
const loading = ref(false)
const list = ref<BatchRecordReportVO[]>([])
const total = ref(0)
const loadErrorMessage = ref('')

const defaultDesignerSrc = computed(
  () => import.meta.env.VITE_BASE_URL + '/jmreport/list?token=' + getRefreshToken()
)
const iframeSrc = ref(defaultDesignerSrc.value)
const designerHint = ref('当前为全局报表设计器列表。')

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  routeKey: activeRouteKey.value
})

const activeRoute = computed(
  () => recognitionRoutes.find((route) => route.key === activeRouteKey.value) || recognitionRoutes[0]
)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error
  }
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  return fallback
}

const resetDesignerView = () => {
  iframeSrc.value = defaultDesignerSrc.value
  designerHint.value = '当前为全局报表设计器列表。'
}

const getList = async () => {
  loading.value = true
  loadErrorMessage.value = ''
  try {
    const data = await BatchRecordReportApi.getGeneratedReportPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch (error: any) {
    list.value = []
    total.value = 0
    loadErrorMessage.value = error?.message || '六路识别报表加载失败，请联系管理员检查后端识别链路。'
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleRecognize = async (routeKey: string) => {
  activeRouteKey.value = routeKey
  queryParams.routeKey = routeKey
  queryParams.pageNo = 1
  try {
    recognizingRouteKey.value = routeKey
    const result = await BatchRecordReportApi.recognizeFixedRoute(routeKey)
    message.alert(
      `路线 ${routeKey} 已生成或更新 ${result.importedCount} 份报表：新建 ${result.createdCount} 份，更新 ${result.updatedCount} 份。`
    )
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, `路线 ${routeKey} 识别失败，请联系管理员检查后端识别链路。`))
  } finally {
    recognizingRouteKey.value = ''
  }
}

const openDesigner = async (reportId: string, reportName: string) => {
  try {
    const data = await BatchRecordReportApi.getDesignerPath(reportId)
    const separator = data.path.includes('?') ? '&' : '?'
    iframeSrc.value =
      import.meta.env.VITE_BASE_URL +
      data.path +
      `${separator}token=${encodeURIComponent(getRefreshToken() || '')}`
    designerHint.value = data.path.includes('/jmreport/view/')
      ? `当前正在预览：${reportName}`
      : `当前正在设计：${reportName}`
    activeTab.value = 'designer'
  } catch (error: any) {
    message.error(error?.message || '报表设计器路径加载失败，请联系管理员。')
  }
}

const handleDelete = async (reportId: string) => {
  try {
    await message.delConfirm()
  } catch {
    return
  }

  try {
    await BatchRecordReportApi.deleteGeneratedReport(reportId)
    message.success('删除成功')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '电子批记录报表删除失败，请联系管理员。'))
  }
}

const handleDeleteAll = async () => {
  let confirmation = ''
  try {
    const promptResult = await message.prompt(
      '该操作会删除电子批记录目录下的全部批记录模板。请输入 PROD 确认。',
      '删除全部批记录模板'
    )
    confirmation = String(promptResult.value || '')
  } catch {
    return
  }
  if (confirmation.trim() !== 'PROD') {
    message.warning('必须输入 PROD 才能删除全部批记录模板。')
    return
  }

  try {
    deletingAll.value = true
    const result = await BatchRecordReportApi.deleteAllGeneratedReports('PROD')
    message.success(
      `已删除可删除的批记录模板：删除 ${result.deletedReportCount} 份报表，清理 ${result.deletedMetadataCount} 条元数据，保留 ${result.skippedBoundReportCount} 份已绑定模板。`
    )
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '删除全部批记录模板失败，请联系管理员。'))
  } finally {
    deletingAll.value = false
  }
}

watch(
  () => defaultDesignerSrc.value,
  (value) => {
    if (!iframeSrc.value) {
      iframeSrc.value = value
    }
  },
  { immediate: true }
)

onMounted(() => {
  getList()
})
</script>

<style scoped>
.report-management-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.report-designer-toolbar,
.recognition-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.report-designer-toolbar__actions,
.recognition-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.report-designer-toolbar__hint,
.recognition-route-banner__text {
  color: #4b5563;
  font-size: 0.9rem;
}

.recognition-toolbar__search {
  width: 320px;
  max-width: 100%;
}

.recognition-route-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.recognition-table {
  border-top: 1px solid #edf1f6;
}

@media (max-width: 768px) {
  .report-designer-toolbar,
  .recognition-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .recognition-toolbar__search {
    width: 100%;
  }
}
</style>
