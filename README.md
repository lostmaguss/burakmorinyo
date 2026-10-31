# burakmorinyo — Android uygulamaları

Bu repoda iki Android uygulaması var. Her ikisi de GitHub Actions ile derlenir, APK'ları **Releases** bölümünden telefona indirilip kurulur.

| Uygulama | Klasör | APK |
|---|---|---|
| **Gelir Gider** — kişisel finans ve bütçe | `gelirgider/` | `GelirGider-vX.apk` |
| Marvel Splendor Puan Sayacı | `app/` | `MarvelSplendorSayac-vX.apk` |

---

## Gelir Gider

Modern, internetsiz, Türkiye'ye göre düşünülmüş bir gelir-gider ve bütçe uygulaması (Kotlin + Jetpack Compose, Material 3).

### Kurulum

1. **Releases** bölümünden en son `GelirGider-vX.apk` dosyasını telefona indir.
2. Dosyayı aç; telefon "bilinmeyen kaynaklardan yükleme" izni isterse izin ver.
3. Kur ve aç. Android 8.0 ve üzeri. Yeni sürümler eskisinin üzerine kurulur, kayıtlar silinmez.

### Özellikler

**Kayıt**
- Hesap makineli tutar girişi (`120+45,5×2` gibi), gider / gelir / transfer
- En çok kullandığın kategoriler başta; not yazarken önceki notlardan öneri ve kategori otomatik seçimi
- Tarih seçimi, "dün" kısayolu, kaydırarak silme ve **geri al**
- Ana ekran widget'ı ve uygulama kısayolları (uzun bas → Gider ekle / Gelir ekle)

**Hesaplar ve kredi kartları**
- Nakit, banka, kredi kartı, birikim, yatırım (altın/döviz/fon) hesapları; net varlık
- Kredi kartında **hesap kesim ve son ödeme günü**: ekstre borcu, kalan ödeme, dönem içi harcama, kullanılabilir limit
- **Taksitli alışveriş**: 2–24 taksit, her taksit ilgili aya yazılır; gelecek taksitler ayrıca gösterilir
- Kart ödemesi = banka hesabından karta transfer

**Döviz ve altın**
- Hesap açarken para birimi: ₺, $, €, £, CHF, **gram altın**, **çeyrek altın**
- Canlı kurlar (Truncgil Finans; yedek: TCMB + gold-api), saatlik otomatik güncelleme, elle yenileme
- Net varlık güncel kurla TL'ye çevrilir; ayrıca dolar ve gram altın karşılığı gösterilir
- Farklı para birimleri arası transfer: ör. bankadan 30.000 ₺ ile 4,82 gr altın alımı (gerçek alış miktarı yazılabilir)
- Döviz/altın hesaplarındaki harcamalar bütçe ve grafiklerde güncel kurla TL'ye çevrilir

**Enflasyon karşısında**
- Analiz ekranında geçen yılın aynı dönemine (yoksa önceki döneme) göre: nominal, **reel (TÜFE'den arındırılmış)** ve **dolar bazında** değişim
- Giderlerde "gerçekte daha az mı harcıyorum?", gelirde "alım gücüm arttı mı?" yorumu
- Yıllık TÜFE oranı Ayarlar'dan girilir (TÜİK her ayın 3'ünde açıklar); geçmiş dolar kuru TCMB arşivinden otomatik alınır

**Fiş tarama**
- İşlem ekranında 📷 düğmesi ya da Özet'teki "Fiş tara": fişin fotoğrafını çek veya galeriden seç
- Telefonda, internetsiz metin tanıma (Google ML Kit); toplam tutar, tarih, mağaza ve kategori otomatik dolar
- Migros, A101, BİM, ŞOK, Opet, Shell, Starbucks, Teknosa, LC Waikiki gibi 35+ marka tanınır; bilinmeyen mağazada fiş metninden kategori tahmin edilir, daha önce o mağazaya verdiğin kategori önceliklidir

**Bütçe ve plan**
- **Maaş gününe göre dönem** (ör. 15'inden 14'üne), takvim ayı da olur
- Aylık toplam bütçe + kategori bütçeleri, geçmiş ortalamaya göre bütçe önerisi
- %80 ve %100'de bildirim
- Düzenli işlemler: kira, fatura, maaş… otomatik kaydedilir ya da hatırlatılır ("Ödendi / Atla")
- Abonelik takibi: aylık/yıllık toplam maliyet, popüler abonelikler tek dokunuşla
- Birikim hedefleri: ilerleme halkası, "ayda şu kadar ayırmalısın" hesabı
- Borç / alacak defteri, kısmi ödeme, istenirse hesaba işleme

**Akıllı öneriler**
- Günlük harcama hakkı (yaklaşan ödemeler düşülerek)
- Dönem sonu harcama tahmini, geçen döneme göre değişim
- Olağandışı yüksek kategori uyarısı, tasarruf oranı (50/30/20), harcamasız günler

**Analiz**
- Kategori halka grafiği, son 6 dönem gelir/gider sütunları, günlük harcama, hafta günü alışkanlığı, net varlık seyri

