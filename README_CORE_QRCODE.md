# Zxing QRCode
ZXing ("zebra crossing") is a great open-source project that support multi-format 1D/2D barcode scan.
But we only need QR Code, so remove the support of other code formats in the folder zxing-core-qrcode.
For which we can compress the jar package from 550k to 150k

## android
android folder is the an Android demo of zxing, remove the support of other code formats except QRCode, and change the screenOrientation from landscope to portrait.
