const arr = [10, 20, 30, 40, 50];

const copyArr = [...arr];

const [first, second, ...remaining] = arr;

console.log(arr);
console.log(copyArr);

console.log(first);
console.log(second);
console.log(remaining);