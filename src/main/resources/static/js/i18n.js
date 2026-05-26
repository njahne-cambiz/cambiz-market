var i18n = {
    currentLang: localStorage.getItem('cambiz_lang') || 'en',
    translations: {
        en: { dashboard: 'Dashboard', users: 'Users', sellers: 'Sellers', categories: 'Categories', revenue: 'Revenue', payments: 'Payments', products: 'Products', orders: 'Orders', transactions: 'Transactions', disputes: 'Disputes', reviews: 'Reviews', settings: 'Settings', health: 'Health', sign_out: 'Sign Out' },
        fr: { dashboard: 'Tableau de Bord', users: 'Utilisateurs', sellers: 'Vendeurs', categories: 'Catégories', revenue: 'Revenus', payments: 'Paiements', products: 'Produits', orders: 'Commandes', transactions: 'Transactions', disputes: 'Litiges', reviews: 'Avis', settings: 'Paramètres', health: 'Santé', sign_out: 'Déconnexion' }
    },
    t: function(key) { return this.translations[this.currentLang] && this.translations[this.currentLang][key] ? this.translations[this.currentLang][key] : key; },
    setLang: function(lang) { this.currentLang = lang; localStorage.setItem('cambiz_lang', lang); location.reload(); },
    createSwitcher: function() { return '<select class="lang-selector form-select form-select-sm" style="width:auto;display:inline-block" onchange="i18n.setLang(this.value)"><option value="en" '+(this.currentLang==='en'?'selected':'')+'>🇬🇧 EN</option><option value="fr" '+(this.currentLang==='fr'?'selected':'')+'>🇫🇷 FR</option></select>'; }
};
