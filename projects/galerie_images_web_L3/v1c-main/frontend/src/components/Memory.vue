<script setup lang="ts">

import { ref, onMounted, reactive, onUnmounted, computed } from 'vue';
import { loadAllImages, loadGallery, addScore, getScores, getConnectedUsers, getHost } from './http-api.ts';
import type { PlayerInfo } from './types.ts';
import dosImage from  '../assets/dos_image.jpg';
import { Client } from '@stomp/stompjs';
import { v4 as uuidv4 } from 'uuid';


const isMultiplayer = ref(false);
const users = ref<Record<string, PlayerInfo>>({});
const userList = computed(() => Object.values(users.value || {}));
const isYourTurn = ref<Boolean>(false);
const hostId = ref<string>("");
const hostName = computed(() => users.value[hostId.value]?.name ?? "");

const images = ref<{ Id: number; Name: string; Type: string; Size: String }[]>([]);
const imagesGallery = ref<{ id: string; src: string; flipped: boolean; fixed: boolean }[]>([]);
const scores = ref<{ name: string; score: number; time: number }[]>([]);
const galleryMapping = new Map<string, string>();

const playerId = ref<string>("");
const name = ref<string>("Anonymous");
const score = ref<number>(0);

let stompClient: Client;

onMounted(() => {
  document.body.classList.add('custom-body')
  fetchGallery();
  fetchScores();
  if (!localStorage.getItem('playerId')) {
    const id = uuidv4();
    localStorage.setItem('playerId', id);
  }
  playerId.value = localStorage.getItem('playerId')!;
  window.addEventListener('beforeunload', handlePageUnload);
});

onUnmounted(() => {
  document.body.classList.remove('custom-body');
  window.removeEventListener('beforeunload', handlePageUnload);
})


function handlePageUnload(event: BeforeUnloadEvent) {
  if (isMultiplayer.value && stompClient && stompClient.active) {
    stompClient.publish({
        destination: "/app/gethost",
        body: "",
      });
    stompClient.deactivate();
  }
  return event;
}

async function tryEnableMultiplayer() {
  await getConnectedUsers(users);
  if (Object.keys(users.value).length >= 2) {
      alert("🚫 Trop de joueurs déjà connectés !");
    return;
  }
  
  if (Object.keys(users.value).length == 0) {
    isYourTurn.value = true;
  }

  enableMultiplayer();
}

function enableMultiplayer() {
  isMultiplayer.value = true;

  stompClient = new Client({
    webSocketFactory: () => new WebSocket('ws://localhost:8181/ws'),

    reconnectDelay: 5000,
    
    onConnect: () => {
      console.log("Connected to WebSocket");
      stompClient.publish({
        destination: '/app/register',
        body: JSON.stringify({ playerId: playerId.value, name: name.value, score: score.value })
      });

      stompClient.subscribe('/topic/grid', message => {
        console.log("Grille reçue");
        const data = JSON.parse(message.body);
        console.log(data);

        imagesGallery.value = data.map((card: { id: string }) => {
          const baseId = card.id.split('-')[0];
          return {
            id: card.id,
            src: galleryMapping.get(baseId) || '', 
            flipped: false,
            fixed: false
          };
        });
        setupGrid();
      });

      stompClient.subscribe('/topic/flipped', message => {
        const data = JSON.parse(message.body);

        if (data.playerId != playerId.value) {
          console.log("Other player flipped:", data);
          const image = imagesGallery.value.find(img => img.id === data.cardId);
          if (image) image.flipped = true;
        }
      });

      stompClient.subscribe('/topic/restart', () => {
        console.log("Game restart signal received");
        restartGame();
      });

      stompClient.subscribe('/topic/gethost', () => {
        getHost(hostId);
      })

      stompClient.subscribe('/topic/match', message => {
        const data = JSON.parse(message.body);
        console.log(data);

        if (data.playerId != playerId.value) {
          const firstImage = imagesGallery.value.find(img => img.id === data.firstId);
          const secondImage = imagesGallery.value.find(img => img.id === data.secondId);
          const isMatched: boolean = data.isMatched;
          if (isMatched) {
            totalImagesWon.value += 2;
            setTimeout(() => {
              checkWin();
            }, 100);
          } else {
            if (firstImage) firstImage.flipped = false;
            if (secondImage) secondImage.flipped = false;
            imagesGallery.value = [...imagesGallery.value];
            isYourTurn.value = true;
          }
        }
      });

      stompClient.subscribe('/topic/getusers', () => {
        console.log("Updating users list");

        getConnectedUsers(users).then(() => {
          if (hostId.value === playerId.value && !isStarted.value) {
            console.log("Giving my grid away");
            console.log(imagesGallery.value);
            stompClient.publish({
              destination: "/app/generateGrid",
              body: JSON.stringify(imagesGallery.value.map(img => ({
                id: img.id
              })))
            });
          }
        });
      });

      stompClient.publish({
        destination: "/app/getusers",
        body: "",
      });

      stompClient.publish({
        destination: "/app/gethost",
        body: "",
      });

      if (Object.keys(users.value).length == 0) {
        stompClient.publish({
            destination: "/app/sethost",
            body: "",
          });
      }

    }
  });
  stompClient.activate();
}


