const data = {
    username: "testuser3",
    password: "Test@1234",
    confirmPassword: "Test@1234",
    gender: "Male",
    email: "testuser3@example.com",
    phone: "9876543222",
    fullName: "Test User 3",
    houseNo: "123",
    street: "Main St",
    area: "Downtown",
    city: "Metropolis",
    district: "Central",
    state: "NY",
    country: "USA",
    pincode: "10001"
};

fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify(data)
})
.then(response => response.json().then(data => ({status: response.status, body: data})))
.then(result => console.log('Result:', result))
.catch(error => console.error('Error:', error));
