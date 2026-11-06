package it.barizone.model.enums;

import java.util.List;

/** Categorie editoriali ammesse. "Territorio" e "Mobilità" sono usate dalle notizie redazionali. */
public final class NewsCategory {

    public static final List<String> VALUES = List.of(
            "Sport", "Arte & Cultura", "Eventi", "Attualità", "Cronaca", "Mondo", "Territorio", "Mobilità");

    private NewsCategory() {
    }

    public static boolean isValid(String value) {
        return VALUES.contains(value);
    }
}
