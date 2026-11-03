# JustRemote (Android TV Remote)

Android TV / TV Box için telefondan kullanılan uzaktan kumanda uygulaması.

## Teknoloji
- Kotlin + Jetpack Compose (Material 3)
- Telefon için dikey (portrait) arayüz, siyah-beyaz tema
- Hedef: Android TV Remote protokolü (TLS + pairing)

## Gereksinimler
- Android Studio (Hedgehog+)
- JDK 17
- Android SDK 35

## Kurulum
1. Android Studio'da projeyi aç.
2. Gradle sync yap.
3. Telefon veya emülatörde çalıştır.

## Kullanım
1. Telefon ile TV'yi aynı Wi-Fi ağına bağla.
2. Uygulamada **TARA** ile TV'yi bul veya IP adresini elle gir.
3. **BAĞLAN**, ardından TV'de gösterilen 6 haneli kodu gir.
4. Kumanda / Touchpad / Klavye sekmelerini kullan.

## Derleme

```bash
./gradlew assembleDebug          # Linux / macOS
gradlew.bat assembleDebug        # Windows
```

APK çıktısı: `app/build/outputs/apk/debug/app-debug.apk`

`main` dalına yapılan her push'ta GitHub Actions APK üretir:
Actions → "Android Build" → Artifacts → `JustRemote-APK`.

### Sürüm yayınlama (oto güncelleme için)

1. `app/build.gradle.kts` içinde `versionCode` / `versionName` değerlerini artır.
2. Aynı sürümle etiket oluştur ve gönder:

```bash
git tag v0.3.0
git push origin v0.3.0
```

`Release` workflow'u imzalı APK'yı derler ve GitHub Releases'e
`JustRemote-<sürüm>.apk` olarak yükler. Uygulamadaki
"GÜNCELLEME KONTROL ET" bu release'i bulup indirir ve kurulum ekranını açar.

İmzalama anahtarı GitHub Secrets'tan gelir (`KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Anahtar her sürümde
aynı olmalıdır; aksi halde güncelleme kurulamaz.

> **Windows notu:** Proje yolu Türkçe karakter içeriyorsa (ör. `Varsayılan Proje`)
> `protoc` adımı hata verir. Bu durumda projeyi ASCII bir yola taşıyın
> (ör. `C:\dev\JustRemote`) veya doğrudan CI'daki APK'yı kullanın.

## Yol Haritası
1. Proje iskeleti (tamamlandı)
2. Pairing & TLS (tamamlandı)
3. Kumanda tuşları (D-pad, OK, Geri, ses, medya) (tamamlandı)
4. Touchpad modu (tamamlandı)
5. Klavye girişi (tamamlandı)
6. Logo + siyah-beyaz telefon arayüzü (tamamlandı)
7. Favoriler & Siteler
8. Güvenlik (TLS doğrulama, APK imza kontrolü, şifreli saklama)

## Güvenlik
- TLS zorunlu
- Pairing kodu TV'de gösterilir
- Token'lar EncryptedSharedPreferences ile saklanır
- Ağ izolasyonu (sadece aynı Wi-Fi)
