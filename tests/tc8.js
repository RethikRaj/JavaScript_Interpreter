// Strings

let text = "   JavaScript is Awesome! JavaScript is Popular.   ";

console.log("Original:", text);

// trim()
let trimmed = text.trim();
console.log("trim():", trimmed);

// toUpperCase()
console.log("toUpperCase():", trimmed.toUpperCase());

// toLowerCase()
console.log("toLowerCase():", trimmed.toLowerCase());

// replace()
console.log(
  "replace():",
  trimmed.replace("Awesome", "Powerful")
);

// replaceAll()
console.log(
  "replaceAll():",
  trimmed.replaceAll("JavaScript", "JS")
);

// substring(start, end)
console.log(
  "substring(0, 10):",
  trimmed.substring(0, 10)
);

// slice(start, end)
console.log(
  "slice(11, 21):",
  trimmed.slice(11, 21)
);

// split()
console.log(
  "split(' '):",
  trimmed.split(" ")
);

// includes()
console.log(
  "includes('Awesome'):",
  trimmed.includes("Awesome")
);

// startsWith()
console.log(
  "startsWith('JavaScript'):",
  trimmed.startsWith("JavaScript")
);

// endsWith()
console.log(
  "endsWith('Popular.'):",
  trimmed.endsWith("Popular.")
);

// indexOf()
console.log(
  "indexOf('Awesome'):",
  trimmed.indexOf("Awesome")
);