# ChocoSMP eklentisi

Komutlar: /shop (market GUI), /sell [hand], /stash, /spawn, /setspawn, /stash give <oyuncu> (admin)
Gereksinim: Vault + EssentialsX (ekonomi için), Paper 26.x, Java 25.

## Jar'ı nasıl derlerim? (GitHub ile, bilgisayara bir şey kurmadan)
1. github.com'da ücretsiz hesap aç, yeni (boş) bir repo oluştur.
2. Bu klasörün içindeki her şeyi (.github klasörü dahil) repoya yükle.
3. Repoda "Actions" sekmesine gir. "Build" çalışır (yeşil tik çıkana kadar bekle).
4. Çalışan build'e tıkla, en altta "Artifacts" kısmından ChocoSMP.zip'i indir. İçinden çıkan jar'ı sunucunun plugins klasörüne at.

## Kurulum
- /bal ve /pay komutlarını EssentialsX zaten veriyor, bu yüzden eklemedim. Ekonomi Vault üzerinden EssentialsX'e bağlanır.
- Komut çakışması: EssentialsX'in config.yml dosyasında `disabled-commands: [spawn, setspawn, sell]` yaz, yoksa onun komutları devreye girer.
- Spawn'ı ayarlamak için spawn'da dururken /setspawn yaz.
- Fiyatlar plugins/ChocoSMP/shop.yml içinde. Değiştirince sunucuyu yeniden başlat.
- Market dolu envanterde alınan itemleri otomatik /stash'e gönderir.
