/**
 * Adam Asmaca (Hangman) kelime havuzları — kategori bazlı.
 *
 * Kelimeler küçük harfle, boşluksuz tutulur; ekranda gösterirken
 * Türkçe'ye duyarlı (İ/ı) büyük harfe çevrilir.
 *
 * NOT: Bu havuzun bir kopyası backend'de HangmanWordPool.java içinde durur
 * (takım oyununda kelime sunucuda seçilir). İki dosya birlikte güncellenmelidir.
 *
 * Kurallar: küçük harf, boşluksuz, 2-30 karakter, dilin kendi alfabesinde
 * (TR'de q/w/x yok).
 */

/** Kategori başına dil dil kelimeler — tek kaynak, aşağıda türetilir. */
const CATEGORY_DATA = [
  {
    code: 'ANIMALS', emoji: '🐾',
    tr: {
      label: 'Hayvanlar',
      words: ['kaplan', 'aslan', 'zürafa', 'fil', 'penguen', 'yunus', 'kelebek', 'sincap',
        'kanguru', 'timsah', 'kirpi', 'baykuş', 'kartal', 'ahtapot', 'karınca',
        'tavşan', 'kaplumbağa', 'yarasa', 'zebra', 'leopar']
    },
    en: {
      label: 'Animals',
      words: ['tiger', 'lion', 'giraffe', 'elephant', 'penguin', 'dolphin', 'butterfly',
        'squirrel', 'kangaroo', 'crocodile', 'hedgehog', 'owl', 'eagle', 'octopus',
        'ant', 'rabbit', 'turtle', 'bat', 'zebra', 'leopard']
    }
  },
  {
    code: 'PLANTS', emoji: '🌷',
    tr: {
      label: 'Bitkiler ve Çiçekler',
      words: ['papatya', 'lale', 'gül', 'orkide', 'karanfil', 'menekşe', 'nergis', 'zambak',
        'sardunya', 'kaktüs', 'çınar', 'meşe', 'söğüt', 'kavak', 'ladin', 'yosun',
        'sarmaşık', 'yonca', 'lavanta', 'eğrelti']
    },
    en: {
      label: 'Plants & Flowers',
      words: ['daisy', 'tulip', 'rose', 'orchid', 'carnation', 'violet', 'daffodil', 'lily',
        'cactus', 'maple', 'oak', 'willow', 'poplar', 'spruce', 'moss', 'ivy',
        'clover', 'lavender', 'mushroom', 'fern']
    }
  },
  {
    code: 'FOOD', emoji: '🍔',
    tr: {
      label: 'Yiyecek ve İçecek',
      words: ['çorba', 'pilav', 'köfte', 'kebap', 'börek', 'mantı', 'simit', 'poğaça',
        'baklava', 'dondurma', 'çikolata', 'sandviç', 'makarna', 'peynir', 'zeytin',
        'reçel', 'ayran', 'kahve', 'limonata', 'şerbet']
    },
    en: {
      label: 'Food & Drink',
      words: ['soup', 'rice', 'meatball', 'kebab', 'pastry', 'dumpling', 'bagel', 'pancake',
        'dessert', 'chocolate', 'sandwich', 'pasta', 'cheese', 'olive', 'honey',
        'yogurt', 'coffee', 'lemonade', 'sorbet', 'bread']
    }
  },
  {
    code: 'FRUITS_VEGETABLES', emoji: '🍎',
    tr: {
      label: 'Meyve ve Sebze',
      words: ['elma', 'armut', 'çilek', 'karpuz', 'kavun', 'kiraz', 'muz', 'portakal',
        'mandalina', 'şeftali', 'erik', 'incir', 'üzüm', 'domates', 'salatalık',
        'patlıcan', 'biber', 'havuç', 'ıspanak', 'brokoli']
    },
    en: {
      label: 'Fruits & Vegetables',
      words: ['apple', 'pear', 'strawberry', 'watermelon', 'melon', 'cherry', 'banana',
        'orange', 'tangerine', 'peach', 'plum', 'fig', 'grape', 'tomato', 'cucumber',
        'eggplant', 'pepper', 'carrot', 'spinach', 'broccoli']
    }
  },
  {
    code: 'COUNTRIES', emoji: '🌍',
    tr: {
      label: 'Ülkeler',
      words: ['türkiye', 'almanya', 'fransa', 'ispanya', 'italya', 'portekiz', 'hollanda',
        'belçika', 'isviçre', 'avusturya', 'yunanistan', 'macaristan', 'polonya',
        'japonya', 'çin', 'hindistan', 'brezilya', 'arjantin', 'kanada', 'fas']
    },
    en: {
      label: 'Countries',
      words: ['turkey', 'germany', 'france', 'spain', 'italy', 'portugal', 'netherlands',
        'belgium', 'switzerland', 'austria', 'greece', 'hungary', 'poland', 'japan',
        'china', 'india', 'brazil', 'argentina', 'canada', 'morocco']
    }
  },
  {
    code: 'CITIES', emoji: '🏙️',
    tr: {
      label: 'Şehirler',
      words: ['istanbul', 'ankara', 'izmir', 'bursa', 'antalya', 'konya', 'trabzon', 'adana',
        'eskişehir', 'samsun', 'kayseri', 'mardin', 'erzurum', 'çanakkale',
        'diyarbakır', 'malatya', 'sivas', 'rize', 'aydın', 'muğla']
    },
    en: {
      label: 'Cities',
      words: ['istanbul', 'ankara', 'london', 'paris', 'berlin', 'madrid', 'rome', 'vienna',
        'prague', 'lisbon', 'amsterdam', 'dublin', 'oslo', 'helsinki', 'warsaw',
        'budapest', 'tokyo', 'sydney', 'toronto', 'cairo']
    }
  },
  {
    code: 'NATURE', emoji: '🏔️',
    tr: {
      label: 'Doğa ve Coğrafya',
      words: ['dağ', 'deniz', 'orman', 'çöl', 'ada', 'nehir', 'şelale', 'vadi', 'mağara',
        'buzul', 'volkan', 'göl', 'kanyon', 'yayla', 'körfez', 'plaj', 'bataklık',
        'tepe', 'kayalık', 'ırmak']
    },
    en: {
      label: 'Nature & Geography',
      words: ['mountain', 'ocean', 'forest', 'desert', 'island', 'river', 'waterfall',
        'valley', 'cave', 'glacier', 'volcano', 'lake', 'canyon', 'plateau', 'bay',
        'beach', 'swamp', 'hill', 'cliff', 'meadow']
    }
  },
  {
    code: 'SPACE', emoji: '🚀',
    tr: {
      label: 'Uzay',
      words: ['gezegen', 'yıldız', 'güneş', 'gökada', 'meteor', 'uydu', 'teleskop',
        'astronot', 'roket', 'yörünge', 'karadelik', 'evren', 'mars', 'satürn',
        'jüpiter', 'venüs', 'takımyıldız', 'asteroit', 'kuyrukluyıldız', 'gökbilim']
    },
    en: {
      label: 'Space',
      words: ['planet', 'star', 'sun', 'galaxy', 'meteor', 'satellite', 'telescope',
        'astronaut', 'rocket', 'orbit', 'eclipse', 'universe', 'mars', 'saturn',
        'jupiter', 'venus', 'constellation', 'asteroid', 'comet', 'nebula']
    }
  },
  {
    code: 'WEATHER', emoji: '🌦️',
    tr: {
      label: 'Hava ve Mevsimler',
      words: ['yağmur', 'kar', 'rüzgar', 'fırtına', 'sis', 'dolu', 'gökkuşağı', 'şimşek',
        'yıldırım', 'bulut', 'ayaz', 'sıcaklık', 'kuraklık', 'ilkbahar', 'yaz',
        'sonbahar', 'kış', 'çiy', 'kırağı', 'tipi']
    },
    en: {
      label: 'Weather & Seasons',
      words: ['rain', 'snow', 'wind', 'storm', 'fog', 'hail', 'rainbow', 'lightning',
        'thunder', 'cloud', 'frost', 'humidity', 'drought', 'spring', 'summer',
        'autumn', 'winter', 'breeze', 'blizzard', 'sunshine']
    }
  },
  {
    code: 'PROFESSIONS', emoji: '👷',
    tr: {
      label: 'Meslekler',
      words: ['doktor', 'hemşire', 'öğretmen', 'mühendis', 'avukat', 'hakim', 'pilot',
        'kaptan', 'aşçı', 'garson', 'marangoz', 'terzi', 'berber', 'çiftçi',
        'itfaiyeci', 'polis', 'eczacı', 'mimar', 'muhasebeci', 'veteriner']
    },
    en: {
      label: 'Professions',
      words: ['doctor', 'nurse', 'teacher', 'engineer', 'lawyer', 'judge', 'pilot',
        'captain', 'chef', 'waiter', 'carpenter', 'tailor', 'barber', 'farmer',
        'firefighter', 'police', 'pharmacist', 'architect', 'accountant', 'veterinarian']
    }
  },
  {
    code: 'SPORTS', emoji: '⚽',
    tr: {
      label: 'Spor',
      words: ['futbol', 'basketbol', 'voleybol', 'tenis', 'yüzme', 'güreş', 'boks',
        'atletizm', 'hentbol', 'kayak', 'bisiklet', 'halter', 'jimnastik', 'okçuluk',
        'eskrim', 'binicilik', 'yelken', 'kürek', 'judo', 'maraton']
    },
    en: {
      label: 'Sports',
      words: ['football', 'basketball', 'volleyball', 'tennis', 'swimming', 'wrestling',
        'boxing', 'athletics', 'handball', 'skiing', 'cycling', 'weightlifting',
        'gymnastics', 'archery', 'fencing', 'riding', 'sailing', 'rowing', 'judo',
        'marathon']
    }
  },
  {
    code: 'MUSIC', emoji: '🎵',
    tr: {
      label: 'Müzik',
      words: ['gitar', 'piyano', 'keman', 'davul', 'flüt', 'saksafon', 'trompet', 'arp',
        'akordeon', 'bağlama', 'ney', 'kanun', 'org', 'melodi', 'ritim', 'nota',
        'akor', 'orkestra', 'senfoni', 'koro']
    },
    en: {
      label: 'Music',
      words: ['guitar', 'piano', 'violin', 'drum', 'flute', 'saxophone', 'trumpet', 'harp',
        'accordion', 'banjo', 'cello', 'clarinet', 'organ', 'melody', 'rhythm',
        'note', 'chord', 'orchestra', 'symphony', 'choir']
    }
  },
  {
    code: 'CINEMA', emoji: '🎬',
    tr: {
      label: 'Sinema ve Dizi',
      words: ['sinema', 'oyuncu', 'yönetmen', 'senaryo', 'sahne', 'kamera', 'kurgu', 'gişe',
        'fragman', 'altyazı', 'dizi', 'bölüm', 'karakter', 'komedi', 'korku',
        'gerilim', 'animasyon', 'belgesel', 'festival', 'ödül']
    },
    en: {
      label: 'Movies & TV',
      words: ['cinema', 'actor', 'director', 'script', 'scene', 'camera', 'editing',
        'trailer', 'subtitle', 'series', 'episode', 'character', 'comedy', 'horror',
        'thriller', 'animation', 'documentary', 'festival', 'award', 'premiere']
    }
  },
  {
    code: 'LITERATURE', emoji: '📚',
    tr: {
      label: 'Edebiyat',
      words: ['roman', 'şiir', 'öykü', 'deneme', 'tiyatro', 'destan', 'masal', 'yazar',
        'şair', 'kitap', 'kütüphane', 'sayfa', 'bölüm', 'kahraman', 'dize', 'kafiye',
        'mecaz', 'benzetme', 'yayınevi', 'çeviri']
    },
    en: {
      label: 'Literature',
      words: ['novel', 'poem', 'story', 'essay', 'theatre', 'epic', 'fable', 'author',
        'poet', 'book', 'library', 'page', 'chapter', 'hero', 'verse', 'rhyme',
        'metaphor', 'simile', 'publisher', 'translation']
    }
  },
  {
    code: 'ART', emoji: '🎨',
    tr: {
      label: 'Sanat',
      words: ['resim', 'heykel', 'tuval', 'fırça', 'boya', 'palet', 'desen', 'portre',
        'manzara', 'sergi', 'galeri', 'müze', 'seramik', 'çömlek', 'mozaik',
        'minyatür', 'hat', 'ebru', 'fotoğraf', 'gravür']
    },
    en: {
      label: 'Art',
      words: ['painting', 'sculpture', 'canvas', 'brush', 'paint', 'palette', 'pattern',
        'portrait', 'landscape', 'exhibition', 'gallery', 'museum', 'ceramic',
        'pottery', 'mosaic', 'miniature', 'calligraphy', 'etching', 'photograph',
        'sketch']
    }
  },
  {
    code: 'TECHNOLOGY', emoji: '💻',
    tr: {
      label: 'Teknoloji',
      words: ['bilgisayar', 'klavye', 'ekran', 'fare', 'yazıcı', 'tarayıcı', 'telefon',
        'tablet', 'kulaklık', 'hoparlör', 'kamera', 'batarya', 'işlemci', 'bellek',
        'anakart', 'modem', 'yönlendirici', 'kablo', 'robot', 'dron']
    },
    en: {
      label: 'Technology',
      words: ['computer', 'keyboard', 'screen', 'mouse', 'printer', 'scanner', 'phone',
        'tablet', 'headphone', 'speaker', 'camera', 'battery', 'processor', 'memory',
        'motherboard', 'modem', 'router', 'cable', 'robot', 'drone']
    }
  },
  {
    code: 'SOFTWARE', emoji: '⌨️',
    tr: {
      label: 'Yazılım',
      words: ['kod', 'yazılım', 'donanım', 'fonksiyon', 'değişken', 'algoritma', 'döngü',
        'dizi', 'sınıf', 'nesne', 'arayüz', 'kütüphane', 'derleyici', 'hata', 'sürüm',
        'depo', 'dal', 'birleştirme', 'sunucu', 'veritabanı']
    },
    en: {
      label: 'Software',
      words: ['code', 'software', 'hardware', 'function', 'variable', 'algorithm', 'loop',
        'array', 'class', 'object', 'interface', 'library', 'compiler', 'debug',
        'version', 'repository', 'branch', 'merge', 'server', 'database']
    }
  },
  {
    code: 'SCRUM', emoji: '🌀',
    tr: {
      label: 'Scrum ve Çeviklik',
      words: ['sprint', 'çeviklik', 'takım', 'pano', 'birikim', 'öncelik', 'hedef',
        'planlama', 'tahmin', 'değerlendirme', 'retrospektif', 'kanban', 'hız',
        'kapasite', 'engel', 'teslimat', 'sürüm', 'geribildirim', 'toplantı', 'paydaş']
    },
    en: {
      label: 'Scrum & Agile',
      words: ['sprint', 'agile', 'team', 'board', 'backlog', 'priority', 'goal', 'planning',
        'estimate', 'review', 'retrospective', 'kanban', 'velocity', 'capacity',
        'blocker', 'delivery', 'release', 'feedback', 'standup', 'stakeholder']
    }
  },
  {
    code: 'OFFICE', emoji: '💼',
    tr: {
      label: 'Ofis ve İş Dünyası',
      words: ['toplantı', 'sunum', 'rapor', 'bütçe', 'fatura', 'sözleşme', 'müşteri',
        'tedarikçi', 'pazarlama', 'satış', 'strateji', 'hedef', 'verimlilik', 'terfi',
        'maaş', 'izin', 'mesai', 'departman', 'yönetici', 'proje']
    },
    en: {
      label: 'Office & Business',
      words: ['meeting', 'presentation', 'report', 'budget', 'invoice', 'contract', 'client',
        'supplier', 'marketing', 'sales', 'strategy', 'target', 'productivity',
        'promotion', 'salary', 'leave', 'overtime', 'department', 'manager', 'project']
    }
  },
  {
    code: 'SCIENCE', emoji: '🔬',
    tr: {
      label: 'Bilim',
      words: ['kimya', 'fizik', 'biyoloji', 'matematik', 'deney', 'laboratuvar', 'mikroskop',
        'atom', 'molekül', 'hücre', 'enerji', 'yerçekimi', 'manyetizma', 'elektrik',
        'denklem', 'teori', 'hipotez', 'element', 'genetik', 'evrim']
    },
    en: {
      label: 'Science',
      words: ['chemistry', 'physics', 'biology', 'mathematics', 'experiment', 'laboratory',
        'microscope', 'atom', 'molecule', 'cell', 'energy', 'gravity', 'magnetism',
        'electricity', 'equation', 'theory', 'hypothesis', 'element', 'genetics',
        'evolution']
    }
  },
  {
    code: 'HEALTH', emoji: '🏥',
    tr: {
      label: 'Sağlık ve Tıp',
      words: ['hastane', 'doktor', 'hemşire', 'ameliyat', 'reçete', 'ilaç', 'aşı', 'vitamin',
        'bağışıklık', 'ateş', 'grip', 'alerji', 'röntgen', 'tansiyon', 'nabız',
        'teşhis', 'tedavi', 'ambulans', 'eczane', 'muayene']
    },
    en: {
      label: 'Health & Medicine',
      words: ['hospital', 'doctor', 'nurse', 'surgery', 'prescription', 'medicine', 'vaccine',
        'vitamin', 'immunity', 'fever', 'influenza', 'allergy', 'radiology', 'pressure',
        'pulse', 'diagnosis', 'treatment', 'ambulance', 'pharmacy', 'checkup']
    }
  },
  {
    code: 'BODY', emoji: '🫀',
    tr: {
      label: 'İnsan Vücudu',
      words: ['kalp', 'akciğer', 'karaciğer', 'böbrek', 'beyin', 'mide', 'damar', 'kemik',
        'kas', 'omurga', 'dirsek', 'bilek', 'parmak', 'topuk', 'kaburga', 'göz',
        'kulak', 'burun', 'dudak', 'kaş']
    },
    en: {
      label: 'Human Body',
      words: ['heart', 'lung', 'liver', 'kidney', 'brain', 'stomach', 'vein', 'bone',
        'muscle', 'spine', 'elbow', 'wrist', 'finger', 'heel', 'rib', 'eye', 'ear',
        'nose', 'lip', 'eyebrow']
    }
  },
  {
    code: 'CLOTHING', emoji: '👕',
    tr: {
      label: 'Giyim',
      words: ['gömlek', 'pantolon', 'ceket', 'palto', 'kazak', 'tişört', 'etek', 'elbise',
        'çorap', 'ayakkabı', 'bot', 'terlik', 'şapka', 'atkı', 'eldiven', 'kemer',
        'kravat', 'yelek', 'mont', 'eşofman']
    },
    en: {
      label: 'Clothing',
      words: ['shirt', 'trousers', 'jacket', 'coat', 'sweater', 'blouse', 'skirt', 'dress',
        'socks', 'shoes', 'boots', 'slippers', 'hat', 'scarf', 'gloves', 'belt',
        'tie', 'vest', 'jeans', 'hoodie']
    }
  },
  {
    code: 'HOME', emoji: '🏠',
    tr: {
      label: 'Ev ve Eşyalar',
      words: ['mutfak', 'salon', 'yatak', 'koltuk', 'masa', 'sandalye', 'dolap', 'halı',
        'perde', 'ayna', 'lamba', 'avize', 'buzdolabı', 'fırın', 'tencere', 'tabak',
        'bardak', 'çatal', 'kaşık', 'yastık']
    },
    en: {
      label: 'Home & Furniture',
      words: ['kitchen', 'lounge', 'bedroom', 'sofa', 'table', 'chair', 'wardrobe', 'carpet',
        'curtain', 'mirror', 'lamp', 'chandelier', 'fridge', 'oven', 'pot', 'plate',
        'fork', 'spoon', 'pillow', 'blanket']
    }
  },
  {
    code: 'TRANSPORT', emoji: '🚗',
    tr: {
      label: 'Ulaşım ve Taşıtlar',
      words: ['otobüs', 'tramvay', 'metro', 'vapur', 'uçak', 'tren', 'bisiklet', 'motosiklet',
        'kamyon', 'taksi', 'helikopter', 'yelkenli', 'gemi', 'feribot', 'araba',
        'karavan', 'römork', 'iskele', 'havalimanı', 'istasyon']
    },
    en: {
      label: 'Transport & Vehicles',
      words: ['bus', 'tram', 'metro', 'ferry', 'airplane', 'train', 'bicycle', 'motorcycle',
        'truck', 'taxi', 'helicopter', 'sailboat', 'ship', 'yacht', 'car', 'caravan',
        'trailer', 'harbor', 'airport', 'station']
    }
  },
  {
    code: 'SCHOOL', emoji: '🎓',
    tr: {
      label: 'Okul ve Eğitim',
      words: ['okul', 'öğrenci', 'öğretmen', 'sınıf', 'ders', 'tahta', 'defter', 'kalem',
        'silgi', 'çanta', 'sınav', 'ödev', 'karne', 'teneffüs', 'kütüphane',
        'laboratuvar', 'müdür', 'zil', 'harita', 'diploma']
    },
    en: {
      label: 'School & Education',
      words: ['school', 'student', 'teacher', 'classroom', 'lesson', 'board', 'notebook',
        'pencil', 'eraser', 'backpack', 'exam', 'homework', 'report', 'recess',
        'library', 'laboratory', 'principal', 'bell', 'map', 'diploma']
    }
  },
  {
    code: 'MYTHOLOGY', emoji: '🐉',
    tr: {
      label: 'Mitoloji ve Masal',
      words: ['ejderha', 'büyücü', 'peri', 'dev', 'cüce', 'cadı', 'kahraman', 'kılıç',
        'kalkan', 'hazine', 'krallık', 'şato', 'prenses', 'tılsım', 'iksir', 'lanet',
        'kehanet', 'efsane', 'canavar', 'tanrıça']
    },
    en: {
      label: 'Myth & Fantasy',
      words: ['dragon', 'wizard', 'fairy', 'giant', 'dwarf', 'witch', 'hero', 'sword',
        'shield', 'treasure', 'kingdom', 'castle', 'princess', 'amulet', 'potion',
        'curse', 'prophecy', 'legend', 'monster', 'goddess']
    }
  },
  {
    code: 'HOBBIES', emoji: '🎲',
    tr: {
      label: 'Hobi ve Oyun',
      words: ['satranç', 'tavla', 'yapboz', 'bulmaca', 'origami', 'fotoğrafçılık', 'resim',
        'bahçıvanlık', 'koleksiyon', 'kamp', 'balıkçılık', 'dalış', 'tırmanış', 'dans',
        'örgü', 'seramik', 'yürüyüş', 'bisiklet', 'kitap', 'günlük']
    },
    en: {
      label: 'Hobbies & Games',
      words: ['chess', 'backgammon', 'puzzle', 'crossword', 'origami', 'photography',
        'painting', 'gardening', 'collecting', 'camping', 'fishing', 'diving',
        'climbing', 'dancing', 'knitting', 'pottery', 'hiking', 'cycling', 'reading',
        'journaling']
    }
  }
]

