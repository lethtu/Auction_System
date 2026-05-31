param(
    [string]$InputPath = "docs/code-reading-guide.md",
    [string]$OutputPath = "docs/code-reading-guide.pdf"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = (Get-Location).Path
$inputFull = [System.IO.Path]::GetFullPath((Join-Path $root $InputPath))
$outputFull = [System.IO.Path]::GetFullPath((Join-Path $root $OutputPath))
$outputDir = [System.IO.Path]::GetDirectoryName($outputFull)
[System.IO.Directory]::CreateDirectory($outputDir) | Out-Null

$pageDir = Join-Path $outputDir ".code-reading-guide-pages"
if (Test-Path $pageDir) {
    Remove-Item -LiteralPath $pageDir -Recurse -Force
}
[System.IO.Directory]::CreateDirectory($pageDir) | Out-Null

$pageWidth = 1240
$pageHeight = 1754
$margin = 78
$contentWidth = $pageWidth - ($margin * 2)

$brush = [System.Drawing.Brushes]::Black
$mutedBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(70, 80, 95))
$codeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 34, 45))
$headingBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(12, 26, 42))

function New-DocFont {
    param(
        [string]$Name,
        [float]$Size,
        [System.Drawing.FontStyle]$Style = [System.Drawing.FontStyle]::Regular
    )
    return New-Object System.Drawing.Font($Name, $Size, $Style, [System.Drawing.GraphicsUnit]::Pixel)
}

$fontTitle = New-DocFont "Segoe UI" 42 ([System.Drawing.FontStyle]::Bold)
$fontH2 = New-DocFont "Segoe UI" 31 ([System.Drawing.FontStyle]::Bold)
$fontH3 = New-DocFont "Segoe UI" 24 ([System.Drawing.FontStyle]::Bold)
$fontBody = New-DocFont "Segoe UI" 20 ([System.Drawing.FontStyle]::Regular)
$fontBold = New-DocFont "Segoe UI" 20 ([System.Drawing.FontStyle]::Bold)
$fontCode = New-DocFont "Consolas" 18 ([System.Drawing.FontStyle]::Regular)

function New-Page {
    $bitmap = New-Object System.Drawing.Bitmap($pageWidth, $pageHeight)
    $bitmap.SetResolution(150, 150)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::White)
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    return [PSCustomObject]@{
        Bitmap = $bitmap
        Graphics = $graphics
    }
}

function Measure-TextWidth {
    param($Graphics, [string]$Text, $Font)
    return $Graphics.MeasureString($Text, $Font).Width
}

function Split-LongWord {
    param($Graphics, [string]$Word, $Font, [int]$MaxWidth)
    $pieces = New-Object System.Collections.Generic.List[string]
    $current = ""
    foreach ($ch in $Word.ToCharArray()) {
        $candidate = $current + $ch
        if ($current.Length -gt 0 -and (Measure-TextWidth $Graphics $candidate $Font) -gt $MaxWidth) {
            $pieces.Add($current)
            $current = [string]$ch
        } else {
            $current = $candidate
        }
    }
    if ($current.Length -gt 0) {
        $pieces.Add($current)
    }
    return $pieces.ToArray()
}

function Get-WrappedLines {
    param($Graphics, [string]$Text, $Font, [int]$MaxWidth)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @("")
    }

    $result = New-Object System.Collections.Generic.List[string]
    $words = $Text -split "\s+"
    $line = ""

    foreach ($word in $words) {
        if ((Measure-TextWidth $Graphics $word $Font) -gt $MaxWidth) {
            if ($line.Length -gt 0) {
                $result.Add($line)
                $line = ""
            }
            foreach ($piece in (Split-LongWord $Graphics $word $Font $MaxWidth)) {
                $result.Add($piece)
            }
            continue
        }

        $candidate = if ($line.Length -eq 0) { $word } else { "$line $word" }
        if ((Measure-TextWidth $Graphics $candidate $Font) -le $MaxWidth) {
            $line = $candidate
        } else {
            $result.Add($line)
            $line = $word
        }
    }

    if ($line.Length -gt 0) {
        $result.Add($line)
    }

    return $result.ToArray()
}

