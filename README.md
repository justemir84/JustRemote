# JustRemote (Android TV Remote)

Android TV / TV Box için uzaktan kumanda uygulaması (prototip).

## Teknoloji
- Kotlin + Jetpack Compose
- Android TV (LEANBACK_LAUNCHER)
- Hedef: Android TV Remote protokolü (TLS + pairing)

## Gereksinimler
- Android Studio (Hedgehog+)
- JDK 17
- Android SDK 34

## Kurulum
1. Android Studio'da projeyi aç.
2. Gradle sync yap.
3. Emülatör veya TV cihazında çalıştır.

## Yol Haritası
1. Proje iskeleti (tamamlandı)
2. Pairing & TLS
3. Kumanda tuşları (D-pad, OK, Geri)
4. Touchpad (mouse) modu
5. Klavye girişi
6. Favoriler & Siteler
7. Güvenlik (TLS, pairing onayı, şifreli saklama)

## Güvenlik
- TLS zorunlu
- Pairing kodu TV'de gösterilir
- Token'lar EncryptedSharedPreferences ile saklanır
- Ağ izolasyonu (sadece aynı Wi-Fi)