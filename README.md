# Marvel Splendor Puan Sayacı (Android)

Splendor'un Marvel versiyonu için Android puan sayacı uygulaması. İnternet gerekmez.

## İndir ve kur

1. Repodaki **Releases** bölümünden en son `MarvelSplendorSayac-vX.apk` dosyasını telefona indir.
2. Dosyayı aç; telefon "bilinmeyen kaynaklardan yükleme" izni isterse izin ver.
3. Kur ve aç. Android 5.0 ve üzeri tüm telefonlarda çalışır.

Yeni sürümler eskisinin üzerine kurulur, kayıtlı oyun silinmez.

## Özellikler

- 2 / 3 / 4 oyuncu: soylu sayısı (3/4/5) ve jeton sayıları (4/5/7, joker 5) otomatik
- Tur ve sıra takibi; hedef puana ulaşılınca "son tur" uyarısı, tur tamamlanınca oyun biter
- Kart satın alma (seviye, bonus taşı, puan), kart ayırma (en fazla 3)
- Soylu gereksinimleri girilirse tur sonunda otomatik verilir (turda en fazla bir tane), kime kaç kart kaldığı gösterilir
- Eşitlikte en az kart satın alan kazanır
- Geri al, elle düzeltme, oyun kaydı, rövanş
- Ayarlar: hedef puan, taş adları, isteğe bağlı Avengers jetonu (+3)
- Oyun sırasında ekran kararmaz; geri tuşu açık pencereyi kapatır; uygulama kapansa bile oyun kaldığı yerden devam eder

## Derleme

APK'yı GitHub Actions (`.github/workflows/android.yml`) derler: her push'ta APK, Actions sekmesine yüklenir; varsayılan dalda ayrıca yeni bir Release yayınlanır.

Uygulama hep aynı anahtarla (`app/release.keystore`) imzalanır, bu yüzden güncellemeler sorunsuz kurulur. Play Store'a koymak istersen kendi anahtarını oluşturup `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` ortam değişkenleriyle verebilirsin.
