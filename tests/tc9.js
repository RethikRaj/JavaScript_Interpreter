// Date

// Creating a Date object
let now = new Date();

console.log("Current Date & Time:", now);

// Getting date components
console.log("Year:", now.getFullYear());
console.log("Month:", now.getMonth() + 1); // Months are 0-based
console.log("Date:", now.getDate());
console.log("Day:", now.getDay()); // 0 = Sunday
console.log("Hours:", now.getHours());
console.log("Minutes:", now.getMinutes());
console.log("Seconds:", now.getSeconds());

// Creating a specific date
let birthday = new Date("2004-08-15");

console.log("Birthday:", birthday);

// Setting date values
birthday.setFullYear(2025);
console.log("Updated Birthday:", birthday);

// Date to String
console.log("toDateString():", now.toDateString());
console.log("toTimeString():", now.toTimeString());

// Difference between dates
let today = new Date();
let newYear = new Date("2027-01-01");

let diffMs = newYear - today;
let diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

console.log("Days until New Year:", diffDays);