package com.scrumtools.service;

import com.scrumtools.entity.HangmanCategory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Sunucu tarafı dahili kelime havuzu — kategori bazlı.
 *
 * Takım oyununda kelime SUNUCUDA seçilir ve istemciye açık gönderilmez; bu yüzden
 * frontend'deki {@code hangmanWords.js} havuzunun bir kopyası burada da bulunur.
 * (Tek kişilik oyun hâlâ frontend havuzunu kullanır.) İki dosya birlikte güncellenmelidir.
 *
 * DB'deki {@code hangman_words} tablosundaki kelimeler bu havuza EK olarak kullanılır;
 * DB boş olsa bile oyun başlatılabilsin diye buradaki liste güvenli bir taban sağlar.
 *
 * Kurallar: kelimeler küçük harf, boşluksuz, 2-30 karakter ve dilin kendi alfabesinde
 * olmalıdır (TR'de q/w/x yok — bkz. HangmanService'teki desenler).
 */
public final class HangmanWordPool {

    private static final Map<HangmanCategory, List<String>> TR_BY_CATEGORY = new EnumMap<>(HangmanCategory.class);
    private static final Map<HangmanCategory, List<String>> EN_BY_CATEGORY = new EnumMap<>(HangmanCategory.class);

    private HangmanWordPool() {
    }

    private static void register(HangmanCategory category, List<String> tr, List<String> en) {
        TR_BY_CATEGORY.put(category, tr);
        EN_BY_CATEGORY.put(category, en);
    }

