const products = [
  {
    id: 1,
    name: "iPhone 15",
    category: "Electronics",
    price: 79999,
    brand: "Apple",
    stock: 25,
    rating: 4.8
  },
  {
    id: 2,
    name: "Galaxy S24",
    category: "Electronics",
    price: 74999,
    brand: "Samsung",
    stock: 30,
    rating: 4.7
  },
  {
    id: 3,
    name: "MacBook Air M3",
    category: "Laptops",
    price: 114999,
    brand: "Apple",
    stock: 12,
    rating: 4.9
  },
  {
    id: 4,
    name: "Dell XPS 13",
    category: "Laptops",
    price: 99999,
    brand: "Dell",
    stock: 15,
    rating: 4.6
  },
  {
    id: 5,
    name: "Sony WH-1000XM5",
    category: "Accessories",
    price: 29999,
    brand: "Sony",
    stock: 40,
    rating: 4.8
  },
  {
    id: 6,
    name: "Nike Air Max",
    category: "Footwear",
    price: 8999,
    brand: "Nike",
    stock: 50,
    rating: 4.5
  },
  {
    id: 7,
    name: "Adidas Ultraboost",
    category: "Footwear",
    price: 11999,
    brand: "Adidas",
    stock: 35,
    rating: 4.7
  },
  {
    id: 8,
    name: "Levi's 511 Jeans",
    category: "Clothing",
    price: 2999,
    brand: "Levi's",
    stock: 60,
    rating: 4.4
  },
  {
    id: 9,
    name: "Puma Sports T-Shirt",
    category: "Clothing",
    price: 1499,
    brand: "Puma",
    stock: 80,
    rating: 4.3
  },
  {
    id: 10,
    name: "Canon EOS R50",
    category: "Cameras",
    price: 67999,
    brand: "Canon",
    stock: 10,
    rating: 4.8
  }
];

// const filteredProducts = products.filter((product)=> {return product.price >= 10000});
// const filteredProducts = products.filter((product) => product.price >= 10000);

// console.log(filteredProducts.length);

const arr = [10, 20, 30];

const cb = (num)=> num*2

const doubledArr = arr.map(cb);
console.log(doubledArr);

const sum = arr.reduce((acc, curr)=> acc + curr, 0);
console.log(sum);