import withMT from "@material-tailwind/html/utils/withMT";

module.exports = withMT({
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
        extend: {},
    },
    plugins: [],

});