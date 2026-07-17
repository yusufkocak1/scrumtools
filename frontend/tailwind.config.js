import withMT from "@material-tailwind/html/utils/withMT";
import twColors from "tailwindcss/colors";

module.exports = withMT({
    darkMode: 'class',
    content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}",],
    theme: {
        // xs breakpoint mobilde buton metni kısaltmaları için kullanılıyor
        screens: {
            xs: '480px',
            sm: '640px',
            md: '768px',
            lg: '1024px',
            xl: '1280px',
            '2xl': '1536px',
        },
        extend: {
            // withMT varsayılan Tailwind paletini Material paletiyle değiştiriyor;
            // tasarımda kullanılan ama Material palette olmayan renkleri geri ekliyoruz
            colors: {
                emerald: twColors.emerald,
                rose: twColors.rose,
                sky: twColors.sky,
                violet: twColors.violet,
                fuchsia: twColors.fuchsia,
            },
        },
    },
    plugins: [require('@tailwindcss/typography')],

});