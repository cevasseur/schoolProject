<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { uploadCanvas, loadAllImages, loadImage } from './http-api.ts';

const canvasRef = ref<HTMLCanvasElement | null>(null);
const drawing = ref(false);
const drawingName = ref("");
const format = ref<"png" | "jpg" | "webp">("jpg");
const context = ref<CanvasRenderingContext2D | null>(null);
const color = ref("black");
const bgColor = ref("white");
const tool = ref<"pen" | "eraser" | "fill">("pen");
const penSize = ref(2);
const eraserSize = ref(10);
const width = ref(500);
const height = ref(500);
const images = ref<{ Id: number; Name: string; Type: string; Size: String}[]>([])
const imageSelected = ref(-1);

const startDrawing = (event: MouseEvent) => {
    if (!canvasRef.value || !context.value) return;
    drawing.value = true;
    context.value.beginPath();
    context.value.moveTo(event.offsetX, event.offsetY);
};

const draw = (event: MouseEvent) => {
    if (!drawing.value || !context.value) return;
    if(tool.value === "fill"){
        const x = event.offsetX;
        const y = event.offsetY;
        floodFill(x, y, color.value);
        tool.value = "pen";
    } else {
        if(tool.value === "eraser"){
            context.value.strokeStyle = bgColor.value;
            context.value.lineWidth = eraserSize.value;
        }
        else{
            context.value.strokeStyle = color.value;
            context.value.lineWidth = penSize.value;        
        }
        context.value.lineTo(event.offsetX, event.offsetY);
        context.value.stroke()
    }
};

const stopDrawing = () => {
    drawing.value = false;
    if (context.value) context.value.closePath();
};

const saveDrawing = () => {
    uploadCanvas(canvasRef.value, drawingName.value, format.value);
}

const clearCanvas = () => {
    if (canvasRef.value && context.value) {
        context.value.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height);
        applyBackgroundColor();
    }
};

const applyBackgroundColor = () => {
    if (canvasRef.value && context.value) {
        context.value.fillStyle = bgColor.value;
        context.value.fillRect(0, 0, canvasRef.value.width, canvasRef.value.height);
    }
};

const floodFill = (x: number, y: number, fillColor: string) => {
    if (!canvasRef.value || !context.value) return;

    const canvas = canvasRef.value;
    const ctx = context.value;
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const data = imageData.data;

    const getColorAtPixel = (x: number, y: number) => {
        const index = (y * canvas.width + x) * 4;
        return {
            r: data[index],
            g: data[index + 1],
            b: data[index + 2],
            a: data[index + 3],
        };
    };

    const setColorAtPixel = (x: number, y: number, color: { r: number; g: number; b: number; a: number }) => {
        const index = (y * canvas.width + x) * 4;
        data[index] = color.r;
        data[index + 1] = color.g;
        data[index + 2] = color.b;
        data[index + 3] = color.a;
    };

    const isSameColor = (c1: any, c2: any) => {
        return c1.r === c2.r && c1.g === c2.g && c1.b === c2.b && c1.a === c2.a;
    };

    const targetColor = getColorAtPixel(x, y);
    const newColor = {
        r: parseInt(fillColor.substring(1, 3), 16),
        g: parseInt(fillColor.substring(3, 5), 16),
        b: parseInt(fillColor.substring(5, 7), 16),
        a: 255, // Opacité 100%
    };

    if (isSameColor(targetColor, newColor)) return;

    const stack = [{ x, y }];
    while (stack.length) {
        const { x, y } = stack.pop()!;
        if (x < 0 || y < 0 || x >= canvas.width || y >= canvas.height) continue;
        if (!isSameColor(getColorAtPixel(x, y), targetColor)) continue;

        setColorAtPixel(x, y, newColor);
        stack.push({ x: x + 1, y });
        stack.push({ x: x - 1, y });
        stack.push({ x, y : y + 1 });
        stack.push({ x, y : y - 1 });
    }

    ctx.putImageData(imageData, 0, 0);
};


