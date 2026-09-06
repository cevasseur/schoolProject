<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { loadAllImages } from './http-api.ts';
import Comment from './Comments.vue';

const images = ref<{ Id: number; Name: string; Type: string; Size: String }[]>([]);
const currentIndex = ref(0);

onMounted(async () => {
  images.value = await loadAllImages(images);
});

function nextImage() {
  currentIndex.value = (currentIndex.value + 1) % images.value.length;
}

function prevImage() {
  currentIndex.value = (currentIndex.value - 1 + images.value.length) % images.value.length;
}
</script>

<template>
  <div class="carousel-container">
    <h1>Carousel d'images</h1>
    <div class="carousel">
      <button @click="prevImage" class="nav-button">‹</button>
      <img v-if="images.length" :src="`/images/${images[currentIndex].Id}`" :alt="images[currentIndex].Name" class="carousel-image">
      <button @click="nextImage" class="nav-button">›</button>
    </div>
  </div>
  <div class="commentaires">
    <Comment :imageId="images[currentIndex].Id"></Comment>
  </div>
</template>

<style scoped>
.carousel-container {
  text-align: center;
  max-width: 600px;
  margin: auto;
}

.carousel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.carousel-image {
  width: 100%;
  max-width: 500px;
  height: auto;
  border-radius: 10px;
}

.nav-button {
  background: none;
  border: none;
  font-size: 2rem;
  cursor: pointer;
}
</style>