async function fetchGallery() {
  await loadAllImages(images);
  var gallery = await loadGallery(images);

  gallery.forEach((image: { id: number; src: string }) => {
    galleryMapping.set(String(image.id), image.src);
  });

  if (difficulty.value === "Easy") {
    gallery = gallery.slice(0, 2);
  } else if (difficulty.value === "Normal") {
    gallery = gallery.slice(0, 8);
  } else if (difficulty.value === "Hard") {
    gallery = gallery.slice(0, 18);
  }

  const duplicatedGallery = gallery.flatMap(image => ([
    { id: `${image.id}-1`, src: image.src, flipped: false, fixed: false },
    { id: `${image.id}-2`, src: image.src, flipped: false, fixed: false }
  ]));

  let shuffled = shuffleArray(duplicatedGallery);


  imagesGallery.value = shuffled;

  setupGrid();
}

function shuffleArray(array: any[]) {
  return array.sort(() => Math.random() - 0.5);
}

async function fetchScores() {
  await getScores(scores);
}

const grid = ref<HTMLDivElement | null>(null);
const imageSize = ref<number>(0);
const isStarted = ref<boolean>(false);

function setupGrid() {
  const totalImages = imagesGallery.value.length;
  const columns = Math.min(6,Math.ceil(Math.sqrt(totalImages))); // Max 6 colonnes

  const minSize = 128;
  const maxSize = 256;
  var size = minSize;
  if (columns < 6) {
      size = minSize + ((maxSize - minSize) / 5) * (6 - columns);
  }
  imageSize.value = size;

  if (grid.value) {
      grid.value.style.gridTemplateColumns = `repeat(${columns}, 1fr)`;
  }
}

var nbrImageFlipped = 0;
const imagesFlipped = ref<{ id: string }[]>([]);
const totalImagesWon = ref(0);
const gameWon = ref(false);
const youWon = ref(true);
const tie = ref(false);


var isFlipping = false;
function flipImage(id: string) {
  if (isFlipping || !isStarted.value) {
    return;
  }
  if (isMultiplayer.value && !isYourTurn.value) {
    alert("It's not your turn to play");
    return;
  }

  isFlipping = true;
  const image = imagesGallery.value.find(img => img.id === id);

  if (stompClient && isMultiplayer.value) {
    stompClient.publish({
      destination: "/app/flip",
      body: JSON.stringify({ cardId: id, playerId: playerId.value })
    });
  }


  if (image && !image.flipped) {
      image.flipped = true;
      imagesFlipped.value.push({ id });
      nbrImageFlipped += 1;
  }

  // Check when two images are flipped
  if (nbrImageFlipped === 2) {
      setTimeout(() => {checkMatch();isFlipping = false;}, 800);
  } else {
    isFlipping = false;
  }
}

