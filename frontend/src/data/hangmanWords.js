/**
 * Adam Asmaca (Hangman) kelime havuzları.
 * Kelimeler küçük harfle, boşluksuz tutulur; ekranda gösterirken
 * Türkçe'ye duyarlı (İ/ı) büyük harfe çevrilir.
 */

export const HANGMAN_WORDS = {
  tr: [
    // Genel
    'bilgisayar', 'klavye', 'yazılım', 'donanım', 'geliştirici', 'takım', 'oyun', 'kahve',
    'dağ', 'deniz', 'orman', 'çöl', 'ada', 'nehir', 'köprü', 'kale', 'bahçe', 'mutfak',
    'yatak', 'kütüphane', 'hastane', 'havalimanı', 'istasyon', 'restoran', 'bisiklet',
    'uçak', 'yelkenli', 'fil', 'zürafa', 'penguen', 'yunus', 'kelebek', 'sincap', 'kanguru',
    'çikolata', 'ananas', 'çilek', 'sandviç', 'şemsiye', 'teleskop', 'mikroskop', 'kimya',
    'coğrafya', 'tarih', 'matematik', 'bilim', 'resim', 'heykel', 'orkestra', 'senfoni',
    'macera', 'hazine', 'gizem', 'ejderha', 'büyücü', 'krallık', 'özgürlük', 'cesaret',
    'bilgelik', 'uyum', 'güneş', 'gökkuşağı', 'yıldırım', 'kartopu', 'şelale', 'yıldız',
    'gemi', 'roket', 'volkan', 'buzul', 'çiftlik', 'değirmen', 'fener', 'liman',
    // Scrum & yazılım temalı
    'sprint', 'koşu', 'pano', 'öncelik', 'hedef', 'planlama', 'tahmin', 'sürüm',
    'hata', 'özellik', 'test', 'sunucu', 'tarayıcı', 'fonksiyon', 'değişken', 'algoritma',
    'terminal', 'ekran', 'yazıcı', 'şifre', 'kullanıcı', 'takvim', 'toplantı', 'geribildirim'
  ],
  en: [
    // General
    'computer', 'keyboard', 'developer', 'teamwork', 'coffee', 'mountain', 'ocean', 'forest',
    'desert', 'island', 'river', 'bridge', 'castle', 'garden', 'kitchen', 'bedroom', 'library',
    'hospital', 'airport', 'station', 'restaurant', 'bicycle', 'airplane', 'sailboat',
    'elephant', 'giraffe', 'penguin', 'dolphin', 'butterfly', 'squirrel', 'kangaroo',
    'chocolate', 'pineapple', 'strawberry', 'sandwich', 'umbrella', 'telescope', 'microscope',
    'chemistry', 'geography', 'history', 'mathematics', 'science', 'painting', 'sculpture',
    'orchestra', 'symphony', 'adventure', 'treasure', 'mystery', 'dragon', 'wizard', 'kingdom',
    'freedom', 'courage', 'wisdom', 'harmony', 'sunshine', 'thunder', 'lightning', 'snowflake',
    'waterfall', 'rocket', 'volcano', 'glacier', 'lighthouse', 'harbor', 'windmill', 'rainbow',
    // Scrum & software themed
    'sprint', 'backlog', 'kanban', 'meeting', 'deadline', 'feedback', 'strategy', 'priority',
    'velocity', 'standup', 'planning', 'estimate', 'release', 'version', 'feature', 'testing',
    'deploy', 'server', 'browser', 'function', 'variable', 'algorithm', 'framework', 'terminal'
  ]
}

/**
 * Verilen dil için rastgele bir kelime döndürür.
 * @param {'tr'|'en'} lang
 * @param {string} [exclude] tekrar aynı kelimeyi almamak için hariç tutulacak kelime
 * @param {string[]} [extraWords] dahili havuza eklenecek takıma özel kelimeler
 */
export function randomHangmanWord(lang, exclude = null, extraWords = []) {
  const base = HANGMAN_WORDS[lang] || HANGMAN_WORDS.tr
  const pool = extraWords.length ? [...new Set([...base, ...extraWords])] : base
  if (pool.length === 1) return pool[0]
  let word
  do {
    word = pool[Math.floor(Math.random() * pool.length)]
  } while (word === exclude)
  return word
}
