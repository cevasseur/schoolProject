<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { loadAllImages, loadImage, importFile, deleteFile, downloadImage } from './http-api.ts';
import Gallery from './Gallery.vue';
import Similar from './Similar.vue';
defineProps<{ msg: string }>();

const images = ref<{ Id: number; Name: string; Type: string; Size: String}[]>([])

onMounted(async () => {
  await loadAllImages(images);
});

const imageSelected = ref(-1);
const imageUrl = ref<string>("");
const imageSrc = ref<string>("");
const showSimilarImages = ref<boolean>(false);
const NSimilarImages = ref<number>(0);
const descriptor = ref<string>("");
const reloadSimilar = ref(0);

async function reloadImage() {
  if (imageSelected.value != -1) {
    imageUrl.value = `/images/${imageSelected.value}`;  
    imageSrc.value = await loadImage(imageUrl);
  }

}
const file = ref<File | null>(null);
function handleFileUpload(event : Event) {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    file.value = target.files[0];
    if (file.value) {
      importFile(file.value);
    } else {
      console.log("No file selected");
    }
  }
}

async function handleFileDelete() {
  await deleteFile(imageSelected.value);
  imageSelected.value = -1;
  imageUrl.value = "";
  imageSrc.value = "";
  triggerFetchGallery();
  await loadAllImages(images);

}

function handleDownload() {
    if (imageSelected.value !== -1) {
        const selectedImage = images.value.find(img => img.Id === imageSelected.value);
        if (selectedImage) {
            downloadImage(selectedImage.Id, selectedImage.Name);
        }
    }
}

// Reference to the child component (Gallery.vue)
const galleryRef = ref<InstanceType<typeof Gallery> | null>(null);

// Trigger fetchGallery from the parent
function triggerFetchGallery() {
  if (galleryRef.value) {
    galleryRef.value.fetchGallery();
  }
}

function toggleSimilar() {
  if (!showSimilarImages.value) {
    showSimilarImages.value = true;
  }
  reloadSimilar.value += 1;
  
}

watch(() => imageSelected.value, reloadImage);
const hide = ref(true);

function toggle() {
  hide.value = !hide.value;
}
</script>

<template>

  <h1>{{ msg }}</h1>
  <h1>Single File</h1>

  <select class="custom-select" v-model="imageSelected" @change="reloadImage">
    <option :value="-1" disabled>Sélectionner une image</option>
    <option  v-for="image in images" :key="image.Id" :value="image.Id">
      {{ image.Name }}
    </option>
  </select>
  <br>
  <br>
  <div class="image-container">
    <img v-if="imageSrc" :src="imageSrc" width="300">
    <div v-if="imageSrc" class="metadonnees-button">
      <input @click="toggle" type="button" id="metadonnees" class="hide">
        <label for="metadonnees" class="button"><div v-if="hide">Afficher les métadonnées</div><div v-else>Cacher les métadonnées</div></label>
      </input>
      <p v-if="!hide">
        Identifiant de l'image : {{ imageSelected }} <br>
        Nom de l'image : {{ images.find(img => img.Id === imageSelected)?.Name || 'Non trouvé' }} <br>
        Type de l'image : {{ images.find(img => img.Id === imageSelected)?.Type || 'Non trouvé' }} <br>
        Taille de l'image : {{ images.find(img => img.Id === imageSelected)?.Size || 'Non trouvé' }}
      </p>
    </div>
  </div>

  <div class="container">
    <div>
      <input type="file" id="fileUpload" @change="handleFileUpload($event)" />
      <label for="fileUpload" class="button">Choisir un fichier à importer</label>
      <br>
      <input v-if="imageSelected !== -1" type="button" class="button" value="Appuyer ici pour supprimer l'image sélectionnée" @click="handleFileDelete">
      <br>
      <input v-if="imageSelected !== -1" type="button" class="button" value="Appuyer ici pour télécharger l'image sélectionnée" @click="handleDownload">
      <br>
      <br>
      <div class="similarContainer">
        <p for="numImages">Cliquez sur le bouton ci-dessous pour obtenir les </p>
        <input type="number" class="styled-input" v-model="NSimilarImages" id="numImages" min="1" max="100" required>
        
        <p for="descriptor">  images les plus similaires en utilisant le descripteur </p>
        <select id="descriptor" class="custom-select" v-model="descriptor" required>
          <option value="histo_2D_T/S">histo_2D_T/S</option>
          <option value="histo_3D_RGB">histo_3D_RGB</option>
        </select>
        <br>
        <br>
      </div>
        <input type="button" class="button" @click="toggleSimilar" value="Obtenir les images similaires">
        <Similar v-if="imageSelected >= 0 && showSimilarImages" :key="reloadSimilar" :imageId="imageSelected" :N="NSimilarImages" :descriptor=descriptor ref="similarRef" />
    </div>
  </div>

  

  <Gallery ref="galleryRef" v-model:imageSelected="imageSelected" />

</template>

<style scoped>
.read-the-docs {
  color: #888;
}

.similarContainer{
  display: flex;
  margin-bottom: 10px;
}
.similarContainer p{
  padding:5px;
  color: white;
  font-weight: bold;
}

.custom-select {
  padding: 10px;
  background-color: #007bff;
  border-radius: 5px;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease-in-out;
  outline: none;
}

.custom-select:focus {
  border-color: #0056b3;
  box-shadow: 0 0 5px rgba(116, 210, 250, 0.5);
}


.custom-select:hover {
  background-color: #0056b3;
  border-color: #0056b3;
}

.custom-select option {
  background-color: white;
  color: #333;
  font-size: 16px;
}

.custom-select option:disabled {
  color: #888;
  font-style: italic;
}


img {
  padding: 20px;
  max-height: 320px;
  max-width: 600px;
}

.container {
  display: flex;
  justify-content: center;
  align-items: center;
}

input[type="file"] {
  display: none;
}

.button {
  display: inline-block;
  height: auto;
  background-color: #007bff;
  color: white;
  padding: 10px 20px;
  margin: 5px;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.3s ease-in-out;
  font-size: 16px;
  line-height: 1;
  text-align: center;
  box-sizing: border-box;
  border-color: white;
}

.button:hover {
  background-color: #0056b3;
}

label.upload-label {
  display: inline-block;
  background-color: #007bff;
  color: white;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.3s ease-in-out;
  height: 40px;
  line-height: 1;
  text-align: center;
  box-sizing: border-box;
  border-width: 2px;
  border-style: outset;
  border-color: white;
}

label.upload-label:hover {
  background-color: #0056b3;
}

label {
  font-weight: bold;
}



.styled-input {
  padding: 10px;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
  border: 2px solid #ccc;
  border-radius: 8px;
  width: 100%;
  max-width: 40px;
  background-color: #f9f9f9;
  color: #333;
  transition: all 0.3s ease;
}


.image-container {
  position: relative;
  display: inline-block;
}

.hide {
  display: none;
}
</style>
