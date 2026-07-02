package org.evocraft.evojobs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdvancementGenerator {

    public static void main(String[] args) {
        String basePath = "src/main/resources/data/evojobs/advancements/";

        // 1. GENERĂM UN SINGUR ROOT PENTRU TOT MODUL (Tab-ul principal)
        File rootDir = new File(basePath);
        rootDir.mkdirs();

        String mainRootJson = "{\n" +
                "  \"display\": {\n" +
                "    \"icon\": {\n" +
                "      \"item\": \"minecraft:experience_bottle\"\n" + // Iconița pentru tab-ul principal
                "    },\n" +
                "    \"title\": \"§6§lEvoJobs\",\n" +
                "    \"description\": \"Toate joburile într-un singur loc!\",\n" +
                "    \"background\": \"minecraft:textures/gui/advancements/backgrounds/stone.png\",\n" +
                "    \"show_toast\": false,\n" +
                "    \"announce_to_chat\": false,\n" +
                "    \"hidden\": false\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"auto\": {\n" +
                "      \"trigger\": \"minecraft:tick\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        try (FileWriter writer = new FileWriter(new File(basePath, "root.json"))) {
            writer.write(mainRootJson);
        } catch (IOException e) { e.printStackTrace(); }

        Map<String, String> jobIcons = new HashMap<>();
        jobIcons.put("miner", "minecraft:iron_pickaxe");
        jobIcons.put("woodcutter", "minecraft:iron_axe");
        jobIcons.put("digger", "minecraft:iron_shovel");
        jobIcons.put("hunter", "minecraft:iron_sword");
        jobIcons.put("farmer", "minecraft:wheat");
        jobIcons.put("fisherman", "minecraft:fishing_rod");
        jobIcons.put("builder", "minecraft:bricks");
        jobIcons.put("crafter", "minecraft:crafting_table");
        jobIcons.put("trader", "minecraft:emerald");
        jobIcons.put("somer", "minecraft:painting");
        jobIcons.put("fierar", "minecraft:anvil");

        Map<String, Map<Integer, String>> allAchievements = getAchievementsMap();

        for (Map.Entry<String, Map<Integer, String>> jobEntry : allAchievements.entrySet()) {
            String job = jobEntry.getKey();
            String icon = jobIcons.getOrDefault(job, "minecraft:paper");

            File jobDir = new File(basePath + job);
            jobDir.mkdirs();

            // 2. GENERĂM "INTRAREA" PENTRU FIECARE JOB (Legată de Root-ul principal)
            String jobStartId = "evojobs:" + job + "/start";
            String jobStartJson = "{\n" +
                    "  \"parent\": \"evojobs:root\",\n" + // Se leagă de tab-ul principal
                    "  \"display\": {\n" +
                    "    \"icon\": {\n" +
                    "      \"item\": \"" + icon + "\"\n" +
                    "    },\n" +
                    "    \"title\": \"§eCarieră: " + job.substring(0, 1).toUpperCase() + job.substring(1) + "\",\n" +
                    "    \"description\": \"Începe-ți aventura ca " + job + "!\",\n" +
                    "    \"frame\": \"task\",\n" +
                    "    \"show_toast\": false,\n" +
                    "    \"announce_to_chat\": false,\n" +
                    "    \"hidden\": false\n" +
                    "  },\n" +
                    "  \"criteria\": {\n" +
                    "    \"auto\": {\n" +
                    "      \"trigger\": \"minecraft:tick\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            try (FileWriter writer = new FileWriter(new File(jobDir, "start.json"))) {
                writer.write(jobStartJson);
            } catch (IOException e) { e.printStackTrace(); }

            // 3. GENERĂM NIVELELE (Legate de start-ul jobului respectiv)
            int[] levels = {5, 10, 20, 25, 30, 40, 50, 60, 70, 75, 80, 90, 100};
            String currentParent = jobStartId;

            for (int level : levels) {
                if (!jobEntry.getValue().containsKey(level)) continue;
                String title = jobEntry.getValue().get(level);

                String frame = (level == 100) ? "challenge" : (level >= 50 ? "goal" : "task");

                String json = "{\n" +
                        "  \"parent\": \"" + currentParent + "\",\n" +
                        "  \"display\": {\n" +
                        "    \"icon\": {\n" +
                        "      \"item\": \"" + icon + "\"\n" +
                        "    },\n" +
                        "    \"title\": \"" + title + "\",\n" +
                        "    \"description\": \"Job " + job + " - Nivel " + level + "\",\n" +
                        "    \"frame\": \"" + frame + "\",\n" +
                        "    \"show_toast\": true,\n" +
                        "    \"announce_to_chat\": true,\n" +
                        "    \"hidden\": false\n" +
                        "  },\n" +
                        "  \"criteria\": {\n" +
                        "    \"trigger\": {\n" +
                        "      \"trigger\": \"minecraft:impossible\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";

                try (FileWriter writer = new FileWriter(new File(jobDir, "level_" + level + ".json"))) {
                    writer.write(json);
                } catch (IOException e) { e.printStackTrace(); }

                currentParent = "evojobs:" + job + "/level_" + level;
            }
        }
        System.out.println("Gata! Structura a fost compactată într-un singur tab.");
    }

    private static Map<String, Map<Integer, String>> getAchievementsMap() {
        // ... (Același Map de achievements ca înainte, nu s-a schimbat)
        Map<String, Map<Integer, String>> map = new HashMap<>();
        Map<Integer, String> miner = new HashMap<>();
        miner.put(5, "Căutător de Piatră"); miner.put(10, "Sfredelitor"); miner.put(20, "Spărgător de Rocă"); miner.put(25, "Spărgător de Stânci"); miner.put(30, "Săpător de Tuneluri"); miner.put(40, "Miner Experimentat"); miner.put(50, "Maestrul Minereurilor"); miner.put(60, "Căutător de Comori"); miner.put(70, "Fărâmițător de Obsidian"); miner.put(75, "Inimă de Piatră"); miner.put(80, "Distrugător de Peșteri"); miner.put(90, "Legenda Adâncurilor"); miner.put(100, "Zeul Subteranului");
        map.put("miner", miner);
        Map<Integer, String> wood = new HashMap<>();
        wood.put(5, "Tăietor de Crengi"); wood.put(10, "Adunător de Lemne"); wood.put(20, "Tăietor de Copaci"); wood.put(25, "Lumberjack"); wood.put(30, "Fărâmițător de Trunchiuri"); wood.put(40, "Defrișator"); wood.put(50, "Druidul Topoarelor"); wood.put(60, "Măcelar de Copaci"); wood.put(70, "Maestrul Pădurilor"); wood.put(75, "Spărgător de Ecosisteme"); wood.put(80, "Legenda Codrului"); wood.put(90, "Spaima Ent-ilor"); wood.put(100, "Regele Naturii");
        map.put("woodcutter", wood);
        Map<Integer, String> digger = new HashMap<>();
        digger.put(5, "Zgârie-Pământ"); digger.put(10, "Cârtiță Curioasă"); digger.put(20, "Săpător de Șanțuri"); digger.put(25, "Excavator Uman"); digger.put(30, "Mută-Nisip"); digger.put(40, "Săpător Profesionist"); digger.put(50, "Spărgător de Temelii"); digger.put(60, "Făcător de Cratere"); digger.put(70, "Maestrul Lopatei"); digger.put(75, "Înghițitor de Pământ"); digger.put(80, "Sculptor de Teren"); digger.put(90, "Seismolog"); digger.put(100, "Zeul Pământului");
        map.put("digger", digger);
        Map<Integer, String> hunter = new HashMap<>();
        hunter.put(5, "Ucenic Vânător"); hunter.put(10, "Vânător de Zombi"); hunter.put(20, "Ucigaș de Scheleți"); hunter.put(25, "Spaima Nopții"); hunter.put(30, "Vânător de Monștri"); hunter.put(40, "Vânător de Elită"); hunter.put(50, "Spaima Nether-ului"); hunter.put(60, "Vânător de Wither"); hunter.put(70, "Maestrul Săbiilor"); hunter.put(75, "Eradicator de Umbre"); hunter.put(80, "Vânător de Dragoni"); hunter.put(90, "Asasin Implacabil"); hunter.put(100, "Zeul Războiului");
        map.put("hunter", hunter);
        Map<Integer, String> farmer = new HashMap<>();
        farmer.put(5, "Săpător în Noroi"); farmer.put(10, "Plantator de Semințe"); farmer.put(20, "Îngrijitor de Animale"); farmer.put(25, "Culegător Harnic"); farmer.put(30, "Fermier Priceput"); farmer.put(40, "Agricultor de Elită"); farmer.put(50, "Domnul Recoltelor"); farmer.put(60, "Îmblânzitor de Bestii"); farmer.put(70, "Maestrul Plantațiilor"); farmer.put(75, "Maestrul Fertilizării"); farmer.put(80, "Membru C.A.P."); farmer.put(90, "Regele Grânelor"); farmer.put(100, "Zeul Agriculturii");
        map.put("farmer", farmer);
        Map<Integer, String> fisher = new HashMap<>();
        fisher.put(5, "Pescar de Baltă"); fisher.put(10, "Prinzător de Somon"); fisher.put(20, "Pescar de Râu"); fisher.put(25, "Spaima Peștilor"); fisher.put(30, "Pescar de Mare"); fisher.put(40, "Navigator"); fisher.put(50, "Maestrul Undiței"); fisher.put(60, "Vânător de Rechini"); fisher.put(70, "Regele Apelor"); fisher.put(75, "Spaima Oceanelor"); fisher.put(80, "Stăpânul Valurilor"); fisher.put(90, "Căpitan de Vas"); fisher.put(100, "Zeul Mărilor");
        map.put("fisherman", fisher);
        Map<Integer, String> builder = new HashMap<>();
        builder.put(5, "Cărămidar Începător"); builder.put(10, "Așează-Blocuri"); builder.put(20, "Zidar Priceput"); builder.put(25, "Constructor de Case"); builder.put(30, "Arhitect Ucenic"); builder.put(40, "Inginer Constructor"); builder.put(50, "Arhitect Șef"); builder.put(60, "Constructor de Castele"); builder.put(70, "Maestru Constructor"); builder.put(75, "Făuritor de Baze"); builder.put(80, "Creator de Orașe"); builder.put(90, "Proiectant Suprem"); builder.put(100, "Făuritor de Lumi");
        map.put("builder", builder);
        Map<Integer, String> crafter = new HashMap<>();
        crafter.put(5, "Lipește-Lemne"); crafter.put(10, "Ucenic la Banc"); crafter.put(20, "Creator de Unelte"); crafter.put(25, "Artizan Priceput"); crafter.put(30, "Meșter în Fier"); crafter.put(40, "Meșter în Diamante"); crafter.put(50, "Maestrul Rețetelor"); crafter.put(60, "Creator de Armuri"); crafter.put(70, "Mecanic Redstone"); crafter.put(75, "Făuritor de Magie"); crafter.put(80, "Inventator"); crafter.put(90, "Geniu Tehnic"); crafter.put(100, "Creatorul Suprem");
        map.put("crafter", crafter);
        Map<Integer, String> trader = new HashMap<>();
        trader.put(5, "Negociator Slab"); trader.put(10, "Bișnițar Începător"); trader.put(20, "Vânzător Ambulant"); trader.put(25, "Bișnițar Local"); trader.put(30, "Comerciant"); trader.put(40, "Negustor Respectat"); trader.put(50, "Lupul de pe Wall Street"); trader.put(60, "Antreprenor"); trader.put(70, "Maestru în Afaceri"); trader.put(75, "Milionar"); trader.put(80, "Magnat"); trader.put(90, "Regele Economiei"); trader.put(100, "Monopolistul Suprem");
        map.put("trader", trader);
        Map<Integer, String> somer = new HashMap<>();
        somer.put(5, "Leneș Începător"); somer.put(10, "Pierde-Vară"); somer.put(20, "Adormit"); somer.put(25, "Campion la Stat Degeaba"); somer.put(30, "Asistat Social"); somer.put(40, "Regele Paturilor"); somer.put(50, "Expert în Lene"); somer.put(60, "Maestrul Somnului"); somer.put(70, "Pensionar Special"); somer.put(75, "Legenda Inactivității"); somer.put(80, "Statuie Vie"); somer.put(90, "Fantoma Serverului"); somer.put(100, "Zeul Inactivității");
        map.put("somer", somer);
        Map<Integer, String> fierar = new HashMap<>();
        fierar.put(5, "Bate-Fier"); fierar.put(10, "Forjor Începător"); fierar.put(20, "Reparator"); fierar.put(25, "Forjor Priceput"); fierar.put(30, "Topitor de Metale"); fierar.put(40, "Ucenicul Nicovalei"); fierar.put(50, "Maestrul Nicovalei"); fierar.put(60, "Făuritor de Săbii"); fierar.put(70, "Făuritor de Armuri"); fierar.put(75, "Expert în Aliaje"); fierar.put(80, "Vrăjitorul Metalelor"); fierar.put(90, "Legenda Fierăriei"); fierar.put(100, "Zeul Metalului");
        map.put("fierar", fierar);
        return map;
    }
}