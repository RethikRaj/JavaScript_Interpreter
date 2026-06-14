// Arrays


const arr = [10, 20, 30];

arr.push(40, 50);
console.log(arr);

arr.pop();
console.log(arr);

arr.shift();
console.log(arr);

arr.unshift(1, 2);
console.log(arr);

arr.splice(2, 2, 10, 290);
console.log("Arr : " + arr);

const ans = arr.slice(2, 4);
console.log(ans);

const ans2 = arr.concat(ans);
console.log(ans2);

console.log(arr.includes(20));

arr.sort();
console.log(arr);

arr.sort((a,b)=> a-b);
console.log(arr);

arr.reverse();
console.log(arr);

console.log(arr.indexOf(10));