# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# PaddleOCR Lite - PaddleLite native
-keep class com.baidu.paddle.lite.** { *; }

# PaddleOCR Lite - library
-keep class com.equationl.paddleocr4android.** { *; }

# Our OCR engine
-keep class com.labfreezer.data.ocr.** { *; }
