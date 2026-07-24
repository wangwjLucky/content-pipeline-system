<template>
  <a-card title="发布管理">
    <template #extra>
      <a-space>
        <a-button @click="$router.push('/publish/calendar')">发布日历</a-button>
        <a-button type="primary" @click="showCreate">新建发布</a-button>
      </a-space>
    </template>
    <a-table :dataSource="publishes" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'PUBLISHED' ? 'green' : record.status === 'FAILURE' ? 'red' : record.status === 'CANCELLED' ? 'default' : 'orange'">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button v-if="record.status === 'PENDING'" type="primary" size="small" @click="handlePublish(record.id)">发布</a-button>
            <a-button v-if="record.status === 'PENDING'" size="small" @click="showSchedule(record)">定时</a-button>
            <a-popconfirm v-if="record.status === 'PENDING'" title="确定取消发布？" @confirm="handleCancel(record.id)">
              <a-button size="small" danger>取消</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="visible" title="新建发布" @ok="handleCreate" width="600px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="任务 ID"><a-input-number v-model:value="form.taskId" style="width: 100%" /></a-form-item>
        <a-form-item label="平台">
          <a-select v-model:value="form.platform">
            <a-select-option value="douyin">抖音</a-select-option>
            <a-select-option value="kuaishou">快手</a-select-option>
            <a-select-option value="xiaohongshu">小红书</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="标题"><a-input v-model:value="form.title" /></a-form-item>
        <a-form-item label="标签（逗号分隔）"><a-input v-model:value="form.tags" placeholder="#AI,#程序员,#ChatGPT" /></a-form-item>
        <a-form-item label="封面 URL"><a-input v-model:value="form.coverUrl" placeholder="https://..." /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="scheduleVisible" title="定时发布" @ok="handleSchedule" ok-text="确认定时">
      <a-form>
        <a-form-item label="定时时间">
          <a-date-picker v-model:value="scheduleTime" show-time style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getPublishes, createPublish, doPublish, schedulePublish, cancelPublishItem } from '../../api/publish'

const publishes = ref<any[]>([])
const loading = ref(false)
const visible = ref(false)
const scheduleVisible = ref(false)
const scheduleTargetId = ref<number | null>(null)
const scheduleTime = ref<any>(null)
const form = ref({ taskId: undefined, platform: 'douyin', title: '', tags: '', coverUrl: '' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '任务ID', dataIndex: 'taskId', width: 80 },
  { title: '平台', dataIndex: 'platform', width: 80 },
  { title: '标题', dataIndex: 'title' },
  { title: '标签', dataIndex: 'tags', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '发布时间', dataIndex: 'publishedAt', width: 160 },
  { title: '操作', key: 'action', width: 200 },
]

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待发布', PUBLISHED: '已发布', FAILURE: '失败', CANCELLED: '已取消' }
  return map[s] || s
}

async function load() {
  loading.value = true
  try {
    const res: any = await getPublishes()
    publishes.value = res.data || []
  } finally {
    loading.value = false
  }
}

function showCreate() {
  visible.value = true
  form.value = { taskId: undefined, platform: 'douyin', title: '', tags: '', coverUrl: '' }
}

async function handleCreate() {
  await createPublish(form.value)
  message.success('创建成功')
  visible.value = false
  load()
}

async function handlePublish(id: number) {
  await doPublish(id)
  message.success('发布成功')
  load()
}

function showSchedule(record: any) {
  scheduleTargetId.value = record.id
  scheduleTime.value = null
  scheduleVisible.value = true
}

async function handleSchedule() {
  if (!scheduleTargetId.value || !scheduleTime.value) return
  await schedulePublish(scheduleTargetId.value, scheduleTime.value.toISOString())
  message.success('定时设置成功')
  scheduleVisible.value = false
  load()
}

async function handleCancel(id: number) {
  await cancelPublishItem(id)
  message.success('已取消发布')
  load()
}

onMounted(() => load())
</script>