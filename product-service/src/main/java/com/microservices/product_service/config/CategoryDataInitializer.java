package com.microservices.product_service.config;

import com.microservices.product_service.entity.Category;
import com.microservices.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Uygulama başlarken varsayılan kategorileri DB'ye ekler.
 * Kategori zaten varsa (isim kontrolüyle) tekrar eklenmez.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    /** Sisteme önceden yüklenecek varsayılan kategoriler */
    private static final List<String[]> DEFAULT_CATEGORIES = List.of(
            // { "Ad", "Açıklama" }
            new String[]{"Elektronik",             "Bilgisayar, laptop, tablet ve diğer elektronik ürünler"},
            new String[]{"Telefon & Aksesuar",     "Akıllı telefonlar, kılıflar ve aksesuarlar"},
            new String[]{"Giyim & Aksesuar",       "Erkek, kadın ve çocuk giyim ürünleri"},
            new String[]{"Ayakkabı & Çanta",       "Her türlü ayakkabı, çanta ve deri aksesuar"},
            new String[]{"Ev & Yaşam",             "Mobilya, dekorasyon ve ev tekstili"},
            new String[]{"Mutfak & Ev Aletleri",   "Küçük ev aletleri ve mutfak gereçleri"},
            new String[]{"Kitap, Müzik & Film",    "Kitaplar, müzik albümleri ve filmler"},
            new String[]{"Spor & Outdoor",         "Spor ekipmanları, kamp ve outdoor malzemeleri"},
            new String[]{"Oyun & Hobi",            "Video oyunları, oyuncaklar ve hobi malzemeleri"},
            new String[]{"Kozmetik & Kişisel Bakım","Makyaj, parfüm ve kişisel bakım ürünleri"},
            new String[]{"Bebek & Çocuk",          "Bebek kıyafetleri, arabaları ve oyuncakları"},
            new String[]{"Otomotiv",               "Araç aksesuarları ve yedek parçalar"},
            new String[]{"Bahçe & Yapı Market",    "Bahçe aletleri, inşaat ve tamirat malzemeleri"},
            new String[]{"Ofis & Kırtasiye",       "Ofis malzemeleri, kırtasiye ve yazıcı sarf malzemeleri"},
            new String[]{"Gıda & İçecek",          "Şarküteri, içecek ve gıda ürünleri"},
            new String[]{"Pet Shop",               "Evcil hayvan maması, aksesuarları ve bakım ürünleri"},
            new String[]{"Sağlık & Eczane",        "Takviye, medikal ürünler ve sağlık malzemeleri"},
            new String[]{"Genel",                  "Diğer kategorilere girmeyen ürünler"}
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int added = 0;
        for (String[] cat : DEFAULT_CATEGORIES) {
            String name = cat[0];
            String description = cat[1];
            boolean exists = categoryRepository.findByNameIgnoreCase(name).isPresent();
            if (!exists) {
                categoryRepository.save(
                        Category.builder()
                                .name(name)
                                .description(description)
                                .build()
                );
                added++;
            }
        }
        if (added > 0) {
            log.info("✅ Kategori seed tamamlandı — {} yeni kategori eklendi.", added);
        } else {
            log.info("ℹ️  Kategoriler zaten mevcut, seed atlandı.");
        }
    }
}

