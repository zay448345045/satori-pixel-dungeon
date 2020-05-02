package studio.baka.satoripixeldungeon.journal;

import studio.baka.satoripixeldungeon.messages.Messages;
import studio.baka.satoripixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.DeviceCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

public enum Document {

    ADVENTURERS_GUIDE(ItemSpriteSheet.GUIDE_PAGE),
    ALCHEMY_GUIDE(ItemSpriteSheet.ALCH_PAGE);

    Document(int sprite) {
        pageSprite = sprite;
    }

    private final LinkedHashMap<String, Boolean> pages = new LinkedHashMap<>();

    public Collection<String> pages() {
        return pages.keySet();
    }

    public boolean addPage(String page) {
        if (pages.containsKey(page) && !pages.get(page)) {
            pages.put(page, true);
            Journal.saveNeeded = true;
            return true;
        }
        return false;
    }

    public boolean hasPage(String page) {
        return pages.containsKey(page) && pages.get(page);
    }

    public boolean hasPage(int pageIdx) {
        return hasPage(pages.keySet().toArray(new String[0])[pageIdx]);
    }

    public boolean hasAnyPages() {
        for (String p : pages.keySet()) {
            if (pages.get(p)) {
                return true;
            }
        }
        return false;
    }

    private final int pageSprite;

    public int pageSprite() {
        return pageSprite;
    }

    public String title() {
        return Messages.get(this, name() + ".title");
    }

    public String pageTitle(String page) {
        return Messages.get(this, name() + "." + page + ".title");
    }

    public String pageTitle(int pageIdx) {
        return pageTitle(pages.keySet().toArray(new String[0])[pageIdx]);
    }

    public String pageBody(String page) {
        return Messages.get(this, name() + "." + page + ".body");
    }

    public String pageBody(int pageIdx) {
        return pageBody(pages.keySet().toArray(new String[0])[pageIdx]);
    }

    public static final String GUIDE_INTRO_PAGE = "Intro";
    public static final String GUIDE_SEARCH_PAGE = "Examining_and_Searching";

    static {
        ADVENTURERS_GUIDE.pages.put(GUIDE_INTRO_PAGE, DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Identifying", DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put(GUIDE_SEARCH_PAGE, DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Strength", DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Food", DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Levelling", DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Surprise_Attacks", DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Dieing", DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Looting", DeviceCompat.isDebug());
        ADVENTURERS_GUIDE.pages.put("Magic", DeviceCompat.isDebug());

        //sewers
        ALCHEMY_GUIDE.pages.put("Potions", DeviceCompat.isDebug());
        ALCHEMY_GUIDE.pages.put("Stones", DeviceCompat.isDebug());
        ALCHEMY_GUIDE.pages.put("Energy_Food", DeviceCompat.isDebug());
        ALCHEMY_GUIDE.pages.put("Bombs", DeviceCompat.isDebug());
        //ALCHEMY_GUIDE.pages.put("Darts",              DeviceCompat.isDebug());

        //prison
        ALCHEMY_GUIDE.pages.put("Exotic_Potions", DeviceCompat.isDebug());
        ALCHEMY_GUIDE.pages.put("Exotic_Scrolls", DeviceCompat.isDebug());

        //caves
        ALCHEMY_GUIDE.pages.put("Catalysts", DeviceCompat.isDebug());
        ALCHEMY_GUIDE.pages.put("Brews_Elixirs", DeviceCompat.isDebug());
        ALCHEMY_GUIDE.pages.put("Spells", DeviceCompat.isDebug());
    }

    private static final String DOCUMENTS = "documents";

    public static void store(Bundle bundle) {

        Bundle docBundle = new Bundle();

        for (Document doc : values()) {
            ArrayList<String> pages = new ArrayList<>();
            for (String page : doc.pages()) {
                if (doc.pages.get(page)) {
                    pages.add(page);
                }
            }
            if (!pages.isEmpty()) {
                docBundle.put(doc.name(), pages.toArray(new String[0]));
            }
        }

        bundle.put(DOCUMENTS, docBundle);

    }

    public static void restore(Bundle bundle) {

        if (!bundle.contains(DOCUMENTS)) {
            return;
        }

        Bundle docBundle = bundle.getBundle(DOCUMENTS);

        for (Document doc : values()) {
            if (docBundle.contains(doc.name())) {
                String[] pages = docBundle.getStringArray(doc.name());
                for (String page : pages) {
                    if (doc.pages.containsKey(page)) {
                        doc.pages.put(page, true);
                    }
                    //pre-0.7.2 saves
                    else if (page.equals("Brews")) {
                        doc.pages.put("Catalysts", true);
                        doc.pages.put("Brews_Elixirs", true);
                    }
                }
            }
        }
    }

}
