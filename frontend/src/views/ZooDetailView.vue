<template>
  <div class="zoo-detail-page" v-if="zoo">
    <el-page-header @back="router.push('/zoo')" :content="zoo.name" />

    <el-card class="card">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="城市">{{ zoo.city }} · {{ zoo.province }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ zoo.address }}</el-descriptions-item>
        <el-descriptions-item label="成人票">¥{{ zoo.ticketAdult }}</el-descriptions-item>
        <el-descriptions-item label="儿童票">¥{{ zoo.ticketChild }}</el-descriptions-item>
        <el-descriptions-item label="门票备注" :span="2">{{ zoo.ticketNote || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开放时间">{{ zoo.openTime }}</el-descriptions-item>
        <el-descriptions-item label="时间备注">{{ zoo.openHoursNote || '—' }}</el-descriptions-item>
        <el-descriptions-item label="官网">{{ zoo.website || '—' }}</el-descriptions-item>
        <el-descriptions-item label="明星动物">{{ zoo.starAnimals }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ zoo.description }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="card">
      <template #header>园内动物（{{ zoo.animals.length }}）</template>
      <el-table :data="zoo.animals">
        <el-table-column prop="animalName" label="动物" width="120" />
        <el-table-column prop="category" label="类别" width="90" />
        <el-table-column prop="exhibitArea" label="展区" width="140" />
        <el-table-column prop="showTime" label="投喂/表演时间" min-width="180" />
        <el-table-column label="明星" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.star" type="danger" size="small">明星</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="funFacts" label="冷知识" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getZoo } from '../api/zoo'

const route = useRoute()
const router = useRouter()
const zoo = ref(null)

onMounted(async () => {
  zoo.value = await getZoo(route.params.id)
})
</script>

<style scoped>
.zoo-detail-page { padding: 20px; }
.card { margin-top: 16px; }
</style>
