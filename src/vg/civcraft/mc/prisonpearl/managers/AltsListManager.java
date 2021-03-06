package vg.civcraft.mc.prisonpearl.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import vg.civcraft.mc.prisonpearl.PrisonPearl;
import vg.civcraft.mc.prisonpearl.PrisonPearlPlugin;
import vg.civcraft.mc.prisonpearl.events.AltsListEvent;
import vg.civcraft.mc.prisonpearl.events.RequestAltsListEvent;
import vg.civcraft.mc.prisonpearl.misc.FakeLocation;

public class AltsListManager implements Listener {
	private HashMap<UUID, List<UUID>> altsHash;

	public AltsListManager() {
		altsHash = new HashMap<UUID, List<UUID>>();
		// We are going to speed up some altsList that we know we will need.
		// Only querry for the pearls on this server since the other servers will do the same for themselves.
		List<UUID> uuids = new ArrayList<UUID>();
		for (PrisonPearl pp: PrisonPearlPlugin.getDBHandler().getStorageHandler().getPrisonPearlStorage().getAllPearls())
			if (!(pp.getLocation() instanceof FakeLocation))
				uuids.add(pp.getImprisonedId());
		queryForUpdatedAltLists(uuids);
	}

	public void queryForUpdatedAltLists(List<UUID> playersToCheck) {
		// Fires the RequestAltsListEvent event with the list of players to
		// check. This event won't contain results upon return. It is up to
		// the upstream event handler to fire the AltsListEvent synchronously
		// back to this class for each updated alts list to provide results.
		Bukkit.getServer()
				.getPluginManager()
				.callEvent(
						new RequestAltsListEvent(new ArrayList<UUID>(
								playersToCheck)));
	}

	public void cacheAltListFor(UUID playerUUID) {
		if (altsHash.containsKey(playerUUID)) {
			return;
		}
		List<UUID> singleton = new ArrayList<UUID>(1);
		singleton.add(playerUUID);
		Bukkit.getServer().getPluginManager()
				.callEvent(new RequestAltsListEvent(singleton));
	}
	
	public List<UUID> getAlts(UUID uuid) {
		// This method needs to stay like this.  Time bomb otherwise when AltsListner requests alts.
		return altsHash.get(uuid);
	}

	public UUID[] getAltsArray(UUID uuid) {
		if (!altsHash.containsKey(uuid)){
			List<UUID> uuids = new ArrayList<UUID>();
			uuids.add(uuid);
			queryForUpdatedAltLists(uuids);
		}
		List<UUID> uuids = altsHash.get(uuid);
		if (uuids == null || uuids.size() == 0){
			return new UUID[0];
		}
		List<UUID> alts = new ArrayList<UUID>(uuids.size() - 1);
		for (UUID altUUID : uuids) {
			if (!altUUID.equals(uuid)) {
				alts.add(altUUID);
			}
		}
		return alts.toArray(new UUID[alts.size()]);
	}

	public Set<UUID> getAllNames() {
		return altsHash.keySet();
	}
	
	public synchronized void addAltsHash(UUID uuid, List<UUID> list) {
		altsHash.put(uuid, list);
	}
}
