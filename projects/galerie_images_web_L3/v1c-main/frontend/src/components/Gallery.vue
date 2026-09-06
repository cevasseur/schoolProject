<script setup lang="ts">
import { onMounted, ref} from 'vue';
import { loadAllImages, loadGallery } from './http-api.ts';

const images = ref<{ Id: number; Name: string; Type: string; Size: String }[]>([])
const imagesGallery = ref<{ id: number; src: string }[]>([]);

async function fetchGallery() {
  await loadAllImages(images);
  imagesGallery.value = await loadGallery(images);
}

function changeImageSelected(id: number) {
  emit('update:imageSelected',id);
}

const emit = defineEmits(['update:imageSelected']);

onMounted(fetchGallery);

defineExpose({ fetchGallery });
</script>

<template>
  <div>
    <h1>Gallery</h1>
  </div>
  <br>
  <img v-for="image in imagesGallery" :src="image.src" @click="changeImageSelected(image.id)">
  
</template>

<style scoped>
.read-the-docs {
  color: #888;
}

img {
  padding: 20px;
  border-radius: 30px;
  max-width: 320px;
  height: 180px;
  transition: transform 0.25s;
}

img:hover {
  transform: scale(1.2);
  cursor: pointer;
}
</style>
