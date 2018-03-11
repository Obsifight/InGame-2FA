package fr.thisismac.gauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.util.com.google.common.collect.Maps;
import net.minecraft.util.org.apache.commons.codec.binary.Base32;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.google.common.collect.Lists;

public class Core extends JavaPlugin implements Listener{
	
	public static Core instance;
	private Map<UUID, String> users = Maps.newHashMap();
	private List<UUID> validating = Lists.newArrayList();
	private List<UUID> lockeds = Lists.newArrayList();
	private File configFile = new File(getDataFolder() + File.separator + "data.yml");
	private String prefix = ChatColor.DARK_RED + "[" + ChatColor.GOLD + "Authentification" + ChatColor.DARK_RED + "] " + ChatColor.RESET;
	private String apiKey = "AIzaSyDdQxTmr1HDbyndiDdatwu0Yb5hvQmfuBo";
	private String server = "HardFight";
	private FileConfiguration configuration;
	
	@Override
	public void onEnable() {
		// Instance of plugin
		instance = this;
		
		// Load file & data
		if(!getDataFolder().exists()) { getDataFolder().mkdirs(); }
		if(!configFile.exists()) { try { configFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); } }
		configuration = YamlConfiguration.loadConfiguration(configFile);
		loadData();
		
		// Register listener
		getServer().getPluginManager().registerEvents(this, this);
		
		// Register command
		getCommand("auth").setExecutor(new AuthCommand());
		
