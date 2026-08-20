param(
    [Parameter(Mandatory=$true)][string]$OutFile   # nama file, contoh: test_login.png
)

$figureDir = "C:\Users\acer\Downloads\Latex-TA-IF-ITERA-2026.1\Latex-TA-IF-ITERA-main\Latex-TA-IF-ITERA-2026.1\Latex-TA-IF-ITERA-main\figure"

Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public class WinCapture {
    public struct RECT { public int Left, Top, Right, Bottom; }
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);
    [DllImport("user32.dll")] public static extern int GetWindowTextLength(IntPtr hWnd);
    [DllImport("user32.dll", CharSet = CharSet.Auto)] public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr hwnd, IntPtr hdcBlt, uint nFlags);

    public static IntPtr FindByPartialTitle(string partial) {
        IntPtr found = IntPtr.Zero;
        EnumWindows((hWnd, lParam) => {
            int len = GetWindowTextLength(hWnd);
            if (len == 0 || !IsWindowVisible(hWnd)) return true;
            StringBuilder sb = new StringBuilder(len + 1);
            GetWindowText(hWnd, sb, sb.Capacity);
            if (sb.ToString().IndexOf(partial, StringComparison.OrdinalIgnoreCase) >= 0) {
                found = hWnd; return false;
            }
            return true;
        }, IntPtr.Zero);
        return found;
    }
}
"@

# Judul jendela Android Studio biasanya memuat nama project — sesuaikan kalau berbeda
$hwnd = [WinCapture]::FindByPartialTitle("SKN Inventaris")
if ($hwnd -eq [IntPtr]::Zero) {
    Write-Error "Jendela Android Studio (judul mengandung 'TA-Inventaris') tidak ditemukan. Pastikan project sudah terbuka dan tidak diminimize."
    exit 1
}

[WinCapture]::SetForegroundWindow($hwnd) | Out-Null
Start-Sleep -Milliseconds 400

$rect = New-Object WinCapture+RECT
[WinCapture]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$width = $rect.Right - $rect.Left
$height = $rect.Bottom - $rect.Top

Add-Type -AssemblyName System.Drawing
$bitmap = New-Object System.Drawing.Bitmap $width, $height
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$hdc = $graphics.GetHdc()
[WinCapture]::PrintWindow($hwnd, $hdc, 2) | Out-Null   # 2 = PW_RENDERFULLCONTENT, wajib utk New UI Android Studio
$graphics.ReleaseHdc($hdc)

$outPath = Join-Path $figureDir $OutFile
$bitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose(); $bitmap.Dispose()
Write-Host "Tersimpan: $outPath"
