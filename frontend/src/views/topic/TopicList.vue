<template>
  <a-card title="选题管理">
    <template #extra>
      <a-button type="primary" @click="showCreate">新建选题</a-button>
    </template>
    <a-table :dataSource="topics" :columns="columns" :loading="loading" rowKey="id" :pagination="{ current: page, pageSize: size, total, onChange: load }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'COMPLETED' ? 'green' : 'orange'">{{ record.status }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="$router.push(`/topics/${record.id}/edit`)">编辑</a-button>
            <a-button type="link" @click="goCreateTask(record)">创建任务</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <a-modal v-model:visible="visible" title="新建选题" @ok="handleCreate">
      <a-form :model="form">
        <a-form-item label="标题" :rules="[{ required: true }]">
          <a-input v-model:value="form.title" />
        </a-form-item>
        <a-form-item label="来源">
          <a-select v-model:value="form.source">
            <a-select-option value="MANUAL">手动</a-select-option>
            <a-select-option value="AUTO">自动</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getTopics, createTopic } from '../../api/topic'
import { createTask } from '../../api/task'

const router = useRouter()
const topics = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const visible = ref(false)
const form = ref({ title: '', source: 'MANUAL' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '标题', dataIndex: 'title' },
  { title: '来源', dataIndex: 'source', width: 100 },
  { title: '热度', dataIndex: 'hotScore', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 120 },
]

async function load(p?: number) {
  if (p) page.value = p
  loading.value = true
  try {
    const res: any = await getTopics({ page: page.value, size: size.value })
    topics.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function showCreate() { visible.value = true }

async function handleCreate() {
  await createTopic(form.value)
  message.success('创建成功')
  visible.value = false
  load()
}

async function goCreateTask(record: any) {
  await createTask({ topicId: record.id, title: record.title })
  message.success('任务已创建')
  router.push('/tasks')
}

onMounted(() => load())
</script>