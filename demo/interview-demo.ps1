param(
	[string]$ApiBase = "http://localhost:8080",
	[string]$DemoPassword = $env:DEMO_PASSWORD
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($DemoPassword)) {
	throw "Set DEMO_PASSWORD before running the demo."
}

function Get-DemoToken([string]$Email) {
	$body = @{
		email = $Email
		password = $DemoPassword
	} | ConvertTo-Json
	$response = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/v1/auth/login" `
		-ContentType "application/json" -Body $body
	return $response.accessToken
}

function Get-AuthHeaders([string]$Token, [string]$CorrelationId) {
	return @{
		Authorization = "Bearer $Token"
		"X-Correlation-ID" = $CorrelationId
	}
}

$bookingDate = (Get-Date).Date.AddDays(1)
while ($bookingDate.DayOfWeek -in @("Saturday", "Sunday")) {
	$bookingDate = $bookingDate.AddDays(1)
}
$bookingDateText = $bookingDate.ToString("yyyy-MM-dd")

$doctor = (Invoke-RestMethod -Method Get -Uri "$ApiBase/api/v1/doctors")[0]
$slots = Invoke-RestMethod -Method Get `
	-Uri "$ApiBase/api/v1/doctors/$($doctor.id)/available-slots?date=$bookingDateText&durationMinutes=30"
if ($slots.Count -eq 0) {
	throw "No demo slot is available on $bookingDateText."
}
$slot = $slots[0]

$patientToken = Get-DemoToken "patient@demo.local"
$createBody = @{
	doctorId = $doctor.id
	startAt = $slot.startAt
	durationMinutes = 30
	reason = "Recurring headaches for three days"
} | ConvertTo-Json
$appointment = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/v1/me/appointments" `
	-Headers (Get-AuthHeaders $patientToken "interview-create") `
	-ContentType "application/json" -Body $createBody
Write-Host "1. Patient created appointment $($appointment.id): $($appointment.status)"

$doctorToken = Get-DemoToken "doctor@demo.local"
$appointment = Invoke-RestMethod -Method Post `
	-Uri "$ApiBase/api/v1/appointments/$($appointment.id)/confirm" `
	-Headers (Get-AuthHeaders $doctorToken "interview-confirm")
Write-Host "2. Assigned doctor confirmed it: $($appointment.status)"

$adminToken = Get-DemoToken "admin@demo.local"
$adminView = Invoke-RestMethod -Method Get `
	-Uri "$ApiBase/api/v1/appointments/$($appointment.id)" `
	-Headers (Get-AuthHeaders $adminToken "interview-admin-view")
$adminHasReason = $null -ne $adminView.PSObject.Properties["reason"]
Write-Host "3. Admin operational view contains clinical reason: $adminHasReason"

$cancelBody = @{ reason = "Symptoms resolved" } | ConvertTo-Json
$appointment = Invoke-RestMethod -Method Post `
	-Uri "$ApiBase/api/v1/appointments/$($appointment.id)/cancel" `
	-Headers (Get-AuthHeaders $patientToken "interview-cancel") `
	-ContentType "application/json" -Body $cancelBody
Write-Host "4. Patient cancelled it: $($appointment.status)"

$releasedSlots = Invoke-RestMethod -Method Get `
	-Uri "$ApiBase/api/v1/doctors/$($doctor.id)/available-slots?date=$bookingDateText&durationMinutes=30"
$released = @($releasedSlots | Where-Object { $_.startAt -eq $slot.startAt }).Count -eq 1
Write-Host "5. Original slot became available again: $released"
