<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Home, Compass, ClipboardList, HeartPulse, ShoppingBag, Bell, ChevronRight } from 'lucide-vue-next'
const route = useRoute(); const router = useRouter(); const query = ref('')
const nav = [{to:'/',label:'首页',icon:Home},{to:'/explore',label:'发现好味道',icon:Compass},{to:'/orders',label:'我的订单',icon:ClipboardList},{to:'/health',label:'健康分析',icon:HeartPulse}]
const title = computed(() => ({'/':'今日概览','/explore':'发现好味道','/orders':'我的订单','/health':'健康分析'}[route.path] || '商家详情'))
function submit(){ router.push({path:'/explore',query:{keyword:query.value}}) }
</script>
<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><div class="brand-mark">H</div><div><b>Healthy</b><span>轻盈生活</span></div></div>
      <div class="user-chip"><div class="avatar">林</div><div><b>林晓雯</b><span>普通用户</span></div><Bell :size="16" class="muted"/></div>
      <nav><router-link v-for="item in nav" :key="item.to" :to="item.to" class="nav-item"><component :is="item.icon" :size="18"/><span>{{item.label}}</span></router-link></nav>
      <div class="sidebar-foot"><div class="tip"><span>本周健康分</span><strong>86</strong><small>较上周 +8</small></div><p>每天一份好好吃饭的计划。</p></div>
    </aside>
    <main class="main"><header class="topbar"><div><div class="eyebrow">星期三，6月18日</div><h1>{{title}}</h1></div><form class="search" @submit.prevent="submit"><Search :size="18"/><input v-model="query" placeholder="搜索商家、菜品或营养标签"/><kbd>⌘ K</kbd></form><button class="cart" @click="router.push('/orders')"><ShoppingBag :size="19"/><i>2</i></button></header><div class="content"><router-view/></div></main>
  </div>
</template>
