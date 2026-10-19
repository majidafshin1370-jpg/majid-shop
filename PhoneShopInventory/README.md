# موبایل بلاد شاپور — PhoneShopInventory

## ساختار پروژه
پروژه اندروید با Jetpack Compose برای مدیریت موجودی مغازه موبایل.

## ویژگی‌ها
- نمایش و مدیریت محصولات
- فیلتر بر اساس برند (Samsung, Apple, Nokia, Xiaomi, Huawei, LG)
- فیلتر بر اساس دسته‌بندی (گوشی، لوازم جانبی، قطعات)
- مرتب‌سازی جدیدترین / قدیمی‌ترین
- لیست بدهکاران مغازه
- نمایش تاریخ شمسی و میلادی

## ساخت پروژه
```bash
git clone https://github.com/majidafshin1370-jpg/bilad-shop
cd PhoneShopInventory
./gradlew assembleDebug
```

## آیکون اپ
فایل آیکون را در مسیرهای زیر قرار دهید:
- app/src/main/res/mipmap-mdpi/ic_launcher.png (48x48)
- app/src/main/res/mipmap-hdpi/ic_launcher.png (72x72)
- app/src/main/res/mipmap-xhdpi/ic_launcher.png (96x96)
- app/src/main/res/mipmap-xxhdpi/ic_launcher.png (144x144)
- app/src/main/res/mipmap-xxxhdpi/ic_launcher.png (192x192)

## پیش‌نیازها
- JDK 17، Android SDK 34 (minSdk 26)، Gradle 8.2+

> توجه: wrapper گریدل در این بسته نیست؛ ابتدا آیکون‌ها را در پوشه‌های mipmap اضافه کنید
> (فعلاً فقط PLACEHOLDER.txt وجود دارد) وگرنه ساخت با خطای resource پیدا نشدن آیکون متوقف می‌شود.
