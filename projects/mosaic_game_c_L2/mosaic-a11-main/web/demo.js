Module.onRuntimeInitialized = () => { start(); }

var canvas = document.getElementById('mycanvas');
var body = document.getElementsByTagName('body')[0];


var squareX = 300;
var squareY = 300;

const EMPTY = 0;
const WHITE = 1;
const BLACK = 2;
const UNCONSTRAINED = -1;
const FULL = 0;
const ORTHO = 1;
const FULL_EXCLUDE = 2;
const ORTHO_EXCLUDE = 3;

let col2str = [" ", "□", "■"];
let num2str = [
    ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"],     // empty
    ["🄋", "➀", "➁", "➂", "➃", "➄", "➅", "➆", "➇", "➈"],  // white
    ["⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾"],     // black
];


function square2str(n, c) {
    if (n == UNCONSTRAINED)
        return col2str[c];
    else
        return num2str[c][n];
}

function drawCanvas() {
    ctx = canvas.getContext('2d');
    canvas.width = 300;
    canvas.height = 300;

    width = canvas.width;
    height = canvas.height;

    // clear canvas
    ctx.clearRect(0, 0, width, height);

    // draw some lines
    for (let index = 0; index < 6; index++) {
        ctx.save();
        ctx.strokeStyle = 'violet';
        ctx.moveTo(index*width/5, 0);
        ctx.lineTo(index*width/5, height);
        ctx.moveTo(0, index*height/5);
        ctx.lineTo(width, index*height/5);
        ctx.stroke();
        ctx.restore();
    }
}


async function wonMessage(g) {
    var wonElement = document.getElementById('won');
    if (Module._won(g)) {
        wonElement.classList.add('display');
        animChangeColor(g, Module._nb_rows(g), Module._nb_cols(g));
        await new Promise(resolve => setTimeout(resolve, 700));
        animChangeColor(g, Module._nb_rows(g), Module._nb_cols(g));
    } else {
        wonElement.classList.remove('display');
    }
}


function animChangeColor(g, nb_rows, nb_cols) {
    for (let row = 0; row < nb_rows; row++) {
        for (let col = 0; col < nb_cols; col++) {
            if(Module._get_color(g, row, col) == BLACK){
                Module._play_move(g, row, col, WHITE);
            }else if(Module._get_color(g, row, col) == WHITE){
                Module._play_move(g, row, col, BLACK);
            }
            let color = Module._get_color(g, row, col);
            if (color == WHITE) {
                ctx.fillStyle = 'white';
                ctx.fillRect((col * width / nb_cols)+1, (row * height / nb_rows)+1, (width / nb_cols)-2, (height / nb_rows)-2);
            }else if (color == BLACK) {
                ctx.fillStyle = 'black';
                ctx.fillRect((col * width / nb_cols)+1, (row * height / nb_rows)+1, (width / nb_cols)-2, (height / nb_rows)-2);
            }else{
                ctx.clearRect((col * width / nb_cols)+1, (row * height / nb_rows)+1, (width / nb_cols)-2, (height / nb_rows)-2);
            } let constraint = Module._get_constraint(g, row, col);
            if(constraint!=-1){
                ctx.fillStyle = 'green';
            ctx.font = 'bold 40px mlp';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(constraint, (col * width / nb_cols) + (width / nb_cols / 2), (row * height / nb_rows) + (height / nb_rows / 2));
            }
        }
    }
}

function colorStatus(g, nb_rows, nb_cols) {
    for (let row = 0; row < nb_rows; row++) {
        for (let col = 0; col < nb_cols; col++) {
            let color = Module._get_color(g, row, col);
            if (color == WHITE) {
                ctx.fillStyle = 'white';
                ctx.fillRect((col * width / nb_cols)+1, (row * height / nb_rows)+1, (width / nb_cols)-2, (height / nb_rows)-2);
            }else if (color == BLACK) {
                ctx.fillStyle = 'black';
                ctx.fillRect((col * width / nb_cols)+1, (row * height / nb_rows)+1, (width / nb_cols)-2, (height / nb_rows)-2);
            }else{
                ctx.clearRect((col * width / nb_cols)+1, (row * height / nb_rows)+1, (width / nb_cols)-2, (height / nb_rows)-2);
            } 
            let constraint = Module._get_constraint(g, row, col);
            if(constraint != -1){
                if(Module._get_status(g, row, col) == 0){
                    ctx.fillStyle = 'red';
                }else if(Module._get_status(g, row, col) == 1){
                    ctx.fillStyle = 'lightblue';
                }else{ ctx.fillStyle = 'lightgreen'; }
                ctx.font = 'bold 40px mlp';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText(constraint, (col * width / nb_cols) + (width / nb_cols / 2), (row * height / nb_rows) + (height / nb_rows / 2));
            }
        }
    }
}


