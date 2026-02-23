<template>
  <div class="home-container">
    <h1>BML 业务前台</h1>
    <p>此处预留给具体业务功能使用</p>
    <div class="actions">
      <a-button type="primary" @click="$router.push('/admin')">进入后台管理</a-button>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f0f2f5;
}

h1 {
  font-size: 2.5rem;
  color: #1d2129;
  margin-bottom: 1rem;
}

p {
  font-size: 1.2rem;
  color: #86909c;
  margin-bottom: 2rem;
}
</style>