function checkMatch() {
  if (imagesFlipped.value.length === 2) {
      const [first, second] = imagesFlipped.value;
      var isMatched = false;
      // Extract ID without the unique "-1" or "-2" suffix
      const firstId = first.id.toString().split('-')[0];
      const secondId = second.id.toString().split('-')[0];

      if (firstId === secondId) {
        isMatched = true;
        totalImagesWon.value += 2;
        score.value += 2;
        if (isMultiplayer.value) {
          stompClient.publish({
          destination: "/app/updateScores",
          body: score.value.toString()
        });
        }

      } else {
          imagesGallery.value.forEach(image => {
              if (image.id === first.id || image.id === second.id) {
                  image.flipped = false;
              }
          });
          isYourTurn.value = false;
      }
      if (isMultiplayer.value) {
          stompClient.publish({
            destination: '/app/match',
            body: JSON.stringify({ playerId: playerId.value, firstId: first.id, secondId: second.id, isMatched: isMatched })
          });
        }

      // Reset flipped images
      imagesFlipped.value = [];
      nbrImageFlipped = 0;
      
      setTimeout(() => {
        checkWin();
      }, 100);
  }
}

function restartGame() {
  console.log(users.value);
  console.log(Object.keys(users.value).length);
  score.value = 0;
  if (isMultiplayer.value && Object.keys(users.value).length < 2) {
    alert("Waiting for another player ...");
    return;
  }

  if (isMultiplayer.value && !isStarted.value) {
    stompClient.publish({ destination: "/app/restart", body: "restart" });
    stompClient.publish({
        destination: "/app/updateScores",
        body: '0'
    });
  }
  
  totalImagesWon.value = 0;
  gameWon.value = false;
  imagesGallery.value.forEach(image => {
      image.flipped = false;
  });
  if (users.value[hostId.value]?.playerId === playerId.value && isMultiplayer.value) {
    console.log("I shuffle");
    setTimeout(() => {
      imagesGallery.value = shuffleArray(imagesGallery.value);
      stompClient.publish({
      destination: "/app/generateGrid",
      body: JSON.stringify(imagesGallery.value.map(img => ({
        id: img.id
      })))
      });
    }, 300);
  }
  if (!isMultiplayer.value) {
    setTimeout(() => {
      imagesGallery.value = shuffleArray(imagesGallery.value);
    },300);
  }
  imagesFlipped.value = [];
  nbrImageFlipped = 0;
  actualTime.hour = actualTime.minute = actualTime.second = 0;
  updateTimer();
}

const actualTime = reactive({
  hour: 0,
  minute: 0,
  second: 0
});

function updateTimer() {
  isStarted.value = true;
  let timerInterval = setInterval(() => {
    if (gameWon.value) {
      clearInterval(timerInterval);
      saveScore();
      isStarted.value = false;
      return;
    }
    actualTime.second += 0.1;
    if (actualTime.second >= 60) {
      actualTime.second = 0;
      actualTime.minute += 1;
      if (actualTime.minute >= 60) {
        actualTime.minute = 0;
        actualTime.hour += 1;
      }
    }
  }, 100);

}

async function saveScore() {
  if (!isMultiplayer.value) {  
    const time: number = actualTime.hour*3600 + actualTime.minute*60 + actualTime.second;
    await addScore(name.value,totalImagesWon.value,time);
    await fetchScores();
  }

}

function resetgameWon() {
  gameWon.value = false;
}

function exitMultiplayer() {
  isMultiplayer.value = false;
  gameWon.value = false;
  isStarted.value = false;
  imagesGallery.value.forEach(image => {
    image.flipped = false;
  });
  if (stompClient && stompClient.active) {
    stompClient.deactivate();
  }
}

