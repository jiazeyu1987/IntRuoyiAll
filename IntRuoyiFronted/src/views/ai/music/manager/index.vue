<template>
  <doc-alert title="AI 音乐创作" url="https://doc.iocoder.cn/ai/music/" />

  <el-tabs v-model="activeTab">
    <el-tab-pane label="音乐列表" name="list">
      <ContentWrap>
        <el-form
          ref="queryFormRef"
          class="-mb-15px"
          :model="queryParams"
          :inline="true"
          label-width="68px"
        >
          <el-form-item label="用户编号" prop="userId">
            <el-select
              v-model="queryParams.userId"
              clearable
              placeholder="请输入用户编号"
              class="!w-240px"
            >
              <el-option
                v-for="item in userList"
                :key="item.id"
                :label="item.nickname"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="音乐名称" prop="title">
            <el-input
              v-model="queryParams.title"
              placeholder="请输入音乐名称"
              clearable
              @keyup.enter="handleQuery"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="音乐状态" prop="status">
            <el-select
              v-model="queryParams.status"
              placeholder="请选择音乐状态"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.AI_MUSIC_STATUS)"
                :key="String(dict.value)"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="生成模式" prop="generateMode">
            <el-select
              v-model="queryParams.generateMode"
              placeholder="请选择生成模式"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.AI_GENERATE_MODE)"
                :key="String(dict.value)"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间" prop="createTime">
            <el-date-picker
              v-model="queryParams.createTime"
              value-format="YYYY-MM-DD HH:mm:ss"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item label="是否发布" prop="publicStatus">
            <el-select
              v-model="queryParams.publicStatus"
              placeholder="请选择是否发布"
              clearable
              class="!w-240px"
            >
              <el-option
                v-for="dict in getBoolDictOptions(DICT_TYPE.INFRA_BOOLEAN_STRING)"
                :key="String(dict.value)"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="handleQuery">
              <Icon icon="ep:search" class="mr-5px" />
              搜索
            </el-button>
            <el-button @click="resetQuery">
              <Icon icon="ep:refresh" class="mr-5px" />
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </ContentWrap>

      <ContentWrap>
        <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="编号" align="center" prop="id" width="180" fixed="left" />
          <el-table-column
            label="音乐名称"
            align="center"
            prop="title"
            width="180px"
            fixed="left"
          />
          <el-table-column label="用户" align="center" prop="userId" width="180">
            <template #default="scope">
              <span>{{ userList.find((item) => item.id === scope.row.userId)?.nickname }}</span>
            </template>
          </el-table-column>
          <el-table-column label="音乐状态" align="center" prop="status" width="100">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.AI_MUSIC_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="模型" align="center" prop="model" width="180" />
          <el-table-column label="内容" align="center" width="180">
            <template #default="{ row }">
              <el-link
                v-if="row.audioUrl?.length > 0"
                type="primary"
                :href="row.audioUrl"
                target="_blank"
              >
                音乐
              </el-link>
              <el-link
                v-if="row.videoUrl?.length > 0"
                type="primary"
                :href="row.videoUrl"
                target="_blank"
                class="!pl-5px"
              >
                视频
              </el-link>
              <el-link
                v-if="row.imageUrl?.length > 0"
                type="primary"
                :href="row.imageUrl"
                target="_blank"
                class="!pl-5px"
              >
                封面
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="时长(秒)" align="center" prop="duration" width="100" />
          <el-table-column label="提示词" align="center" prop="prompt" width="180" />
          <el-table-column label="歌词" align="center" prop="lyric" width="180" />
          <el-table-column label="描述" align="center" prop="gptDescriptionPrompt" width="180" />
          <el-table-column label="生成模式" align="center" prop="generateMode" width="100">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.AI_GENERATE_MODE" :value="scope.row.generateMode" />
            </template>
          </el-table-column>
          <el-table-column label="风格标签" align="center" prop="tags" width="180">
            <template #default="scope">
              <el-tag v-for="tag in scope.row.tags" :key="tag" round class="ml-2px">
                {{ tag }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="是否发布" align="center" prop="publicStatus">
            <template #default="scope">
              <el-switch
                v-model="scope.row.publicStatus"
                :active-value="true"
                :inactive-value="false"
                :disabled="scope.row.status !== AiMusicStatusEnum.SUCCESS"
                @change="handleUpdatePublicStatusChange(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="任务编号" align="center" prop="taskId" width="180" />
          <el-table-column label="错误信息" align="center" prop="errorMessage" />
          <el-table-column
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            width="180px"
          />
          <el-table-column label="操作" align="center" width="100" fixed="right">
            <template #default="scope">
              <el-button
                link
                type="danger"
                v-hasPermi="['ai:music:delete']"
                @click="handleDelete(scope.row.id)"
              >
                删除
              </el-button>
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

    <el-tab-pane label="TTS 测试" name="tts-test">
      <ContentWrap>
        <TtsTestPane />
      </ContentWrap>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { getBoolDictOptions, getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { MusicApi, MusicVO } from '@/api/ai/music'
import * as UserApi from '@/api/system/user'
import { AiMusicStatusEnum } from '@/views/ai/utils/constants'
import TtsTestPane from './TtsTestPane.vue'

defineOptions({ name: 'AiMusicManager' })

const message = useMessage()
const { t } = useI18n()

const activeTab = ref('list')
const loading = ref(true)
const list = ref<MusicVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: undefined,
  title: undefined,
  status: undefined,
  generateMode: undefined,
  createTime: [],
  publicStatus: undefined
})
const queryFormRef = ref()
const userList = ref<UserApi.UserVO[]>([])

const getList = async () => {
  loading.value = true
  try {
    const data = await MusicApi.getMusicPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery(true)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await MusicApi.deleteMusic(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleUpdatePublicStatusChange = async (row: MusicVO) => {
  try {
    const text = row.publicStatus ? '公开' : '私有'
    await message.confirm('确认要"' + text + '"这条音乐吗?')
    await MusicApi.updateMusic({
      id: row.id,
      publicStatus: row.publicStatus
    })
    await getList()
  } catch {
    row.publicStatus = !row.publicStatus
  }
}

onMounted(async () => {
  await getList()
  userList.value = await UserApi.getSimpleUserList()
})
</script>
