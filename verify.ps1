
Write-Host "Verifying Railway Backend Deployment..."
$backendUrl = "https://nova-nest-backend-production.up.railway.app"

Write-Host "A. Checking Health..."
try {
    $health = Invoke-RestMethod -Uri "$backendUrl/api/health" -Method Get -TimeoutSec 30
    Write-Host "Health: $health"
} catch {
    Write-Host "Health failed: $($_.Exception.Message)"
}

Write-Host "B. Checking Products..."
try {
    $products = Invoke-RestMethod -Uri "$backendUrl/api/products" -Method Get -TimeoutSec 30
    if ($products.Count -gt 0) {
        Write-Host "Products inserted successfully! Count: $($products.Count)"
    } else {
        Write-Host "Products is STILL empty!"
    }
} catch {
    Write-Host "Products failed: $($_.Exception.Message)"
}

Write-Host "C. Checking Registration..."
$registerBody = @{
    username = "testverifier2"
    email = "verifier2@novanest.com"
    password = "Password123!"
    confirmPassword = "Password123!"
    gender = "Male"
    phone = "1234567890"
    fullName = "Test Verifier Two"
} | ConvertTo-Json
try {
    $regResponse = Invoke-WebRequest -Uri "$backendUrl/api/auth/register" -Method Post -Body $registerBody -ContentType "application/json" -TimeoutSec 30
    Write-Host "Registration successful: $($regResponse.StatusCode)"
} catch {
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Registration failed with status $($_.Exception.Response.StatusCode). Response: $responseBody"
    } else {
        Write-Host "Registration failed: $($_.Exception.Message)"
    }
}

Write-Host "D. Checking Login..."
$loginBody = @{
    email = "verifier2@novanest.com"
    password = "Password123!"
} | ConvertTo-Json
try {
    $loginResponse = Invoke-RestMethod -Uri "$backendUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -TimeoutSec 30
    Write-Host "Login successful! Token received: $(($loginResponse.token).Substring(0, 15))..."
} catch {
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Login failed with status $($_.Exception.Response.StatusCode). Response: $responseBody"
    } else {
        Write-Host "Login failed: $($_.Exception.Message)"
    }
}

