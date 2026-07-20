<template>
  <a-card title="任务列表">
    <template #extra>
      <a-select v-model:value="statusFilter" placeholder="筛选状态" style="width: 200px" allowClear @change="() => load()">
        <a-select-option value="">全部</a-select-option>
        <a-select-option v-for="s in statuses" :key="s" :value="s">{{ s }}</a-select-option>
      </a-select>
    </template>
    <a-table :dataSource="tasks" :columns="columns" :loading="loading" rowKey="id" :pagination="{ current: page, pageSize: size, total, onChange: load }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="$router.push(`/tasks/${record.id}`)">详情</a>
            <a v-if="record.status === 'SCRIPT_REVIEW' && record.scriptId" @click="$router.push(`/scripts/${record.scriptId}`)">审核</a>
            <a v-if="record.status === 'ERROR'" @click="handleRetry(record.id)">重试</a>
            <a v-if="!['PUBLISHED', 'CANCELLED'].includes(record.status)" @click="handleCancel(record.id)">取消</a>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { getTasks, retryTask, cancelTask } from '../../api/task'

const router = useRouter()
const tasks = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const statusFilter = ref('')

const statuses = ['WAIT', 'SCRIPTING', 'SCRIPT_REVIEW', 'STORYBOARD', 'GENERATING', 'VOICEOVER', 'EDITING', 'REVIEW', 'READY', 'PUBLISHED', 'CANCELLED', 'ERROR']

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '标题', dataIndex: 'title' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '进度', dataIndex: 'progress', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 250 },
]

function statusColor(s: string) {
  const map: Record<string, string> = { WAIT: 'orange', SCRIPTING: 'blue', SCRIPT_REVIEW: 'purple', STORYBOARD: 'geekblue', GENERATING: 'blue', VOICEOVER: 'cyan', EDITING: 'processing', REVIEW: 'purple', READY: 'green', PUBLISHED: 'green', CANCELLED: 'default', ERROR: 'red' }
  return map[s] || 'default'
}

function statusLabel(s: string) {
  const map: Record<string, string> = { WAIT: '等待中', SCRIPTING: '脚本生成中', SCRIPT_REVIEW: '脚本审核', STORYBOARD: '分镜中', GENERATING: '素材生成中', VOICEOVER: '配音中', EDITING: '剪辑中', REVIEW: '终审中', READY: '待发布', PUBLISHED: '已发布', CANCELLED: '已取消', ERROR: '异常' }
  return map[s] || s
}

async function load(p?: number) {
  if (p) page.value = p
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res: any = await getTasks(params)
    tasks.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleRetry(id: number) {
  Modal.confirm({ title: '确定重试该任务？', onOk: async () => { await retryTask(id); message.success('已重试'); load() } })
}

function handleCancel(id: number) {
  Modal.confirm({ title: '确定取消该任务？', onOk: async () => { await cancelTask(id); message.success('已取消'); load() } })
}

onMounted(() => load())
</script>