const drawImageOnCanvas = async () => {
  if (!canvasRef.value || !context.value || imageSelected.value === -1) return;

  const url = ref(`/images/${imageSelected.value}`);
  const base64Image = await loadImage(url);

  const image = new Image();
  image.src = base64Image;

  image.onload = () => {
    context.value = canvasRef.value!.getContext("2d");
    if (!context.value) return;
    context.value.fillStyle = bgColor.value;
    context.value.clearRect(0, 0, canvasRef.value!.width, canvasRef.value!.height);
    context.value.drawImage(image, 0, 0, canvasRef.value!.width, canvasRef.value!.height);

  };
};



onMounted(async () => {
    if (canvasRef.value) {
        context.value = canvasRef.value.getContext('2d');
        if (context.value) {
            applyBackgroundColor(); 
            context.value.strokeStyle = color.value;
            context.value.lineWidth = penSize.value;
        }
    }
    await loadAllImages(images);
});

watch(bgColor, () => {
    applyBackgroundColor();
});

</script>

<template>
    <div>
        <label>Largeur: <input type="number" v-model.number="width" min="350" max="900" /></label>
        <label>Hauteur: <input type="number" v-model.number="height" min="350" max="900" /></label>
        <label>Couleur du fond: <input type="color" v-model="bgColor" @input="clearCanvas"/></label>
        <label>Couleur du stylo: <input type="color" v-model="color"/></label>
        <label>Taille du stylo: <input type="range" min="1" max="20" v-model="penSize" /></label>
        <label>Taille de la gomme: <input type="range" min="5" max="50" v-model="eraserSize" /></label>
        <button @click="tool = 'pen'" :class="{ active: tool === 'pen' }" title="Stylo">
            <img src="../assets/crayon.svg" alt="Dessin" width="24" height="24">
        </button>
        <button @click="tool = 'eraser'" :class="{ active: tool === 'eraser' }" title="Gomme">
            <img src="../assets/eraser.png" alt="Gomme" width="24" height="24">
        </button>
        <button @click="tool = 'fill'" :class="{ active: tool === 'fill' }" title="Seau">
            <img src="../assets/fill.png" alt="Remplissage" width="24" height="24">
        </button>
        <button @click="clearCanvas">
            <img src="../assets/trash.png" alt="Effacer" width="24" height="24">
        </button>
        <br>
    </div>
    <canvas 
        ref="canvasRef" 
        :width="width > 900 ? 900 :(width < 350 ? 350: width)"  
        :height="height > 900 ? 900 :(height < 350 ? 350: height)" 
        :class="`tool-${tool}`"
        style="border:1px solid black;"
        @mousedown="startDrawing"
        @mousemove="draw"
        @mouseup="stopDrawing"
        @mouseleave="stopDrawing"
    ></canvas>
    <div>
        <label>Nom du fichier: <input v-model="drawingName" type="text"/></label>
        <label>Format:
            <select v-model="format">
                <option value="png">PNG</option>
                <option value="jpg">JPG</option>
                <option value="webp">WEBP</option>
            </select>
        </label>
        <button @click="saveDrawing">
            <img src="../assets/save.png" alt="Save" width="24" height="24">
        </button>
    </div>
    <div>
        <select class="custom-select" v-model="imageSelected" @change="drawImageOnCanvas">
            <option :value="-1" disabled>Sélectionner une image</option>
            <option  v-for="image in images" :key="image.Id" :value="image.Id">
                {{ image.Name }}
            </option>
        </select>
    </div>
</template>

<style scoped>
p {
    font-weight: bold;
    color: white;
    text-shadow: 2px 2px 5px rgba(0, 0, 0, 0.5);
}
button {
    margin: 5px;
    padding: 5px 10px;
    cursor: pointer;
}
label {
    margin-right: 10px;
    color: white;
    font-weight: bold;
}
input[type="range"], input[type="number"] {
    margin-left: 5px;
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

button.active {
  border: 2px solid #007bff;
  background-color: #e6f0ff;
  border-radius: 5px;
}

button:focus {
    outline: none;
}

canvas.tool-pen {
  cursor: crosshair;
}

canvas.tool-eraser {
    cursor: cell;
}

canvas.tool-fill {
  cursor: grab;
}

</style>
