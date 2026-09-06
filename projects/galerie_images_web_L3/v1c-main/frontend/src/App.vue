<script setup lang="ts">

</script>

<template>
  <nav>
    <router-link to="/">Home</router-link>
    <router-link to="/memory">Memory</router-link>
    <router-link to="/carousel">Carousel</router-link>
    <router-link to="/paint">Paint</router-link>
  </nav>
  <router-view />
</template>

<style scoped>

nav {
  display: flex;
  gap: 15px;
  background: #007bff;
  padding: 10px;
  border-radius: 5px;
}

nav a {
  color: white;
  text-decoration: none;
  padding: 8px;
  transition: background 0.3s ;
}

nav a:hover {
  background: #0056b3;
  border-radius: 5px;
}
</style>
