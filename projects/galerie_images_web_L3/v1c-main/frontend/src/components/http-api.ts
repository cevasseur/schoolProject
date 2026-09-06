import { ref, type Ref } from 'vue';
import axios, { type AxiosResponse } from 'axios';
import type { PlayerInfo } from './types';


export function loadImage(imageUrl: Ref<string>): Promise<string> {
    return new Promise((resolve, reject) => {
        axios.get(imageUrl.value, { responseType: "blob" })
            .then(function (response: AxiosResponse) {
                const reader = new window.FileReader();
                reader.readAsDataURL(response.data);
                reader.onload = function () {
                    resolve(reader.result as string);
                };
                reader.onerror = (error) => {
                    reject(error);
                };
            });
    });
}

export async function loadAllImages(images: Ref<{ Id: number; Name: string; Type: string; Size: String }[]>) {
    {
        try {
            const response = await axios.get('/images');
            images.value = response.data || [];
            return response.data || [];
        } catch (error) {
            console.error('Error fetching images:', error);
        }
    };
}

export async function loadGallery(images: Ref<{ Id: number; Name: string; Type: string; Size: String }[]>) {
    try {
        const gallery: { id: number; src: string }[] = [];
        for (const image of images.value) {
            const imageUrl = ref<string>( `/images/${image.Id}`);
            gallery.push({id: image.Id, src:await Promise.resolve(loadImage(imageUrl))});
        }
        return gallery;
    }
    catch (err) {
        console.error("The gallery couldn't be loaded because of : ", err);
        return [];
    }
}

export async function loadSimilar(imageId: number,N: number, descriptor: string, images : Ref<{ Id: number; Name: string; Type: string; Size: String, Similarity: number }[]>) {
    try {
        console.log(`Loading similar images for ID: ${imageId}`);
        if (imageId < 0) {
            console.error("ID invalid: ", imageId);
        }
        const params = { number: N, descriptor: descriptor};
        const response = await axios.get(`/images/${imageId}/similar?number=${params.number}&descriptor=${params.descriptor}`);
        images.value = response.data;
        return response.data || [];
    }
    catch (err) {
        console.error("The list of similar images couldn't be loaded because of : ", err);
        return [];
    }
}

export function importFile(file: File) {
    if (file != null) {
        let formData = new FormData();
        formData.append('file', file);
        axios.post('/images', formData,
            {
                headers: {
                    'Content-Type': 'multipart/form-data'
                },
            }
        ).then(function () {
            console.log('SUCCESSFULLY IMPORTED!!');
            window.location.reload();
        })
            .catch(function () {
                console.log('FAILURE DURING IMPORT!!');
            });
        
    }
}

export function deleteFile(id : number) {
    try {
        return axios.delete(`/images/${id}`);
    } catch (error) {
        console.error('Une erreur s\'est produite lors de la suppression du fichier :', error);
    }
}

export async function downloadImage(imageId: number, imageName: string) {
    try {
        const response = await axios.get(`/images/${imageId}`, { responseType: 'blob' });
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', imageName);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    } catch (error) {
        console.error("Erreur lors du téléchargement de l'image :", error);
    }
}

export async function addScore(name: string, score: number, time: number) {
    let formData = new FormData();
    formData.append('name', name);
    formData.append('score', score.toString());
    formData.append('time', time.toFixed(1));

    try {
        await axios.post('/memory/add', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            },
        });
        console.log('SUCCESS');
    } catch (error) {
        console.error('FAILURE', error);
    }
}

export async function getScores(scores: Ref<{ name: string, score: number, time: number}[]>) {
    try {
        const response = await axios.get('/memory/scores');
        scores.value = response.data || [];
        return response.data || [];
    } catch (error) {
        console.error('Error fetching scores:', error);
    };
}
  
  export async function getConnectedUsers(users: Ref<Record<string, PlayerInfo>>) {
    try {
      const response = await axios.get('/api/ws/users');
      console.log(response.data);
      users.value = response.data || {};
      return response.data || {};
    } catch (error) {
      users.value = {};
      console.error('Error fetching users:', error);
    }
  }

  export async function getHost(host: Ref<String>) {
    try {
      const response = await axios.get('/api/ws/gethost');
      console.log(response.data);
      host.value = response.data || "";
      return response.data || "";
    } catch (error) {
      host.value = "";
      console.error('Error fetching host:', error);
    }
  }

export function loadComment(commUrl: Ref<string>): Promise<string> {
    return new Promise((resolve, reject) => {
        axios.get(commUrl.value, { responseType: "blob" })
            .then(function (response: AxiosResponse) {
                const reader = new window.FileReader();
                reader.readAsDataURL(response.data);
                reader.onload = function () {
                    resolve(reader.result as string);
                };
                reader.onerror = (error) => {
                    reject(error);
                };
            });
    });
}

// Charger les commentaires
export async function loadAllComments(imageId: number, comments: Ref<{ id: number; image_id: number; content: string; created_at: string }[]>) {
    try {
        const response = await axios.get(`/images/${imageId}/comments`);
        console.log("Commentaire :", response.data);
        comments.value = response.data || [];
        console.log("Ajout: ", response.data);
        return response.data || [];
    } catch (error) {
        console.error("Erreur lors du chargement des commentaires :", error);
        return [];
    }
}

// Ajouter un commentaire
export async function addComment(imageId: number, comment: string) {
    try {
        const response = await axios.post(`/images/${imageId}/comments?content=${comment}`);
        return response.data;
    } catch (error) {
        console.error("Erreur lors de l'ajout du commentaire :", error);
    }
}

// Supprimer un commentaire
export async function deleteComment(commentId: number) {
    try {
        await axios.delete(`/comments/${commentId}`);
    } catch (error) {
        console.error("Erreur lors de la suppression du commentaire :", error);
    }
}

export async function uploadCanvas(canvas: HTMLCanvasElement | null, fileName: string, format: "png" | "jpg" | "webp") {
    if(!canvas) return;
    const type = `image/${format}`
    canvas.toBlob(async (Blob) => {
        if(!Blob) return;
        let formData = new FormData();
        formData.append("file", Blob, `${fileName}.${format}`);
        try{
            const response = await axios.post("/images", formData, {
                headers: {"Content-Type": "multipart/form-data"},
            });
            if(response.status === 201){
                window.location.reload();
                console.log(`Dessin enregistré sous ${fileName}.${format} avec succès.`);
            }
            else{
                console.error("Echec lors de l'enregistrement.");
            }
        } catch(error){
            console.error("Erreur lors de l'envoi :", error);
        }
    }, type)
}
