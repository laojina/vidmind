# Start the middleware VM (headless). Auto-locates the CentOS7 VMX under D:\
$vmx = Get-ChildItem 'D:\' -Recurse -Depth 2 -Filter '*.vmx' -ErrorAction SilentlyContinue |
       Where-Object { $_.FullName -like '*CentOS7*' } | Select-Object -First 1
if (-not $vmx) { Write-Host 'VMX not found under D:\' -ForegroundColor Red; exit 1 }
Write-Host ("Starting VM: " + $vmx.FullName)
& 'D:\VMware\vmrun.exe' -T ws start $vmx.FullName nogui
Write-Host ''
Write-Host 'VM is starting. Middleware (MySQL/Redis/MinIO/Qdrant/RocketMQ) ready in ~3-5 minutes.'
Write-Host 'Then run the backend from IDEA (port 9090).'
