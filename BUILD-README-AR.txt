Mos Client - Minecraft Java 1.21.11 (Fabric)

1) تأكد أن Gradle موجود في المسار:
C:\Users\Moham\Downloads\gradle-8.14.7-bin\gradle-8.14.7\bin\gradle.bat

2) اضغط مرتين على BUILD-MOS-CLIENT.bat.
3) أول بناء يحتاج اتصال إنترنت لتنزيل مكتبات Fabric وMinecraft.
4) بعد ظهور BUILD SUCCESSFUL سيُفتح مجلد build\libs وفيه ملف JAR.

تم ضبط معرف Fabric Loom إلى net.fabricmc.fabric-loom-remap المناسب لإصدارات Minecraft 1.21.11 وما قبلها، ونسخ ملف MosMenuScreen.java الذي أرسلته إلى المشروع.

ملاحظة: هذه حزمة مصدر معدّلة؛ لم يتم بناء JAR في بيئة التجهيز لأن الاتصال بخوادم تنزيل الاعتماديات غير متاح هنا.