function printGame(g) {
    var text = "";
    var nb_rows = Module._nb_rows(g);
    var nb_cols = Module._nb_cols(g);
    for (var row = 0; row < nb_rows; row++) {
        for (var col = 0; col < nb_cols; col++) {
            var n = Module._get_constraint(g, row, col);
            var c = Module._get_color(g, row, col);
            var status = Module._get_status(g, row, col);
            text += square2str(n, c);
        }
        text += "\n";
    }

    // put this text in <div> element with ID 'result'
    var elm = document.getElementById('result');
    elm.innerHTML = text;
}



function changeRectColor(g, row, col) {
    var nb_rows = Module._nb_rows(g);
    var nb_cols = Module._nb_cols(g);
    if (row >= 0 && row < nb_rows && col >= 0 && col < nb_cols) {
        if (Module._get_color(g, row, col) == EMPTY) {
            var next_color = WHITE;
        } else if (Module._get_color(g, row, col) == BLACK) {
            var next_color = EMPTY;
        } else if (Module._get_color(g, row, col) == WHITE) {
            var next_color = BLACK;
        }
        console.log(next_color);
        Module._play_move(g, row, col, next_color);
        console.log("change color of (" + row + ", " + col + "), to color: " + next_color +", with constraint: " + Module._get_constraint(g, row, col));
    } else {
        console.log("Invalid coordinates: (" + row + ", " + col + ")");
    }
    colorStatus(g, Module._nb_rows(g), Module._nb_cols(g));
    wonMessage(g);
}


canvas.addEventListener('click', function(event) {
    if(!Module._won(g)){
        var rect = canvas.getBoundingClientRect();
    var x = event.clientX - rect.left;
    var y = event.clientY - rect.top;
    var row = Math.floor(y / (canvas.height / 5));
    var col = Math.floor(x / (canvas.width / 5));
    changeRectColor(g, row, col);
    }
});

var undoButton = document.getElementById('undo');
    undoButton.addEventListener('click', function() {
    if(!Module._won(g)){
        Module._undo(g);
        colorStatus(g, Module._nb_rows(g), Module._nb_cols(g));
    }
    });


var redoButton = document.getElementById('redo');
    redoButton.addEventListener('click', function() {
        if(!Module._won(g)){
            Module._redo(g);
            colorStatus(g, Module._nb_rows(g), Module._nb_cols(g));
        }
    });

var restartButton = document.getElementById('restart');
restartButton.addEventListener('click', function() {
    Module._restart(g);
    colorStatus(g, Module._nb_rows(g), Module._nb_cols(g));
    wonMessage(g);
});

var solveButton = document.getElementById('solve');
solveButton.addEventListener('click', function() {
    Module._solve(g);
    colorStatus(g, Module._nb_rows(g), Module._nb_cols(g));
    wonMessage(g);
});

var randomButton = document.getElementById('random');
randomButton.addEventListener('click', function() {
    var checkbox=document.getElementById("checkbox");
    if (checkbox.checked){
        g=Module._new_random(5,5,true,FULL);
    }else{
        g=Module._new_random(5,5,false,FULL);
    }
    colorStatus(g, Module._nb_rows(g), Module._nb_cols(g));
    wonMessage(g);
});

function start() {
    console.log("call start routine");
    drawCanvas();
    g = Module._new_default();
    var nb_rows = Module._nb_rows(g);
    var nb_cols = Module._nb_cols(g);
    colorStatus(g, Module._nb_rows(g), Module._nb_cols(g));
    //var g = Module._new_random(6, 6, false, FULL, 0.6, 0.5);
}