function Get-JpegEncoder {
    return [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() |
        Where-Object { $_.MimeType -eq "image/jpeg" } |
        Select-Object -First 1
}

$jpegEncoder = Get-JpegEncoder
$encoderParams = New-Object System.Drawing.Imaging.EncoderParameters(1)
$encoderParams.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter(
    [System.Drawing.Imaging.Encoder]::Quality,
    [int64]92
)

$pages = New-Object System.Collections.Generic.List[string]
$currentPage = New-Page
$y = $margin
$pageNumber = 1

function Save-CurrentPage {
    $path = Join-Path $pageDir ("page-{0:D3}.jpg" -f $script:pageNumber)
    $script:currentPage.Bitmap.Save($path, $script:jpegEncoder, $script:encoderParams)
    $script:currentPage.Graphics.Dispose()
    $script:currentPage.Bitmap.Dispose()
    $script:pages.Add($path)
    $script:pageNumber++
}

function Ensure-Space {
    param([int]$Needed)
    if (($script:y + $Needed) -gt ($script:pageHeight - $script:margin)) {
        Save-CurrentPage
        $script:currentPage = New-Page
        $script:y = $script:margin
    }
}

function Draw-Paragraph {
    param(
        [string]$Text,
        $Font,
        $DrawBrush,
        [int]$Before = 0,
        [int]$After = 8,
        [int]$Indent = 0
    )

    $script:y += $Before
    $maxWidth = $script:contentWidth - $Indent
    $lines = Get-WrappedLines $script:currentPage.Graphics $Text $Font $maxWidth
    $lineHeight = [int][Math]::Ceiling($Font.GetHeight($script:currentPage.Graphics) + 7)

    foreach ($line in $lines) {
        Ensure-Space $lineHeight
        $script:currentPage.Graphics.DrawString(
            $line,
            $Font,
            $DrawBrush,
            [float]($script:margin + $Indent),
            [float]$script:y
        )
        $script:y += $lineHeight
    }
    $script:y += $After
}

$lines = [System.IO.File]::ReadAllLines($inputFull, [System.Text.Encoding]::UTF8)
$inCode = $false

foreach ($rawLine in $lines) {
    $line = $rawLine.TrimEnd()

    if ($line.StartsWith('```')) {
        $inCode = -not $inCode
        $y += 8
        continue
    }

    if ($inCode) {
        if ($line.Length -eq 0) {
            $y += 14
        } else {
            Draw-Paragraph $line $fontCode $codeBrush 0 2 26
        }
        continue
    }

    if ([string]::IsNullOrWhiteSpace($line)) {
        $y += 10
        continue
    }

    if ($line.StartsWith('# ')) {
        Draw-Paragraph $line.Substring(2) $fontTitle $headingBrush 0 22 0
    } elseif ($line.StartsWith('## ')) {
        Draw-Paragraph $line.Substring(3) $fontH2 $headingBrush 26 14 0
    } elseif ($line.StartsWith('### ')) {
        Draw-Paragraph $line.Substring(4) $fontH3 $headingBrush 18 10 0
    } elseif ($line.StartsWith('- ')) {
        Draw-Paragraph ("- " + $line.Substring(2)) $fontBody $brush 0 6 20
    } elseif ($line -match "^\d+\. ") {
        Draw-Paragraph $line $fontBody $brush 0 6 20
    } else {
        Draw-Paragraph $line $fontBody $brush 0 8 0
    }
}

Save-CurrentPage

function Write-Ascii {
    param($Stream, [string]$Text)
    $bytes = [System.Text.Encoding]::ASCII.GetBytes($Text)
    $Stream.Write($bytes, 0, $bytes.Length)
}

function Join-Bytes {
    param([object[]]$Parts)
    $ms = New-Object System.IO.MemoryStream
    foreach ($part in $Parts) {
        if ($part -is [byte[]]) {
            $ms.Write($part, 0, $part.Length)
        } else {
            $bytes = [System.Text.Encoding]::ASCII.GetBytes([string]$part)
            $ms.Write($bytes, 0, $bytes.Length)
        }
    }
    return $ms.ToArray()
}

$pagePointWidth = 595
$pagePointHeight = 842
$objectCount = 2 + ($pages.Count * 3)
$objects = @{}
$kids = New-Object System.Collections.Generic.List[string]

$objects[1] = [System.Text.Encoding]::ASCII.GetBytes("<< /Type /Catalog /Pages 2 0 R >>")

for ($i = 0; $i -lt $pages.Count; $i++) {
    $pageObj = 3 + ($i * 3)
    $contentObj = $pageObj + 1
    $imageObj = $pageObj + 2
    $kids.Add("$pageObj 0 R")

    $content = "q $pagePointWidth 0 0 $pagePointHeight 0 0 cm /Im0 Do Q"
    $contentBytes = [System.Text.Encoding]::ASCII.GetBytes($content)
    $objects[$contentObj] = Join-Bytes @(
        "<< /Length $($contentBytes.Length) >>`nstream`n",
        $contentBytes,
        "`nendstream"
    )

    $jpegBytes = [System.IO.File]::ReadAllBytes($pages[$i])
    $objects[$imageObj] = Join-Bytes @(
        "<< /Type /XObject /Subtype /Image /Width $pageWidth /Height $pageHeight /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length $($jpegBytes.Length) >>`nstream`n",
        $jpegBytes,
        "`nendstream"
    )

    $objects[$pageObj] = [System.Text.Encoding]::ASCII.GetBytes(
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pagePointWidth $pagePointHeight] /Resources << /XObject << /Im0 $imageObj 0 R >> >> /Contents $contentObj 0 R >>"
    )
}

$objects[2] = [System.Text.Encoding]::ASCII.GetBytes(
    "<< /Type /Pages /Kids [$($kids -join ' ')] /Count $($pages.Count) >>"
)

$pdf = New-Object System.IO.MemoryStream
Write-Ascii $pdf "%PDF-1.4`n"
$offsets = New-Object long[] ($objectCount + 1)

for ($id = 1; $id -le $objectCount; $id++) {
    $offsets[$id] = $pdf.Position
    Write-Ascii $pdf "$id 0 obj`n"
    $body = $objects[$id]
    $pdf.Write($body, 0, $body.Length)
    Write-Ascii $pdf "`nendobj`n"
}

$xrefStart = $pdf.Position
Write-Ascii $pdf "xref`n"
Write-Ascii $pdf "0 $($objectCount + 1)`n"
Write-Ascii $pdf "0000000000 65535 f `n"
for ($id = 1; $id -le $objectCount; $id++) {
    Write-Ascii $pdf ("{0:D10} 00000 n `n" -f $offsets[$id])
}
Write-Ascii $pdf "trailer`n"
Write-Ascii $pdf "<< /Size $($objectCount + 1) /Root 1 0 R >>`n"
Write-Ascii $pdf "startxref`n"
Write-Ascii $pdf "$xrefStart`n"
Write-Ascii $pdf "%%EOF`n"

[System.IO.File]::WriteAllBytes($outputFull, $pdf.ToArray())
$pdf.Dispose()

Remove-Item -LiteralPath $pageDir -Recurse -Force
Write-Output $outputFull
