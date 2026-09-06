<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import { loadImage, loadSimilar } from './http-api.ts';

const props = defineProps<{ imageId: number; N: number; descriptor: string }>();

const imagesSimilar = ref<{ Id: number; Name: string; Type: string; Size: String, Similarity: number }[]>([])
const imageUrls = ref<Record<number, string>>({});

async function fetchSimilar() {
  if (props.imageId >= 0) {
    await loadSimilar(props.imageId,props.N,props.descriptor, imagesSimilar);
    await nextTick();
    await loadUrls();
  } else {
    console.warn("imageId est invalide :", props.imageId);
  }
}

async function loadUrls() {
  const urls: Record<number, string> = {};
  const imageUrl = ref<string>("");
  for (const image of imagesSimilar.value) {
    imageUrl.value = `images/${image.Id}`;
    urls[image.Id] = await loadImage(imageUrl);
  }
  imageUrls.value = urls;
}

onMounted(fetchSimilar);
watch(() => props.imageId, fetchSimilar);

defineExpose({ fetchSimilar });
</script>

<template>
    <div>
        <h2>Les images les plus similaires sont celles-ci :</h2>
    </div><br>
    <div class="image-gallery">
      <div v-for="image in imagesSimilar" :key="image.Id" class="image-container">
        <h3 class="title">{{"Similarité : " + image.Similarity }}</h3>
        <img :src="imageUrls[image.Id]" :alt="image.Name" :title="image.Name">
      </div>
    </div>


</template>

<style scoped>
.read-the-docs {
  color: #888;
}

img {
  padding: 20px;
  border-radius: 10px;
  max-width: 250px;
  height: 180px;
}
.image-container {
  text-align: center;
  margin-bottom: 10px;
}
.title {
  font-weight: bold;
  color: #333;
}
.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  justify-content: center;
}

</style>