function checkWin() {
  if (totalImagesWon.value == imagesGallery.value.length) {
    gameWon.value = true;
    if (isMultiplayer.value) {
      const playerScores = Object.values(users.value);
      playerScores.sort((a,b) => b.score - a.score);
      if (playerScores[0].score == playerScores[1].score) {
        tie.value = true;
      }
      else {
        if (playerScores[0].score === score.value) {
          youWon.value = true;
        }
        else {
          youWon.value = false;
        }
      }

    }

  }
}

function formatTime(seconds: number) {
  const totalSeconds = Math.floor(seconds);
  const fractional = Math.floor((seconds % 1) * 10); // pour les dixièmes

  const hrs = Math.floor(totalSeconds / 3600);
  const mins = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;

  const paddedHrs = hrs.toString().padStart(2, '0');
  const paddedMins = mins.toString().padStart(2, '0');
  const paddedSecs = secs.toString().padStart(2, '0');

  return `${paddedHrs}:${paddedMins}:${paddedSecs}.${fractional}`;
}

const difficulty = ref<string>("");
function chooseDifficulty(selectedDifficulty: string) {
  difficulty.value = selectedDifficulty;
  fetchGallery();
}
</script>


<template>

  
<br>
<br>
<div v-if="!isStarted" class="blockContainer">
  <input type="button" class="button" value="Easy" @click="chooseDifficulty('Easy')">
  <input type="button" class="button" value="Normal" @click="chooseDifficulty('Normal')">
  <input type="button" class="button" value="Hard" @click="chooseDifficulty('Hard')">
  <input type="button" class="button" value="Infinite" @click="chooseDifficulty('Infinite')">
</div>

<div class="popup" v-if="gameWon">
  <div class="popup-content" v-if="tie">
    <h2>It's a tie 🎉</h2>
    <p>Nobody won the game!</p>
    <button @click="restartGame">Play Again</button>
    <button @click="resetgameWon">Close</button>
  </div>
  <div class="popup-content" v-if="youWon && !tie">
    <h2>Congratulations! 🎉</h2>
    <p>You won the game!</p>
    <button @click="restartGame">Play Again</button>
    <button @click="resetgameWon">Close</button>
  </div>
  <div class="popup-content" v-if="!youWon && !tie">
    <h2>Bouhhhhhh! 🎉</h2>
    <p>You lost the game!</p>
    <button @click="restartGame">Play Again</button>
    <button @click="resetgameWon">Close</button>
  </div>
</div>
<div v-if="!isStarted">
  <input v-if="!isMultiplayer" v-model="name" type="text" placeholder="Enter your name">
  <input type="button" class="button" @click="restartGame" value="Start">
  <input v-if="!isMultiplayer" type="button" class="button" @click="tryEnableMultiplayer" value="Multiplayer 1VS1">
  <input v-if="isMultiplayer" type="button" class="button" @click="exitMultiplayer" value="Exit Multiplayer">
</div>

<div v-if="!isMultiplayer">
  <h2>Timer : 
    {{(actualTime.hour > 0 ? actualTime.hour + ":" : '') + ((actualTime.minute > 0 || actualTime.hour > 0) ? String(actualTime.minute).padStart(2, '0') + ':' : '') + actualTime.second.toFixed(1).padStart(4,'0')}} </h2>
</div>
<div v-if="isMultiplayer  && userList.length >= 2">
  <div class="flexContainer">
    <h2>{{ "Host : " + hostName}}</h2>
    <h2 class="dontMerge">|</h2>
    <h2 v-if="isYourTurn">It's your turn</h2>
    <h2 v-if="!isYourTurn">It's their turn</h2>
  </div>
  
  <h2>{{ userList[0].name + " : score = " + userList[0].score }}</h2>
  <h2>{{ userList[1].name + " : score = " + userList[1].score }}</h2>
</div>

