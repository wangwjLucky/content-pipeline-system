<template>
  <a-card :title="isEdit ? '编辑选题' : '新建选题'">
    <a-form :model="form" layout="vertical">
      <a-form-item label="标题" :rules="[{ required: true }]">
        <a-input v-model:value="form.title" />
      </a-form-item>
      <a-form-item label="来源">
        <a-select v-model:value="form.source">
          <a-select-option value="MANUAL">手动</a-select-option>
          <a-select-option value="AUTO">自动</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="状态" v-if="isEdit">
        <a-select v-model:value="form.status">
          <a-select-option value="PENDING">待处理</a-select-option>
          <a-select-option value="COMPLETED">已完成</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSave">保存</a-button>
          <a-button @click="$router.back()">取消</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getTopic, createTopic, updateTopic } from '../../api/topic'

const route = useRoute()
const router = useRouter()
const isEdit = ref(false)
const form = ref({ title: '', source: 'MANUAL', status: 'PENDING' })

onMounted(async () => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    const res: any = await getTopic(Number(id))
    if (res.data) {
      form.value = { title: res.data.title, source: res.data.source || 'MANUAL', status: res.data.status || 'PENDING' }
    }
  }
})

async function handleSave() {
  if (isEdit.value) {
    await updateTopic(Number(route.params.id), form.value)
    message.success('更新成功')
  } else {
    await createTopic(form.value)
    message.success('创建成功')
  }
  router.push('/topics')
}
</script>