		// Sava data every 5 minutes
		getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
			public void run() {
				saveData();
			}
		}, 20L, 6000L);
	}
	
	@Override
	public void onDisable() {
		saveData();
	}
	
	// EVENT HANDLING
	
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e) {
		if(!users.containsKey(e.getPlayer().getUniqueId())) return;
		
		// Lock player and blind him
		lockeds.add(e.getPlayer().getUniqueId()); 
		e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 3));
		e.getPlayer().sendMessage(prefix + ChatColor.RED + "Veuillez rentrer le code que l'application vous indique : /auth <code>");
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if(e.getTo().distance(e.getFrom()) > 0 && lockeds.contains(e.getPlayer().getUniqueId())) {
			e.getPlayer().sendMessage(prefix + ChatColor.RED + "Veuillez rentrer le code que l'application vous indique : /auth <code>");
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if(lockeds.contains(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(prefix + ChatColor.RED + "Veuillez rentrer le code que l'application vous indique : /auth <code>");
		}
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if(lockeds.contains(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(prefix + ChatColor.RED + "Veuillez rentrer le code que l'application vous indique : /auth <code>");
		}
	}
	
	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if(e.getMessage().contains("/auth")) return;
		if(lockeds.contains(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(prefix + ChatColor.RED + "Veuillez rentrer le code que l'application vous indique : /auth <code>");
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if(lockeds.contains(e.getEntity().getUniqueId())) {
			e.setCancelled(true);
			((Player) e.getEntity()).sendMessage(prefix + ChatColor.RED + "Veuillez rentrer le code que l'application vous indique : /auth <code>");
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		if(validating.contains(e.getPlayer().getUniqueId())) {
			validating.remove(e.getPlayer().getUniqueId());
			users.remove(e.getPlayer().getUniqueId());
		}
	}
	
	// FUNCTION
	
	private void saveData() {
		
		if(users.isEmpty()) return;
		
		for(Entry<UUID, String> entry : users.entrySet()) {
			configuration.set("users." + entry.getKey(),  entry.getValue());
		}
	 
		  try {
			  configuration.save(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Load data from configuration
	 */
	private void loadData() {
		if(configuration.getConfigurationSection("users") == null) return;
		for(String key : configuration.getConfigurationSection("users").getKeys(false)){
			users.put(UUID.fromString(key), configuration.getString("users." + key));
		}
	}
	
	/**
	 * Generate a random secret
	 * @return the secret
	 */
	private String generateSecret() {
		 byte[] buffer = new byte[10];
	     new SecureRandom().nextBytes(buffer);
	     return new String(new Base32().encode(buffer));
	}

	
	/**
	 * Get current time
	 * @return current time
	 */
	public long getTimeIndex() {
		    return System.currentTimeMillis()/1000/30;
	 }
	 
	/**
	 * Verify the code that user given with his secret, current time and maybe a variation
	 * @param secret user secret
	 * @param code user temp code
	 * @param timeIndex current time
	 * @param variance accepted variation
	 * @return true if its okay
	 * @throws Exception
	 */
	public boolean verifyCode(String secret, int code, long timeIndex, int variance) throws Exception {
		    byte[] secretBytes = new Base32().decode(secret);
		    for (int i = -variance; i <= variance; i++) {
		        if (getCode(secretBytes, timeIndex + i) == code) {
		            return true;
		        }
		    }
		    return false;
	}
	
	/**
	 * Get code from secret and time
	 * @param secret secret of the users
	 * @param timeIndex current time
	 * @return the code
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	 private long getCode(byte[] secret, long timeIndex) throws NoSuchAlgorithmException, InvalidKeyException {
	    SecretKeySpec signKey = new SecretKeySpec(secret, "HmacSHA1");
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(timeIndex);
	    byte[] timeBytes = buffer.array();
	    Mac mac = Mac.getInstance("HmacSHA1");
	    mac.init(signKey);
	    byte[] hash = mac.doFinal(timeBytes);
	    int offset = hash[19] & 0xf;
	    long truncatedHash = hash[offset] & 0x7f;
		    for (int i = 1; i < 4; i++) {
		        truncatedHash <<= 8;
		        truncatedHash |= hash[offset + i] & 0xff;
		    }
	    return (truncatedHash %= 1000000);
	  }
	 
	 /**
	  * Generate a QR code from a description and the secret
	  * @param description
	  * @param secret
	  * @return
	  */
	 private static String getQRBarcodeURL(String description, String secret) {
	        try {
				return "https://chart.googleapis.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=" + URLEncoder.encode(String.format("otpauth://totp/%s?secret=%s", description, secret), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
	        return null;
	  }
	 
	 /**
	  * Short an url using goo.gl api
	  * @param url to reduce
	  * @return reduced url
	  */
	 public String shortURL(String url) {
	     String shortUrl = "";
	     try {
	         URLConnection conn = new URL("https://www.googleapis.com/urlshortener/v1/url?shortUrl=http://goo.gl/fbsS&key=%key%".replaceAll("%key%", apiKey)).openConnection();
	         conn.setDoOutput(true);
	         conn.setRequestProperty("Content-Type", "application/json");
	         OutputStreamWriter wr =  new OutputStreamWriter(conn.getOutputStream());
	         wr.write("{\"longUrl\":\"" + url + "\"}");
	         wr.flush();

	         BufferedReader rd = new BufferedReader( new InputStreamReader(conn.getInputStream()));
	         String line;

	         while ((line = rd.readLine()) != null) {
	             if (line.indexOf("id") > -1) {
	                 shortUrl = line.substring(8, line.length() - 2);
	                 break;
	             }
	         }

	         wr.close();
	         rd.close();
	     }
	     catch (IOException e)  {
	       e.printStackTrace();
	     }
	     return shortUrl;
	 }
	 
	 // Command Executor
	 public class AuthCommand implements CommandExecutor {

		public boolean onCommand(CommandSender sender, Command cmd, String cmdName, String[] args) {
			if(!(sender instanceof Player)) return true;
			
			final Player p = (Player) sender;
			
			if(args.length == 1 && args[0].equalsIgnoreCase("enable") && !lockeds.contains(p.getUniqueId())) {
				if(users.containsKey(p.getUniqueId())) {
					p.sendMessage(prefix + ChatColor.RED + "Vous avez déjà activé la protection par code temporaire.");
					return true;
				}
				String secret = generateSecret();
				users.put(p.getUniqueId(), secret);
				validating.add(p.getUniqueId());
				
				String link = getQRBarcodeURL("Authentification pour " + p.getName() + " sur " + server, secret);
				p.spigot().sendMessage(new ComponentBuilder(prefix).append("Cliquez sur ce message pour relier l'application a l'authentification").underlined(true).color(ChatColor.GREEN).event(new ClickEvent(Action.OPEN_URL, link)).create());
				p.sendMessage(prefix + ChatColor.GREEN + "Veuillez indiqué le code que l'application vous donne pour valider l'installation avec la commande : /auth <code>");
				return true ;
			}
			
			else if(args.length == 1 && args[0].equalsIgnoreCase("disable") && !lockeds.contains(p.getUniqueId())) {
				if(!users.containsKey(p.getUniqueId())) {
					p.sendMessage(prefix + ChatColor.RED + "Vous n'avez pas activé la protection");
					return true;
				}
				users.remove(p.getUniqueId());
				p.sendMessage(prefix + ChatColor.GREEN + "Vous avez bien désactiver la protection, n'hésitez pas à la réactiver pour plus de sécurité");
				return true ;
			}
			
			if (p.isOp() && args.length == 2 && args[0].equals("disable")) {
				Player target = Bukkit.getPlayer(args[1]);
				if(target == null) {
					p.sendMessage(prefix + ChatColor.RED + "Le joueur " + args[1] + " n'est pas connecté");
					return true;
				} else if(!users.containsKey(target.getUniqueId())) {
					p.sendMessage(prefix + ChatColor.RED + "Le joueur " + args[1] + " n'avait pas activé la protection");
					return true;
				}
				users.remove(target.getUniqueId());
				lockeds.remove(target.getUniqueId());
				if (target.isOnline()) {
					Bukkit.getPlayer(args[1]).removePotionEffect(PotionEffectType.BLINDNESS);
				}
				p.sendMessage(prefix + ChatColor.GREEN + "Vous avez bien désactiver la protection pour " + args[1]);
				
				return true;
			}
				
			if(lockeds.contains(p.getUniqueId())) {
				if(args.length == 0  || args.length > 1) {
					p.sendMessage(prefix + ChatColor.RED + "Vous devez vous authentifié avec votre code temporaire : /auth <code>");
					return true;
				}
				boolean result = false;
				try {
					result = verifyCode(users.get(p.getUniqueId()), Integer.parseInt(args[0]), getTimeIndex(), 1);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(result) {
					p.sendMessage(prefix + ChatColor.GREEN + "Vous êtes désormais authentifié, merci d'utiliser cette protection supplémentaire.");
					p.removePotionEffect(PotionEffectType.BLINDNESS);
					lockeds.remove(p.getUniqueId());
					return true;
				} else {
					p.sendMessage(prefix + ChatColor.RED + "Code éronné, vérifier et ressayer.");
					return true;
				}
			}
			if(validating.contains(p.getUniqueId())) {
				if(args.length == 0  || args.length > 1) {
					p.sendMessage(prefix + ChatColor.RED + "Vous devez vous authentifié pour valider l'installation : /auth <code>");
					return true;
				}
				boolean result = false;
				try {
					result = verifyCode(users.get(p.getUniqueId()), Integer.parseInt(args[0]), getTimeIndex(), 1);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(result) {
					p.sendMessage(prefix + ChatColor.GREEN + "L'installation est validé, merci d'utiliser cette protection supplémentaire.");
					validating.remove(p.getUniqueId());
					return true;
				} else {
					p.sendMessage(prefix + ChatColor.RED + "Code éronné, vérifier et ressayer.");
					return true;
				}
			}
			
			p.sendMessage(prefix + ChatColor.GOLD + "_----[Menu d'aide]----_");
			p.sendMessage(prefix + ChatColor.YELLOW + "La double authentification permet de se login avec un code temporaire via une application externe tel que votre téléphone");
			p.sendMessage(prefix + ChatColor.YELLOW + "Si vous souhaitez la mettre en place, veuillez regarder cette vidéo qui explique comment faire : https://youtu.be/CC3ifigoLIE");
			p.sendMessage(prefix + ChatColor.GOLD + "/auth enable " + ChatColor.YELLOW + "pour activer la double authentification");
			p.sendMessage(prefix + ChatColor.GOLD + "/auth disable " + ChatColor.YELLOW + "pour désactiver la double authentification");
			p.sendMessage(prefix + ChatColor.GOLD + "/auth <code> " + ChatColor.YELLOW + "pour s'authentifier quand demandé.");
			p.sendMessage(prefix + ChatColor.GOLD + "------------------------");
			
			return true;
		}
		
		
		
		
		 
	 }
	
}
