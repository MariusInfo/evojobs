package org.evocraft.evojobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class JobStationManager {
    private static JobStationManager INSTANCE;
    private final Set<String> stations = new HashSet<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File saveFile;

    public JobStationManager() {
        this.saveFile = FMLPaths.CONFIGDIR.get().resolve("evo_job_stations.json").toFile();
        load();
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new JobStationManager();
    }

    public static JobStationManager get() { return INSTANCE; }

    public void addStation(BlockPos pos) {
        stations.add(pos.toShortString());
        save();
    }

    public boolean isStation(BlockPos pos) {
        return stations.contains(pos.toShortString());
    }

    public void save() {
        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(stations, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        if (!saveFile.exists()) return;
        try (Reader reader = new FileReader(saveFile)) {
            Set<String> loaded = gson.fromJson(reader, new TypeToken<HashSet<String>>(){}.getType());
            if (loaded != null) stations.addAll(loaded);
        } catch (IOException e) { e.printStackTrace(); }
    }
}