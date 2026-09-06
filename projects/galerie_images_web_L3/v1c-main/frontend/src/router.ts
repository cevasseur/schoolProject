import { createRouter, createWebHistory } from 'vue-router';
import Memory from './components/Memory.vue';
import HelloWorld from './components/HelloWorld.vue';
import Carousel from './components/Carousel.vue';
import Paint from './components/Paint.vue';

const routes = [
  { path: '/', component: HelloWorld },
  { path: '/memory', component: Memory },
  { path: '/carousel', component: Carousel },
  { path: '/paint', component: Paint}
];

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router;