/** Kategori listesi: [{ code, emoji, tr, en }] */
export const HANGMAN_CATEGORIES = CATEGORY_DATA.map(c => ({
  code: c.code,
  emoji: c.emoji,
  tr: c.tr.label,
  en: c.en.label
}))

/** { tr: { ANIMALS: [...], ... }, en: { ... } } */
export const HANGMAN_WORDS = {
  tr: Object.fromEntries(CATEGORY_DATA.map(c => [c.code, c.tr.words])),
  en: Object.fromEntries(CATEGORY_DATA.map(c => [c.code, c.en.words]))
}

/** Dile göre kategori seçenekleri — arayüzde doğrudan kullanılır. */
export function hangmanCategoryOptions(lang = 'tr') {
  return HANGMAN_CATEGORIES.map(c => ({
    code: c.code,
    emoji: c.emoji,
    label: lang === 'en' ? c.en : c.tr
  }))
}

/** Tek bir kategorinin görünen adı; bilinmeyen kod için null. */
export function hangmanCategoryLabel(code, lang = 'tr') {
  const found = HANGMAN_CATEGORIES.find(c => c.code === code)
  return found ? `${found.emoji} ${lang === 'en' ? found.en : found.tr}` : null
}

/**
 * Kategoriye (ya da kategori verilmezse tüm havuza) ait kelimeler.
 * @param {'tr'|'en'} lang
 * @param {string|null} category
 */
export function hangmanWordsOf(lang, category = null) {
  const byCategory = HANGMAN_WORDS[lang] || HANGMAN_WORDS.tr
  if (category) return byCategory[category] || []
  return Object.values(byCategory).flat()
}

/**
 * Verilen dil/kategori için rastgele bir kelime döndürür.
 * @param {'tr'|'en'} lang
 * @param {object} [opts]
 * @param {string|null} [opts.category] null ise tüm kategorilerden seçilir
 * @param {string} [opts.exclude] tekrar aynı kelimeyi almamak için hariç tutulacak kelime
 * @param {string[]} [opts.extraWords] havuza eklenecek (DB'den gelen) kelimeler
 */
export function randomHangmanWord(lang, { category = null, exclude = null, extraWords = [] } = {}) {
  const base = hangmanWordsOf(lang, category)
  const pool = [...new Set([...base, ...extraWords])]
  if (!pool.length) return ''
  if (pool.length === 1) return pool[0]
  let word
  do {
    word = pool[Math.floor(Math.random() * pool.length)]
  } while (word === exclude)
  return word
}
