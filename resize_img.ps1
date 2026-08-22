Add-Type -AssemblyName System.Drawing
$src = 'C:\Users\Administrator\.claude\uploads\1e561e19-2290-4b6e-b0d8-45798c38ab20\4079fdc7-a260-4531-851e-51d4aa2fd66f-pasted-image-1787401372762.png'
$img = [System.Drawing.Image]::FromFile($src)
$ratio = [Math]::Min(1600.0 / $img.Width, 1600.0 / $img.Height)
$w = [int]($img.Width * $ratio)
$h = [int]($img.Height * $ratio)
$bmp = New-Object System.Drawing.Bitmap($w, $h)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.DrawImage($img, 0, 0, $w, $h)
$out = 'C:\Users\Administrator\Desktop\二手平台\shot_small.png'
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose(); $img.Dispose()
Write-Output "saved $out ($w x $h)"
