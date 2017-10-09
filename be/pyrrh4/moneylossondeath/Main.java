package be.pyrrh4.moneylossondeath;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin implements Listener {
	private final Random random;
	private final ArrayList<String> worlds;
	private final ArrayList<Double> lossPercents;
	private Economy econ;
	private String text;
	private double min;

	public Main() {
		this.random = new Random();
		this.worlds = new ArrayList<String>();
		this.lossPercents = new ArrayList<Double>();
	}

	public void onEnable() {
		try {
			this.econ = ((Economy) Bukkit.getServicesManager().getRegistration(Economy.class).getProvider());
		} catch (Exception ignored) {
			Bukkit.getLogger().severe("[MoneyLossOnDeath] Could not find any economy plugin, aborting.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		if (!new File(getDataFolder(), "config.yml").exists()) {
			super.saveResource("config.yml", false);
		}
		this.text = ChatColor.translateAlternateColorCodes('&', getConfig().getString("text"));
		this.min = getConfig().getDouble("min");
		this.worlds.addAll(getConfig().getStringList("worlds"));
		for (String raw : getConfig().getStringList("chances")) {
			try {
				String[] split = raw.split(",");
				int min = Integer.parseInt(split[0]);
				int max = Integer.parseInt(split[1]);
				double moneyLoss = Double.parseDouble(split[2]);
				for (int i = min; i <= max; i++) {
					this.lossPercents.add(Double.valueOf(moneyLoss));
				}
			} catch (Exception ignored) {
				Bukkit.getLogger().severe("[MoneyLossOnDeath] Could not load chance " + raw);
			}
		}
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void event(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (this.worlds.contains(player.getLocation().getWorld().getName())
				&& this.econ.getBalance(player) >= this.min) {
			double moneyLossPercent = ((Double) this.lossPercents.get(this.random.nextInt(this.lossPercents.size())))
					.doubleValue();
			if (moneyLossPercent > 0.0D && moneyLossPercent <= 100.0D) {
				double moneyLoss = this.econ.getBalance(player) / 100.0D * moneyLossPercent;
				this.econ.withdrawPlayer(player, moneyLoss);
				player.sendMessage(this.text.replace("$MONEY", round(moneyLoss)));
			}
		}
	}

	private static DecimalFormat FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));

	static {
		FORMAT.setRoundingMode(RoundingMode.FLOOR);
	}

	public static String round(double value) {
		return FORMAT.format(value);
	}
}
