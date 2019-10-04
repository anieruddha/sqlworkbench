function Get-RedirectedUrl
{
    Param (
        [Parameter(Mandatory=$true)]
        [String]$URL
    )

    $request = [System.Net.WebRequest]::Create($url)
    $request.AllowAutoRedirect=$false
    $response=$request.GetResponse()

    If ($response.StatusCode -eq "Found")
    {
        $response.GetResponseHeader("Location")
    }
}

$url= "https://api.adoptopenjdk.net/v2/binary/releases/openjdk12?openjdk_impl=hotspot&os=windows&arch=x64&release=latest&type=jre"

$fUrl = Get-RedirectedUrl $url
$filename = [System.IO.Path]::GetFileName($fUrl); 

Write-Host "Downloading $filename (approx. 40MB)"

[Net.ServicePointManager]::SecurityProtocol = "tls12, tls11, tls"
Invoke-WebRequest -Uri $url -OutFile $filename

# Download sha checksum file as well
$checksumFile = $filename + ".sha256.txt"
$checksumURL = $fUrl + ".sha256.txt"
# Write-Host "Checksum file is: " $checksumURL

Invoke-WebRequest -Uri $checksumURL -OutFile $checksumFile

Write-Host "Extracting JDK to $PSScriptRoot"
Expand-Archive $filename -DestinationPath $PSScriptRoot