**Gizlilik ve veri**
- Parmak izi / yüz / ekran kilidi ile uygulama kilidi, "tutarları gizle" modu
- JSON yedek al / geri yükle (Drive'a kaydedilebilir), Excel için CSV dışa aktarma, Android otomatik yedeği
- Açık / koyu tema, Material You renkleri, ₺ $ € £ para birimleri

### Yol haritası — sırada ne olabilir?

Benzer uygulamalar (Wallet, Money Manager, YNAB, Cashew, Spendee) ve Türkiye'deki ihtiyaçlar incelenerek çıkarılan fikirler:

Tamamlananlar: ~~fiş tarama~~, ~~canlı döviz ve altın kuru~~, ~~enflasyona göre karşılaştırma~~.

1. **Banka SMS / bildirim okuma** — kart harcaması SMS'i gelince işlemi otomatik önermek
2. **Kategori bazında kişisel enflasyon** — hangi kalemde fiyatların en çok arttığını göstermek
3. **TÜFE'nin otomatik alınması** — TCMB EVDS anahtarı girilirse oranı her ay kendiliğinden güncellemek
4. **Fişleri saklama** — fiş fotoğrafını işleme ekleyip garanti/iade için arşivlemek
5. **Ortak bütçe** — eş/aile ile paylaşılan hesaplar ve bulut eşitleme
6. **Etiketler ve alt kategoriler** — "tatil 2026" gibi projeye göre harcama gruplama
7. **Kredi ve KMH hesaplayıcı** — kalan anapara, faiz, erken kapama senaryoları
8. **Kartlar arası en iyi kesim günü önerisi** — büyük alışverişi hangi kartla yaparsan ödemesi en geç gelir
9. **Açık Bankacılık entegrasyonu** — banka hareketlerinin izinle otomatik çekilmesi
10. **Yapay zekâ asistanı** — "bu ay nerede tasarruf edebilirim?" gibi sorulara cevap
11. **Excel/CSV içe aktarma** — başka uygulamalardan veya banka ekstresinden geçiş
12. **Wear OS / hızlı ayar kutucuğu** — saatten veya bildirim panelinden tek dokunuşla kayıt

### Geliştirme

```bash
gradle :gelirgider:testDebugUnitTest   # iş mantığı, veritabanı, uçtan uca akış ve ekran görüntüsü testleri
gradle :gelirgider:assembleRelease     # imzalı, küçültülmüş APK
```

Ekran görüntüsü testleri (Robolectric + Roborazzi) tüm ekranları örnek veriyle çizip `gelirgider/build/screenshots/` klasörüne kaydeder; CI bunları Actions'a ayrıca yükler. Maven Central'a erişimi kısıtlı ortamlarda `ROBOLECTRIC_REPO_URL` ortam değişkeniyle bir ayna gösterilebilir.

Kod yapısı:

- `data/` — Room veritabanı (hesap, kategori, işlem, düzenli işlem, bütçe, hedef, borç), ayarlar (DataStore), yedekleme
- `domain/` — para biçimi, hesap makinesi, dönem hesabı, kredi kartı ekstresi, akıllı öneriler
- `ui/` — Compose ekranları (Özet, İşlemler, Analiz, Planla, Hesaplar, Ayarlar)
- `work/` — saatlik arka plan görevi (otomatik kayıt, hatırlatmalar), bildirimler
- `widget/` — ana ekran widget'ı (Glance)

---

## Marvel Splendor Puan Sayacı

Splendor'un Marvel versiyonu için Android puan sayacı uygulaması. İnternet gerekmez.

### Özellikler

- 2 / 3 / 4 oyuncu: soylu sayısı (3/4/5) ve jeton sayıları (4/5/7, altın 5) otomatik; klasik Splendor kuralları
- Zafer yolu: tüm oyuncuların hedefe ne kadar yaklaştığı tek bakışta
- Oyuncu panelleri: animasyonlu puan halkası, renk bonusları, seviyelere göre kart yığınları, ayrılmış kart yuvaları, soylular
- Soylu karoları: her oyuncunun her soyluya ne kadar yaklaştığı çubuklarla; şartı sağlayana tur sonunda otomatik verilir (turda en fazla bir tane)
- Grafikler: puan akışı, bonus dağılımı (radar), puan kaynakları, en verimli oyuncu gibi istatistikler
- Hedef puana ulaşılınca son tur uyarısı; tur tamamlanınca podyum ve sonuç grafikleri. Eşitlikte en az kart satın alan kazanır
- Geri al, elle düzeltme, oyun kaydı, rövanş; ekran oyun sırasında kararmaz, uygulama kapansa bile oyun kaldığı yerden devam eder

---

## Derleme ve imzalama

APK'ları GitHub Actions derler (`.github/workflows/gelirgider.yml`, `.github/workflows/android.yml`): her push'ta APK Actions sekmesine yüklenir. Gelir Gider her derlemede bir Release yayınlar; Splendor sayacı varsayılan dalda yayınlar.

İki uygulama da aynı anahtarla (`app/release.keystore`) imzalanır, bu yüzden güncellemeler sorunsuz kurulur. Play Store'a koymak istersen kendi anahtarını oluşturup `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` ortam değişkenleriyle verebilirsin.
