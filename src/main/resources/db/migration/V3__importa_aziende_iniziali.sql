-- Aziende iniziali di Barizone, importate dal vecchio archivio (backend Python).
-- Sono inserite una sola volta: da qui in poi si gestiscono dall'area admin o dal database,
-- e questa migrazione non le ripristina né le sovrascrive (ON CONFLICT DO NOTHING).
--
-- DATI DA COMPLETARE dall'admin: partita IVA, ragione sociale, codice fiscale, sede legale, CAP, PEC
-- non erano presenti nel vecchio archivio. Le schede sono pubblicate ma non ancora "verificate"
-- e senza contratto attivo. Promozioni e piano provengono dal vecchio archivio e vanno
-- confermati con le attività.
INSERT INTO company_profiles
    (id, business_name, owner_name, email, phone, category, quartiere, address, description, promotion, plan,
     city, province, status, verified, contract_active, notifications_enabled, updated_at)
SELECT v.id, v.business_name, v.owner_name, v.email, v.phone, v.category, v.quartiere, v.address,
       v.description, v.promotion, v.plan, 'Bari', 'BA', 'published', FALSE, FALSE, FALSE, now()
FROM (VALUES
    ('b0000000-0000-4000-8000-000000000001', 'Sartoria Miglionico', 'Giovanni Miglionico', 'info@sartoriamiglionico.it', '+39 080 521 8899', 'Moda & Sartoria', 'Murat', 'Via Sparano 89', 'Sartoria artigianale dal 1962. Abiti su misura per uomo e cerimonie.', '10% di sconto sul primo abito su misura', 'Quartiere VIP'),
    ('b0000000-0000-4000-8000-000000000002', 'Caffè del Corso', 'Anna Ricciardi', 'info@caffedelcorso.it', '+39 080 523 4455', 'Bar & Caffetteria', 'Murat', 'Corso Cavour 122', 'Caffè storico e pasticceria in stile liberty. Aperitivo con vista sul teatro Piccinni.', 'Aperitivo a €7 fino alle 20:00', 'Gratuito'),
    ('b0000000-0000-4000-8000-000000000003', 'Libreria Roma', 'Marco Palmieri', 'libreriaroma@bari.it', '+39 080 521 4433', 'Cultura & Libri', 'Libertà', 'Via Manzoni 220', 'Libreria indipendente con reparto ragazzi e presentazioni ogni giovedì.', 'Membership annuale a €12 con eventi inclusi', 'Gratuito'),
    ('b0000000-0000-4000-8000-000000000004', 'Beauty Studio Poggiofranco', 'Chiara Gallo', 'chiara@beautystudio.it', '+39 080 553 1122', 'Bellezza & Benessere', 'Poggiofranco', 'Via Camillo Rosalba 47', 'Centro estetico avanzato con trattamenti coreani e viso su misura.', 'Consulenza gratuita + 15% sul primo trattamento', 'Top Banner'),
    ('b0000000-0000-4000-8000-000000000005', 'Ciclofficina Japigia', 'Luca Petronio', 'info@ciclofficinajapigia.it', '+39 080 543 7788', 'Sport & Bici', 'Japigia', 'Via Peucetia 55', 'Riparazione e-bike e vendita bici artigianali. Community rides ogni domenica.', 'Tagliando bici a €25 (invece di €40)', 'Quartiere VIP'),
    ('b0000000-0000-4000-8000-000000000006', 'Osteria del Mare Torre', 'Francesco Lopez', 'torre@osteriamare.it', '+39 080 543 9911', 'Ristorazione', 'Torre a Mare', 'Via Andrea Milella 12', 'Cucina di mare familiare. Antipasto della casa con 8 assaggi.', 'Menu degustazione a €35 (bevande incluse)', 'Gratuito')
) AS v(id, business_name, owner_name, email, phone, category, quartiere, address, description, promotion, plan)
ON CONFLICT (id) DO NOTHING;
