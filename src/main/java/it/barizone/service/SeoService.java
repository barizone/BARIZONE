package it.barizone.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import it.barizone.configuration.BarizoneProperties;
import it.barizone.model.enums.CompanyStatus;
import it.barizone.model.enums.NewsStatus;
import it.barizone.repository.CompanyProfileRepository;
import it.barizone.repository.NewsRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
public class SeoService {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/", "/notizie", "/archivio", "/eventi", "/farmacie", "/aziende", "/quartieri", "/promuovi", "/iscriviti-news");

    private final NewsRepository news;
    private final CompanyProfileRepository companies;
    private final BarizoneProperties properties;

    public SeoService(NewsRepository news, CompanyProfileRepository companies, BarizoneProperties properties) {
        this.news = news;
        this.companies = companies;
        this.properties = properties;
    }

    public String robots() {
        return "User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /admin\nDisallow: /login\n"
                + "Disallow: /gestione-clienti\nDisallow: /esercente\nDisallow: /adesioni\n"
                + "Disallow: /accedi\nDisallow: /profilo\nDisallow: /area-attivita\nDisallow: /verifica-email\nDisallow: /reimposta-password\n\n"
                + "Sitemap: " + origin() + "/sitemap.xml\n";
    }

    @Transactional(readOnly = true)
    public String sitemap() {
        Set<String> urls = new LinkedHashSet<>();
        PUBLIC_PATHS.forEach(p -> urls.add(origin() + p));
        Instant now = Instant.now();
        news.findByStatusInOrderByPublishedAtDesc(EnumSet.of(NewsStatus.published, NewsStatus.archived), Limit.unlimited())
                .stream()
                .filter(n -> n.getPublishedAt() == null || !n.getPublishedAt().isAfter(now))
                .filter(n -> n.getSourceUrl() != null && !n.getSourceUrl().isBlank())
                .forEach(n -> urls.add(origin() + "/notizie/" + encode(n.getId())));
        companies.findByStatusOrderByUpdatedAtDesc(CompanyStatus.published).stream()
                .forEach(c -> urls.add(origin() + "/aziende/" + encode(c.getId())));
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        urls.forEach(u -> xml.append("  <url><loc>").append(escapeXml(u)).append("</loc></url>\n"));
        return xml.append("</urlset>\n").toString();
    }

    private String origin() {
        return properties.trustedOrigins().iterator().next();
    }

    private static String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
