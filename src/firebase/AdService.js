import {doc, onSnapshot} from "firebase/firestore";
import {db} from "./Firebase.js";

// Reklam ayarlari Firestore'da "siteConfig/adBanner" dokumaninda tutulur.
// Dokuman Firebase Console uzerinden duzenlenir; uygulama sadece okur.
// Dokuman yoksa veya bir alan eksikse asagidaki varsayilanlar kullanilir.
export const DEFAULT_AD_CONFIG = {
    enabled: true,
    title: "🐝 Koçak Bal 🍯",
    subtitle: "Doğal ve Organik Bal Ürünleri",
    phone: "+90 531 604 51 95",
    sponsoredTooltip: "Bu alana reklam vermek için iletişime geçin",
    footerNotes: [
        "* Stok durumu tahminidir ve gerçek zamanlı güncellenmektedir.",
        "* Fiyat bilgisi için lütfen iletişime geçiniz."
    ],
    products: [
        {
            name: "Kara Kovan Balı",
            description: "Geleneksel yöntemlerle üretilen",
            emoji: "🍯",
            tooltip: "Sınırlı stok sebebiyle eylül ayı öncesi ön sipariş talebinde bulunulmalı",
            stock: 0,
            maxStock: 0,
            unit: "kg",
            autoDecrease: false
        },
        {
            name: "Süzme Bal",
            description: "Doğal süzme bal",
            emoji: "🍯",
            tooltip: "",
            stock: 1000,
            maxStock: 1000,
            unit: "kg",
            autoDecrease: true
        },
        {
            name: "Çıta Bal",
            description: "Özel çıta balı",
            emoji: "🍯",
            tooltip: "",
            stock: 300,
            maxStock: 300,
            unit: "Çıta",
            autoDecrease: true
        }
    ]
};

const listenAdConfig = (callback) => {
    return onSnapshot(doc(db, "siteConfig", "adBanner"), (snap) => {
        callback(snap.exists() ? {...DEFAULT_AD_CONFIG, ...snap.data()} : DEFAULT_AD_CONFIG);
    }, (error) => {
        console.error("Reklam ayarları alınamadı:", error);
        callback(DEFAULT_AD_CONFIG);
    });
};

export {listenAdConfig}
