<template>
  <div class="zoo-list-page">
    <div class="toolbar">
      <h3>动物园一览</h3>
      <div class="filters">
        <el-select v-model="city" placeholder="按城市筛选" clearable style="width: 160px" @change="load">
          <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input v-model="animal" placeholder="按动物筛选（如：熊猫）" clearable style="width: 180px" @change="load" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
    </div>

    <el-table :data="zoos" stripe @row-click="goDetail" style="cursor: pointer">
      <el-table-column prop="name" label="动物园" min-width="180" />
      <el-table-column prop="city" label="城市" width="110" />
      <el-table-column label="门票（成人/儿童）" width="150">
        <template #default="{ row }">
          ¥{{ fmt(row.ticketAdult) }} / ¥{{ fmt(row.ticketChild) }}
        </template>
      </el-table-column>
      <el-table-column prop="openTime" label="开放时间" width="120" />
      <el-table-column prop="starAnimals" label="明星动物" min-width="200" show-overflow-tooltip />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listZoos, listCities } from '../api/zoo'

const router = useRouter()
const zoos = ref([])
const cities = ref([])
const city = ref('')
const animal = ref('')
const page = ref(1)
const size = 10
const total = ref(0)

async function load() {
  const data = await listZoos({
    city: city.value || undefined,
    animal: animal.value || undefined,
    page: page.value,
    size
  })
  zoos.value = data.records
  total.value = data.total
}

function goDetail(row) {
  router.push(`/zoo/${row.id}`)
}

function fmt(v) {
  return v == null ? '待定' : Number(v)
}

onMounted(async () => {
  cities.value = await listCities()
  await load()
})
</script>

<style scoped>
.zoo-list-page { padding: 20px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.toolbar h3 { margin: 0; }
.filters { display: flex; gap: 10px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
