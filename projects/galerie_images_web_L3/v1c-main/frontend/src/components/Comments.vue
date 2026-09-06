<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import axios from 'axios';
import { addComment, deleteComment, loadAllComments } from './http-api.ts';

const props = defineProps<{ imageId: number }>();
const comments = ref<{ id: number; image_id: number; content: string; created_at: string }[]>([]);
const newComment = ref('');

const fetchComments = async () => {
  try {
    const response = await axios.get(`/images/${props.imageId}/comments`);
    console.log("Commentaires récupérés :", response.data);
    comments.value = response.data;
  } catch (error) {
    console.error("Erreur lors du chargement des commentaires :", error);
  }
};

async function submitComment() {
  try {
    console.log(newComment.value);
    if (newComment.value.trim()) {
      await addComment(props.imageId, newComment.value);
      newComment.value = "";
      await fetchComments();
    } else {
      alert("Le commentaire ne peut pas être vide !");
      return;
    }
  } catch (error) {
    console.error("Erreur lors de l'ajout du commentaire :", error);
  }
}

async function removeComment(commentId: number) {
  try {
    await deleteComment(commentId);
    fetchComments();
  } catch (error) {
    console.error("Erreur lors de la suppression du commentaire :", error);
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0'); // mois commence à 0
  const year = date.getFullYear();
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${day}/${month}/${year} à ${hours}:${minutes}`;
}

onMounted(fetchComments);
onMounted(async () => {
  console.log("Comments: ", comments.value);
  await loadAllComments(props.imageId, comments);
});

watch(() => props.imageId, () => {
  fetchComments();
});

</script>

<template>
  <div class="comments-section">
    <h3>Commentaires</h3>
    <input v-model="newComment" placeholder="Ajouter un commentaire..." />
    <button @click="submitComment()">Envoyer</button>
    <div v-for="comment in comments" :key="comment.id">
      <p>{{ comment.content }}</p>
      <small>Posté le {{ formatDate(comment.created_at) }}</small>
      <button @click="removeComment(comment.id)">Supprimer</button>      
    </div>
  </div>
</template>

<style scoped>
.comments-section {
  margin-top: 20px;
  padding: 10px;
  border-top: 1px solid #ccc;
}

button {
  margin-left: 10px;
  cursor: pointer;
}
</style>