<div class="memory-grid" ref="grid">
  <div
      v-for="image in imagesGallery"
      :key="image.id"
      :class="{ flipped: image.flipped }"
      :style="{ width: imageSize + 'px', height: imageSize + 'px' }"
      class="memory-card"
      @click="flipImage(image.id)"
    >
      <img :src="dosImage" class="front" draggable="false" />
      <img :src="image.src" class="back" draggable="false" />
  </div>
</div>
<br>
<br>
<div>
    <h2>Leaderboard</h2>
    <table class="leaderboard-table">
      <thead>
        <tr>
          <th>Nom</th>
          <th>Score</th>
          <th>Temps</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="score in scores" :key="score.name +'-'+ score.time">
          <td>{{ score.name }}</td>
          <td>{{ score.score }}</td>
          <td>{{ formatTime(score.time) }}</td>
        </tr>
      </tbody>
    </table>
  </div>




</template>
<style>
body {
  background: dodgerblue linear-gradient(22deg, aqua, dodgerblue, deeppink) fixed;
}
</style>
<style scoped>

.blockContainer {
  display: block;
}

.flexContainer {
  display: flex;
  align-items: center;
  gap: 1rem;
  justify-content: center;
}

input {
  font: inherit;
  padding: 0.2em 0.5em;
}

img {
  padding: 20px;
  border-radius: 30px;
  transition: transform 0.25s;
}

img:hover {
  transform: scale(1.2);
  cursor: pointer;
}

.memory-grid {
    display: grid;
    gap: 20px;
    justify-content: center;
    padding: 20px;
}

.memory-card {
    position: relative;
    object-fit: cover;
    perspective: 1000px; /* Enables 3D effect */
    cursor: pointer;
    height: 100%;
    margin: 10px;
    transform-style: preserve-3d;
}

.memory-card img {
    object-fit: cover;
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
    border-radius: 30px;
    transition: transform 0.3s ease-in-out;
    backface-visibility: hidden;
}

.memory-card:not(.flipped):hover .front {
  transform: rotateY(0deg) scale(1.2);
}
.memory-card:not(.flipped):hover .back {
  transform: rotateY(180deg) scale(1);
}

.memory-card.flipped:hover .back {
  transform: rotateY(0deg) scale(1.2);
}
.memory-card.flipped:hover .front {
  transform: rotateY(-180deg) scale(1);
}

.memory-card .front {
    transform: rotateY(0deg);
}

.memory-card .back {
    transform: rotateY(180deg);
}

.memory-card.flipped .front {
    transform: rotateY(-180deg);
}

.memory-card.flipped .back {
    transform: rotateY(0deg);
}

.popup {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: rgba(0, 0, 0, 0.8);
    padding: 20px;
    border-radius: 10px;
    text-align: center;
    color: white;
    z-index: 1000;
}

.popup-content {
    background: white;
    padding: 20px;
    border-radius: 10px;
    color: black;
}

.popup button {
    background: #4CAF50;
    color: white;
    border: none;
    padding: 10px 20px;
    margin: 10px;
    cursor: pointer;
    border-radius: 5px;
}

.popup button:hover {
    background: #45a049;
}

.leaderboard-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
  border: 2px solid #444;
  font-family: Arial, sans-serif;
  background: hsl(232, 22%, 90%);
  border-radius: 20px;
}

.leaderboard-table th,
.leaderboard-table td {
  border: 1px solid #444;
  padding: 0.75rem;
  text-align: center;
  color: #444;
}

.leaderboard-table th {
  font-weight: bold;
}

.leaderboard-table td {
  opacity: 0.8;
}

.button {
  display: inline-block;
  height: auto;
  background-color: #007bff;
  border-color: #007bff;
  color: white;
  padding: 10px 20px;
  margin: 5px;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.3s ease-in-out;
  font-size: 32px;
  line-height: 1;
  text-align: center;
  box-sizing: border-box;
}

.button:hover {
  background-color: #0056b3;
}


</style>