# Marvel Splendor Puan Sayacı (Android)

Splendor'un Marvel versiyonu için Android puan sayacı uygulaması. İnternet gerekmez.

## İndir ve kur

1. Repodaki **Releases** bölümünden en son `MarvelSplendorSayac-vX.apk` dosyasını telefona indir.
2. Dosyayı aç; telefon "bilinmeyen kaynaklardan yükleme" izni isterse izin ver.
3. Kur ve aç. Android 5.0 ve üzeri tüm telefonlarda çalışır.

Yeni sürümler eskisinin üzerine kurulur, kayıtlı oyun silinmez.

## Özellikler

- 2 / 3 / 4 oyuncu: soylu sayısı (3/4/5) ve jeton sayıları (4/5/7, altın 5) otomatik; klasik Splendor kuralları
- Zafer yolu: tüm oyuncuların hedefe ne kadar yaklaştığı tek bakışta
- Oyuncu panelleri: animasyonlu puan halkası, renk bonusları, seviyelere göre kart yığınları, ayrılmış kart yuvaları, soylular
- Soylu karoları: her oyuncunun her soyluya ne kadar yaklaştığı çubuklarla; şartı sağlayana tur sonunda otomatik verilir (turda en fazla bir tane)
- Grafikler: puan akışı, bonus dağılımı (radar), puan kaynakları, en verimli oyuncu gibi istatistikler
- Hedef puana ulaşılınca son tur uyarısı; tur tamamlanınca podyum ve sonuç grafikleri. Eşitlikte en az kart satın alan kazanır
- Geri al, elle düzeltme, oyun kaydı, rövanş; ekran oyun sırasında kararmaz, uygulama kapansa bile oyun kaldığı yerden devam eder

## Derleme

APK'yı GitHub Actions (`.github/workflows/android.yml`) derler: her push'ta APK, Actions sekmesine yüklenir; varsayılan dalda ayrıca yeni bir Release yayınlanır.

Uygulama hep aynı anahtarla (`app/release.keystore`) imzalanır, bu yüzden güncellemeler sorunsuz kurulur. Play Store'a koymak istersen kendi anahtarını oluşturup `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` ortam değişkenleriyle verebilirsin.
