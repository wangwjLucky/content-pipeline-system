<template>
  <a-card title="脚本列表">
    <template #extra>
      <a-space>
        <a-select v-model:value="filterStatus" placeholder="状态筛选" style="width: 140px" allow-clear @change="() => load()">
          <a-select-option value="PENDING_REVIEW">待审核</a-select-option>
          <a-select-option value="APPROVED">已批准</a-select-option>
          <a-select-option value="REJECTED">已驳回</a-select-option>
        </a-select>
      </a-space>
    </template>
    <a-table :dataSource="scripts" :columns="columns" :loading="loading" rowKey="id" :pagination="{ current: page, pageSize: size, total, onChange: load }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="$router.push(`/scripts/${record.id}`)">查看</a-button>
            <a-button type="link" v-if="record.status === 'PENDING_REVIEW'" @click="handleReview(record, 'approve')">批准</a-button>
            <a-button type="link" danger v-if="record.status === 'PENDING_REVIEW'" @click="handleReview(record, 'reject')">驳回</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getScripts, reviewScript } from '../../api/script'

const scripts = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterStatus = ref<string | undefined>(undefined)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '标题', dataIndex: 'title' },
  { title: '任务 ID', dataIndex: 'taskId', width: 100 },
  { title: '版本', dataIndex: 'version', width: 60 },
  { title: '状态', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 200 },
]

function statusColor(s: string) {
  const map: Record<string, string> = { PENDING_REVIEW: 'purple', APPROVED: 'green', REJECTED: 'red' }
  return map[s] || 'default'
}

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待处理', PENDING_REVIEW: '待审核', APPROVED: '已批准', REJECTED: '已驳回' }
  return map[s] || s
}

async function load(p?: number) {
  if (p) page.value = p
  loading.value = true
  try {
    const res: any = await getScripts({ page: page.value, size: size.value, status: filterStatus.value })
    scripts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleReview(record: any, action: 'approve' | 'reject') {
  if (action === 'approve') {
    Modal.confirm({
      title: '批准脚本',
      content: '批准后任务将进入分镜阶段，确定？',
      onOk: async () => {
        await reviewScript(record.id, 'approve')
        message.success('已批准')
        load()
      },
    })
  } else {
    let reason = ''
    Modal.confirm({
      title: '驳回脚本',
      content: '请输入驳回原因',
      onOk: async () => {
        await reviewScript(record.id, 'reject', undefined, reason)
        message.success('已驳回')
        load()
      },
    })
  }
}

onMounted(() => load())
</script>