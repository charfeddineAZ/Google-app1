# Workflow Studio 🌐📱

تطبيق أندرويد متكامل ومصمم بأسلوب متطور وأنيق للتحكم في شاشات المتصفحات، تسجيل سيناريوهات التصفح تلقائياً (Raccord Browser)، استخراج محددات عناصر الويب (Selectors)، وحفظ الأكواد والسكريبتات البرمجية لتشغيلها آلياً.

An elegant, high-performance Android application built with Jetpack Compose & Material 3 for workflow automation, browser control, raccord recording, selector capturing, and script execution.

---

## 🚀 كيفية التحميل والتشغيل كـ APK عبر GitHub Actions

لقد تم إعداد سيناريو بناء متكامل وآمن (GitHub Actions Workflow) يقوم ببناء ملف الـ **APK** تلقائياً عند رفع الكود أو تشغيله يدوياً.

### 📥 طريقة البناء والتحميل الفوري:
1. **ارفع الكود إلى مستودع GitHub الخاص بك** (Push to your GitHub Repository).
2. اذهب إلى تبويب **Actions** في صفحة المستودع على GitHub.
3. اختر من القائمة الجانبية اليسرى: **"Build Android APK"**.
4. اضغط على زر **"Run workflow"** في الجهة اليمنى:
   - يمكنك اختيار نوع البناء (Debug للتحميل الفوري والتجربة، أو Release لنسخة الإنتاج).
   - اضغط على **Run workflow** الأخضر لبدء البناء.
5. بعد انتهاء عملية البناء بنجاح (خلال دقائق معدودة):
   - اضغط على تشغيل العمل الأخير (Latest Run).
   - في الأسفل تحت قسم **Artifacts**، ستجد ملف **`android-build-outputs`**.
   - قم بتحميل الملف، فك الضغط عنه، وستجد ملف الـ **`app-debug.apk`** جاهزاً للتثبيت الفوري على هاتفك الذكي!

---

## 🛠️ تهيئة البناء لنسخ الإنتاج (Release APK)

لتوقيع نسخة الإنتاج (Release) بشفرتك الخاصة تلقائياً عبر GitHub Actions، قم بإضافة المتغيرات السرية التالية في إعدادات المستودع (**Settings -> Secrets and variables -> Actions**):

| المفتاح (Secret Key) | الوصف (Description) |
| :--- | :--- |
| `STORE_PASSWORD` | كلمة مرور مخزن المفاتيح (Keystore Store Password). |
| `KEY_PASSWORD` | كلمة مرور المفتاح الخاص (Keystore Key Password). |
| `KEYSTORE_PATH` | (اختياري) مسار ملف الـ Keystore داخل المشروع إذا لم تستخدم الافتراضي. |

---

## ⚙️ تشغيل المشروع محلياً (Local Development)

للتعديل وتطوير التطبيق على جهاز الكمبيوتر الخاص بك باستخدام **Android Studio**:

1. تأكد من تثبيت **JDK 17** على الأقل.
2. افتح المجلد الرئيسي للمشروع في Android Studio.
3. لتشغيل التطبيق أو البناء محلياً عبر سطر الأوامر (Terminal):
   - للبناء التجريبي (Debug APK):
     ```bash
     ./gradlew assembleDebug
     ```
   - لتشغيل الاختبارات:
     ```bash
     ./gradlew test
     ```

---

## ✨ المميزات الرئيسية للمشروع (Key Features)

- **WebView Automation Controls**: التحكم الكامل في حركة المتصفح، تصفح الروابط، التمرير، التقاط صور الشاشة وحفظها كـ PNG.
- **Selector Capturing**: التقاط محددات عناصر الويب (CSS Selectors) تلقائياً من خلال النقر المباشر على العناصر وحفظها في المكتبة.
- **Raccord Recording**: تسجيل خطوات التصفح الحية (نقرات، إدخال نصوص) وتجميعها كسيناريو تشغيل آلي لحفظه وتشغيله لاحقاً.
- **Script Integrations**: إدراج سكريبتات JavaScript و Python وتمرير البيانات بين العقد البرمجية.
- **Material 3 UI Theme**: واجهة استخدام عصرية، مريحة للعين، تدعم اللغات والاتجاهات بسلاسة مع حركات تفاعلية جذابة.