    static {
        register(HangmanCategory.ANIMALS,
                List.of("kaplan", "aslan", "zürafa", "fil", "penguen", "yunus", "kelebek", "sincap",
                        "kanguru", "timsah", "kirpi", "baykuş", "kartal", "ahtapot", "karınca",
                        "tavşan", "kaplumbağa", "yarasa", "zebra", "leopar"),
                List.of("tiger", "lion", "giraffe", "elephant", "penguin", "dolphin", "butterfly",
                        "squirrel", "kangaroo", "crocodile", "hedgehog", "owl", "eagle", "octopus",
                        "ant", "rabbit", "turtle", "bat", "zebra", "leopard"));

        register(HangmanCategory.PLANTS,
                List.of("papatya", "lale", "gül", "orkide", "karanfil", "menekşe", "nergis", "zambak",
                        "sardunya", "kaktüs", "çınar", "meşe", "söğüt", "kavak", "ladin", "yosun",
                        "sarmaşık", "yonca", "lavanta", "eğrelti"),
                List.of("daisy", "tulip", "rose", "orchid", "carnation", "violet", "daffodil", "lily",
                        "cactus", "maple", "oak", "willow", "poplar", "spruce", "moss", "ivy",
                        "clover", "lavender", "mushroom", "fern"));

        register(HangmanCategory.FOOD,
                List.of("çorba", "pilav", "köfte", "kebap", "börek", "mantı", "simit", "poğaça",
                        "baklava", "dondurma", "çikolata", "sandviç", "makarna", "peynir", "zeytin",
                        "reçel", "ayran", "kahve", "limonata", "şerbet"),
                List.of("soup", "rice", "meatball", "kebab", "pastry", "dumpling", "bagel", "pancake",
                        "dessert", "chocolate", "sandwich", "pasta", "cheese", "olive", "honey",
                        "yogurt", "coffee", "lemonade", "sorbet", "bread"));

        register(HangmanCategory.FRUITS_VEGETABLES,
                List.of("elma", "armut", "çilek", "karpuz", "kavun", "kiraz", "muz", "portakal",
                        "mandalina", "şeftali", "erik", "incir", "üzüm", "domates", "salatalık",
                        "patlıcan", "biber", "havuç", "ıspanak", "brokoli"),
                List.of("apple", "pear", "strawberry", "watermelon", "melon", "cherry", "banana",
                        "orange", "tangerine", "peach", "plum", "fig", "grape", "tomato", "cucumber",
                        "eggplant", "pepper", "carrot", "spinach", "broccoli"));

        register(HangmanCategory.COUNTRIES,
                List.of("türkiye", "almanya", "fransa", "ispanya", "italya", "portekiz", "hollanda",
                        "belçika", "isviçre", "avusturya", "yunanistan", "macaristan", "polonya",
                        "japonya", "çin", "hindistan", "brezilya", "arjantin", "kanada", "fas"),
                List.of("turkey", "germany", "france", "spain", "italy", "portugal", "netherlands",
                        "belgium", "switzerland", "austria", "greece", "hungary", "poland", "japan",
                        "china", "india", "brazil", "argentina", "canada", "morocco"));

        register(HangmanCategory.CITIES,
                List.of("istanbul", "ankara", "izmir", "bursa", "antalya", "konya", "trabzon", "adana",
                        "eskişehir", "samsun", "kayseri", "mardin", "erzurum", "çanakkale",
                        "diyarbakır", "malatya", "sivas", "rize", "aydın", "muğla"),
                List.of("istanbul", "ankara", "london", "paris", "berlin", "madrid", "rome", "vienna",
                        "prague", "lisbon", "amsterdam", "dublin", "oslo", "helsinki", "warsaw",
                        "budapest", "tokyo", "sydney", "toronto", "cairo"));

        register(HangmanCategory.NATURE,
                List.of("dağ", "deniz", "orman", "çöl", "ada", "nehir", "şelale", "vadi", "mağara",
                        "buzul", "volkan", "göl", "kanyon", "yayla", "körfez", "plaj", "bataklık",
                        "tepe", "kayalık", "ırmak"),
                List.of("mountain", "ocean", "forest", "desert", "island", "river", "waterfall",
                        "valley", "cave", "glacier", "volcano", "lake", "canyon", "plateau", "bay",
                        "beach", "swamp", "hill", "cliff", "meadow"));

        register(HangmanCategory.SPACE,
                List.of("gezegen", "yıldız", "güneş", "gökada", "meteor", "uydu", "teleskop",
                        "astronot", "roket", "yörünge", "karadelik", "evren", "mars", "satürn",
                        "jüpiter", "venüs", "takımyıldız", "asteroit", "kuyrukluyıldız", "gökbilim"),
                List.of("planet", "star", "sun", "galaxy", "meteor", "satellite", "telescope",
                        "astronaut", "rocket", "orbit", "eclipse", "universe", "mars", "saturn",
                        "jupiter", "venus", "constellation", "asteroid", "comet", "nebula"));

        register(HangmanCategory.WEATHER,
                List.of("yağmur", "kar", "rüzgar", "fırtına", "sis", "dolu", "gökkuşağı", "şimşek",
                        "yıldırım", "bulut", "ayaz", "sıcaklık", "kuraklık", "ilkbahar", "yaz",
                        "sonbahar", "kış", "çiy", "kırağı", "tipi"),
                List.of("rain", "snow", "wind", "storm", "fog", "hail", "rainbow", "lightning",
                        "thunder", "cloud", "frost", "humidity", "drought", "spring", "summer",
                        "autumn", "winter", "breeze", "blizzard", "sunshine"));

        register(HangmanCategory.PROFESSIONS,
                List.of("doktor", "hemşire", "öğretmen", "mühendis", "avukat", "hakim", "pilot",
                        "kaptan", "aşçı", "garson", "marangoz", "terzi", "berber", "çiftçi",
                        "itfaiyeci", "polis", "eczacı", "mimar", "muhasebeci", "veteriner"),
                List.of("doctor", "nurse", "teacher", "engineer", "lawyer", "judge", "pilot",
                        "captain", "chef", "waiter", "carpenter", "tailor", "barber", "farmer",
                        "firefighter", "police", "pharmacist", "architect", "accountant", "veterinarian"));

        register(HangmanCategory.SPORTS,
                List.of("futbol", "basketbol", "voleybol", "tenis", "yüzme", "güreş", "boks",
                        "atletizm", "hentbol", "kayak", "bisiklet", "halter", "jimnastik", "okçuluk",
                        "eskrim", "binicilik", "yelken", "kürek", "judo", "maraton"),
                List.of("football", "basketball", "volleyball", "tennis", "swimming", "wrestling",
                        "boxing", "athletics", "handball", "skiing", "cycling", "weightlifting",
                        "gymnastics", "archery", "fencing", "riding", "sailing", "rowing", "judo",
                        "marathon"));

        register(HangmanCategory.MUSIC,
                List.of("gitar", "piyano", "keman", "davul", "flüt", "saksafon", "trompet", "arp",
                        "akordeon", "bağlama", "ney", "kanun", "org", "melodi", "ritim", "nota",
                        "akor", "orkestra", "senfoni", "koro"),
                List.of("guitar", "piano", "violin", "drum", "flute", "saxophone", "trumpet", "harp",
                        "accordion", "banjo", "cello", "clarinet", "organ", "melody", "rhythm",
                        "note", "chord", "orchestra", "symphony", "choir"));

        register(HangmanCategory.CINEMA,
                List.of("sinema", "oyuncu", "yönetmen", "senaryo", "sahne", "kamera", "kurgu", "gişe",
                        "fragman", "altyazı", "dizi", "bölüm", "karakter", "komedi", "korku",
                        "gerilim", "animasyon", "belgesel", "festival", "ödül"),
                List.of("cinema", "actor", "director", "script", "scene", "camera", "editing",
                        "trailer", "subtitle", "series", "episode", "character", "comedy", "horror",
                        "thriller", "animation", "documentary", "festival", "award", "premiere"));

        register(HangmanCategory.LITERATURE,
                List.of("roman", "şiir", "öykü", "deneme", "tiyatro", "destan", "masal", "yazar",
                        "şair", "kitap", "kütüphane", "sayfa", "bölüm", "kahraman", "dize", "kafiye",
                        "mecaz", "benzetme", "yayınevi", "çeviri"),
                List.of("novel", "poem", "story", "essay", "theatre", "epic", "fable", "author",
                        "poet", "book", "library", "page", "chapter", "hero", "verse", "rhyme",
                        "metaphor", "simile", "publisher", "translation"));

        register(HangmanCategory.ART,
                List.of("resim", "heykel", "tuval", "fırça", "boya", "palet", "desen", "portre",
                        "manzara", "sergi", "galeri", "müze", "seramik", "çömlek", "mozaik",
                        "minyatür", "hat", "ebru", "fotoğraf", "gravür"),
                List.of("painting", "sculpture", "canvas", "brush", "paint", "palette", "pattern",
                        "portrait", "landscape", "exhibition", "gallery", "museum", "ceramic",
                        "pottery", "mosaic", "miniature", "calligraphy", "etching", "photograph",
                        "sketch"));

        register(HangmanCategory.TECHNOLOGY,
                List.of("bilgisayar", "klavye", "ekran", "fare", "yazıcı", "tarayıcı", "telefon",
                        "tablet", "kulaklık", "hoparlör", "kamera", "batarya", "işlemci", "bellek",
                        "anakart", "modem", "yönlendirici", "kablo", "robot", "dron"),
                List.of("computer", "keyboard", "screen", "mouse", "printer", "scanner", "phone",
                        "tablet", "headphone", "speaker", "camera", "battery", "processor", "memory",
                        "motherboard", "modem", "router", "cable", "robot", "drone"));

        register(HangmanCategory.SOFTWARE,
                List.of("kod", "yazılım", "donanım", "fonksiyon", "değişken", "algoritma", "döngü",
                        "dizi", "sınıf", "nesne", "arayüz", "kütüphane", "derleyici", "hata", "sürüm",
                        "depo", "dal", "birleştirme", "sunucu", "veritabanı"),
                List.of("code", "software", "hardware", "function", "variable", "algorithm", "loop",
                        "array", "class", "object", "interface", "library", "compiler", "debug",
                        "version", "repository", "branch", "merge", "server", "database"));

        register(HangmanCategory.SCRUM,
                List.of("sprint", "çeviklik", "takım", "pano", "birikim", "öncelik", "hedef",
                        "planlama", "tahmin", "değerlendirme", "retrospektif", "kanban", "hız",
                        "kapasite", "engel", "teslimat", "sürüm", "geribildirim", "toplantı", "paydaş"),
                List.of("sprint", "agile", "team", "board", "backlog", "priority", "goal", "planning",
                        "estimate", "review", "retrospective", "kanban", "velocity", "capacity",
                        "blocker", "delivery", "release", "feedback", "standup", "stakeholder"));

        register(HangmanCategory.OFFICE,
                List.of("toplantı", "sunum", "rapor", "bütçe", "fatura", "sözleşme", "müşteri",
                        "tedarikçi", "pazarlama", "satış", "strateji", "hedef", "verimlilik", "terfi",
                        "maaş", "izin", "mesai", "departman", "yönetici", "proje"),
                List.of("meeting", "presentation", "report", "budget", "invoice", "contract", "client",
                        "supplier", "marketing", "sales", "strategy", "target", "productivity",
                        "promotion", "salary", "leave", "overtime", "department", "manager", "project"));

        register(HangmanCategory.SCIENCE,
                List.of("kimya", "fizik", "biyoloji", "matematik", "deney", "laboratuvar", "mikroskop",
                        "atom", "molekül", "hücre", "enerji", "yerçekimi", "manyetizma", "elektrik",
                        "denklem", "teori", "hipotez", "element", "genetik", "evrim"),
                List.of("chemistry", "physics", "biology", "mathematics", "experiment", "laboratory",
                        "microscope", "atom", "molecule", "cell", "energy", "gravity", "magnetism",
                        "electricity", "equation", "theory", "hypothesis", "element", "genetics",
                        "evolution"));

        register(HangmanCategory.HEALTH,
                List.of("hastane", "doktor", "hemşire", "ameliyat", "reçete", "ilaç", "aşı", "vitamin",
                        "bağışıklık", "ateş", "grip", "alerji", "röntgen", "tansiyon", "nabız",
                        "teşhis", "tedavi", "ambulans", "eczane", "muayene"),
                List.of("hospital", "doctor", "nurse", "surgery", "prescription", "medicine", "vaccine",
                        "vitamin", "immunity", "fever", "influenza", "allergy", "radiology", "pressure",
                        "pulse", "diagnosis", "treatment", "ambulance", "pharmacy", "checkup"));

        register(HangmanCategory.BODY,
                List.of("kalp", "akciğer", "karaciğer", "böbrek", "beyin", "mide", "damar", "kemik",
                        "kas", "omurga", "dirsek", "bilek", "parmak", "topuk", "kaburga", "göz",
                        "kulak", "burun", "dudak", "kaş"),
                List.of("heart", "lung", "liver", "kidney", "brain", "stomach", "vein", "bone",
                        "muscle", "spine", "elbow", "wrist", "finger", "heel", "rib", "eye", "ear",
                        "nose", "lip", "eyebrow"));

        register(HangmanCategory.CLOTHING,
                List.of("gömlek", "pantolon", "ceket", "palto", "kazak", "tişört", "etek", "elbise",
                        "çorap", "ayakkabı", "bot", "terlik", "şapka", "atkı", "eldiven", "kemer",
                        "kravat", "yelek", "mont", "eşofman"),
                List.of("shirt", "trousers", "jacket", "coat", "sweater", "blouse", "skirt", "dress",
                        "socks", "shoes", "boots", "slippers", "hat", "scarf", "gloves", "belt",
                        "tie", "vest", "jeans", "hoodie"));

        register(HangmanCategory.HOME,
                List.of("mutfak", "salon", "yatak", "koltuk", "masa", "sandalye", "dolap", "halı",
                        "perde", "ayna", "lamba", "avize", "buzdolabı", "fırın", "tencere", "tabak",
                        "bardak", "çatal", "kaşık", "yastık"),
                List.of("kitchen", "lounge", "bedroom", "sofa", "table", "chair", "wardrobe", "carpet",
                        "curtain", "mirror", "lamp", "chandelier", "fridge", "oven", "pot", "plate",
                        "fork", "spoon", "pillow", "blanket"));

        register(HangmanCategory.TRANSPORT,
                List.of("otobüs", "tramvay", "metro", "vapur", "uçak", "tren", "bisiklet", "motosiklet",
                        "kamyon", "taksi", "helikopter", "yelkenli", "gemi", "feribot", "araba",
                        "karavan", "römork", "iskele", "havalimanı", "istasyon"),
                List.of("bus", "tram", "metro", "ferry", "airplane", "train", "bicycle", "motorcycle",
                        "truck", "taxi", "helicopter", "sailboat", "ship", "yacht", "car", "caravan",
                        "trailer", "harbor", "airport", "station"));

        register(HangmanCategory.SCHOOL,
                List.of("okul", "öğrenci", "öğretmen", "sınıf", "ders", "tahta", "defter", "kalem",
                        "silgi", "çanta", "sınav", "ödev", "karne", "teneffüs", "kütüphane",
                        "laboratuvar", "müdür", "zil", "harita", "diploma"),
                List.of("school", "student", "teacher", "classroom", "lesson", "board", "notebook",
                        "pencil", "eraser", "backpack", "exam", "homework", "report", "recess",
                        "library", "laboratory", "principal", "bell", "map", "diploma"));

        register(HangmanCategory.MYTHOLOGY,
                List.of("ejderha", "büyücü", "peri", "dev", "cüce", "cadı", "kahraman", "kılıç",
                        "kalkan", "hazine", "krallık", "şato", "prenses", "tılsım", "iksir", "lanet",
                        "kehanet", "efsane", "canavar", "tanrıça"),
                List.of("dragon", "wizard", "fairy", "giant", "dwarf", "witch", "hero", "sword",
                        "shield", "treasure", "kingdom", "castle", "princess", "amulet", "potion",
                        "curse", "prophecy", "legend", "monster", "goddess"));

        register(HangmanCategory.HOBBIES,
                List.of("satranç", "tavla", "yapboz", "bulmaca", "origami", "fotoğrafçılık", "resim",
                        "bahçıvanlık", "koleksiyon", "kamp", "balıkçılık", "dalış", "tırmanış", "dans",
                        "örgü", "seramik", "yürüyüş", "bisiklet", "kitap", "günlük"),
                List.of("chess", "backgammon", "puzzle", "crossword", "origami", "photography",
                        "painting", "gardening", "collecting", "camping", "fishing", "diving",
                        "climbing", "dancing", "knitting", "pottery", "hiking", "cycling", "reading",
                        "journaling"));
    }

    private static Map<HangmanCategory, List<String>> mapFor(String language) {
        return "en".equals(language) ? EN_BY_CATEGORY : TR_BY_CATEGORY;
    }

    /** Tek bir kategorinin kelimeleri. */
    public static List<String> forCategory(String language, HangmanCategory category) {
        return mapFor(language).getOrDefault(category, List.of());
    }

    /** Tüm kategorilerin kelimeleri tek listede — kategori seçilmediğinde kullanılır. */
    public static List<String> forLanguage(String language) {
        List<String> all = new ArrayList<>();
        mapFor(language).values().forEach(all::addAll);
        return all;
    }

    /** Kategori seçiliyse o kategorinin, değilse tüm havuzun kelimeleri. */
    public static List<String> resolve(String language, HangmanCategory category) {
        return category == null ? forLanguage(language) : forCategory(language, category);
    }
}
