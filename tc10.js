// Math
// Basic Math Operations
console.log("Addition:", 10 + 5);
console.log("Subtraction:", 10 - 5);
console.log("Multiplication:", 10 * 5);
console.log("Division:", 10 / 5);
console.log("Modulus:", 10 % 3);
console.log("Exponent:", 2 ** 3);

// Math Object Methods
console.log("Math.PI:", Math.PI);

console.log("Math.round(4.6):", Math.round(4.6));
console.log("Math.floor(4.9):", Math.floor(4.9));
console.log("Math.ceil(4.1):", Math.ceil(4.1));

console.log("Math.abs(-15):", Math.abs(-15));
console.log("Math.sqrt(25):", Math.sqrt(25));
console.log("Math.pow(2, 4):", Math.pow(2, 4));

console.log("Math.max(10, 20, 30):", Math.max(10, 20, 30));
console.log("Math.min(10, 20, 30):", Math.min(10, 20, 30));

// Random Number Generation
console.log("Random Number (0 to 1):", Math.random());

// Random Integer from 1 to 10
let randomNum = Math.floor(Math.random() * 10) + 1;
console.log("Random Integer (1 to 10):", randomNum);

// Random Integer from 50 to 100
let randomRange = Math.floor(Math.random() * (100 - 50 + 1)) + 50;
console.log("Random Integer (50 to 100):", randomRange);