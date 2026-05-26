var i18n = {
    currentLang: localStorage.getItem('cambiz_lang') || 'en',
    translations: {
        en: { sign_out: 'Sign Out' },
        fr: { sign_out: 'Déconnexion' }
    },
    t: function(key) { return (this.translations[this.currentLang] && this.translations[this.currentLang][key]) ? this.translations[this.currentLang][key] : key; },
    setLang: function(lang) { this.currentLang = lang; localStorage.setItem('cambiz_lang', lang); location.reload(); },
    createSwitcher: function() { return '<select onchange="i18n.setLang(this.value)" style="padding:6px 10px;border-radius:6px;border:1px solid #e2e8f0;font-size:0.85rem"><option value="en"' + (this.currentLang === 'en' ? ' selected' : '') + '>EN</option><option value="fr"' + (this.currentLang === 'fr' ? ' selected' : '') + '>FR</option></select>'; }
};
