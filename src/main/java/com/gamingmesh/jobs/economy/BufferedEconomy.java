/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gamingmesh.jobs.economy;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.CMILib.ActionBarManager;
import com.gamingmesh.jobs.CMILib.Version;
import com.gamingmesh.jobs.api.JobsPaymentEvent;
import com.gamingmesh.jobs.container.CurrencyType;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.stuff.ToggleBarHandling;
import com.gamingmesh.jobs.tasks.BufferedPaymentTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class BufferedEconomy {

    private Jobs plugin;
    private Economy economy;

    private final LinkedBlockingQueue<BufferedPayment> payments = new LinkedBlockingQueue<>();
    private final Map<UUID, BufferedPayment> paymentCache = Collections.synchronizedMap(new HashMap<UUID, BufferedPayment>());

    private OfflinePlayer serverTaxesAccount;

    public BufferedEconomy(Jobs plugin, Economy economy) {
	this.plugin = plugin;
	this.economy = economy;
    }

    public Economy getEconomy() {
	return economy;
    }

    /**
     * Add payment to player's payment buffer
     * @param player - player to be paid
     * @param amount - amount to be paid
     */
    public void pay(JobsPlayer player, HashMap<CurrencyType, Double> payments) {
	pay(new BufferedPayment(player.getPlayer(), payments));
    }

//    /**
//     * Add payment to player's payment buffer
//     * @param player - player to be paid
//     * @param amount - amount to be paid
//     */
//    @Deprecated
//    public void pay(JobsPlayer player, double amount, double points, double exp) {
//	pay(new BufferedPayment(player.getPlayer(), amount, points, exp));
//    }

    /**
     * Add payment to player's payment buffer
     * @param payment - payment to be paid
     */
    public void pay(BufferedPayment payment) {
	payments.add(payment);
    }

    public String format(double money) {
	return economy.format(money);
    }

    /**
     * Payout all players the amount they are going to be paid
     */
    @SuppressWarnings("deprecation")
    public void payAll() {
	if (payments.isEmpty() || !plugin.isEnabled())
	    return;

	synchronized (paymentCache) {

	    Double totalAmount = 0.0, totalPoints = 0.0, taxesAmount = 0.0, taxesPoints = 0.0;

	    // combine all payments using paymentCache
	    while (!payments.isEmpty()) {
		BufferedPayment payment = payments.remove();
		totalAmount += payment.get(CurrencyType.MONEY);
		totalPoints += payment.get(CurrencyType.POINTS);

		if (Jobs.getGCManager().UseTaxes) {
		    taxesAmount += payment.get(CurrencyType.MONEY) * (Jobs.getGCManager().TaxesAmount / 100.0);
		    taxesPoints += payment.get(CurrencyType.POINTS) * (Jobs.getGCManager().TaxesAmount / 100.0);
		}

		OfflinePlayer offPlayer = payment.getOfflinePlayer();
		if (offPlayer == null)
		    continue;

		UUID uuid = offPlayer.getUniqueId();
		if (paymentCache.containsKey(uuid)) {
		    BufferedPayment existing = paymentCache.get(uuid);

		    double money = payment.get(CurrencyType.MONEY);
		    double points = payment.get(CurrencyType.POINTS);
		    double exp = payment.get(CurrencyType.EXP);

		    if (Jobs.getGCManager().TakeFromPlayersPayment && Jobs.getGCManager().UseTaxes &&
			((offPlayer.isOnline() && !offPlayer.getPlayer().hasPermission("jobs.tax.bypass")) || !offPlayer.isOnline())) {
			JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(offPlayer.getUniqueId());
			double moneyTaxAmount = Jobs.getPermissionManager().getMaxPermission(jPlayer, "jobs.tax.money", false, false);
			if (moneyTaxAmount == 0D) {
			    moneyTaxAmount = Jobs.getGCManager().TaxesAmount;
			}

			double pointsTaxAmount = Jobs.getPermissionManager().getMaxPermission(jPlayer, "jobs.tax.points", false, false);
			if (pointsTaxAmount == 0D) {
			    pointsTaxAmount = Jobs.getGCManager().TaxesAmount;
			}

			money = money - (money * (moneyTaxAmount / 100.0));
			points = points - (points * (pointsTaxAmount / 100.0));
		    }

		    existing.set(CurrencyType.MONEY, existing.get(CurrencyType.MONEY) + money);
		    existing.set(CurrencyType.POINTS, existing.get(CurrencyType.POINTS) + points);
		    existing.set(CurrencyType.EXP, existing.get(CurrencyType.EXP) + exp);
		} else {
		    double money = payment.get(CurrencyType.MONEY);
		    double points = payment.get(CurrencyType.POINTS);

		    if (Jobs.getGCManager().TakeFromPlayersPayment && Jobs.getGCManager().UseTaxes &&
			((offPlayer.isOnline() && !offPlayer.getPlayer().hasPermission("jobs.tax.bypass")) || !offPlayer.isOnline())) {
			JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(offPlayer.getUniqueId());
			double moneyTaxAmount = Jobs.getPermissionManager().getMaxPermission(jPlayer, "jobs.tax.money", false, false);
			if (moneyTaxAmount == 0D) {
			    moneyTaxAmount = Jobs.getGCManager().TaxesAmount;
			}

			double pointsTaxAmount = Jobs.getPermissionManager().getMaxPermission(jPlayer, "jobs.tax.points", false, false);
			if (pointsTaxAmount == 0D) {
			    pointsTaxAmount = Jobs.getGCManager().TaxesAmount;
			}

			money = money - (money * (moneyTaxAmount / 100.0));
			points = points - (points * (pointsTaxAmount / 100.0));

			payment.set(CurrencyType.MONEY, money);
			payment.set(CurrencyType.POINTS, points);
		    }

		    paymentCache.put(uuid, payment);
		}
	    }

	    String serverAccountName = Jobs.getGCManager().ServerAccountName;
	    String serverTaxesAccountName = Jobs.getGCManager().ServertaxesAccountName;

	    if (serverTaxesAccount == null)
		serverTaxesAccount = Bukkit.getOfflinePlayer(serverTaxesAccountName);

	    if (Jobs.getGCManager().UseTaxes && Jobs.getGCManager().TransferToServerAccount && serverTaxesAccount != null) {
		if (taxesAmount > 0) {
		    economy.depositPlayer(serverTaxesAccount, taxesAmount);
		}

		if (serverTaxesAccount.isOnline() && Jobs.getGCManager().ActionBarsMessageByDefault) {
		    ActionBarManager.send(Bukkit.getPlayer(serverAccountName),
			Jobs.getLanguage().getMessage("message.taxes", "[amount]", (int) (totalAmount * 100) / 100.0));
		}
	    }

	    boolean hasMoney = false;
	    if (Jobs.getGCManager().UseServerAccount && economy.hasMoney(serverAccountName, totalAmount)) {
		hasMoney = true;
		economy.withdrawPlayer(serverAccountName, totalAmount);
	    }

	    // Schedule all payments
	    int i = 0;
	    for (BufferedPayment payment : paymentCache.values()) {
		i++;

		if (payment.getOfflinePlayer() == null)
		    continue;

		// JobsPayment event
		JobsPaymentEvent jobsPaymentEvent = new JobsPaymentEvent(payment.getOfflinePlayer(), payment.getPayment());
		Bukkit.getServer().getPluginManager().callEvent(jobsPaymentEvent);
		// If event is canceled, dont do anything
		if (jobsPaymentEvent.isCancelled())
		    continue;

		// Do we need this?
		payment.getPayment().putAll(jobsPaymentEvent.getPayment());

		if (Jobs.getGCManager().UseServerAccount && !hasMoney) {
		    ActionBarManager.send(payment.getOfflinePlayer().getPlayer(), Jobs.getLanguage().getMessage("economy.error.nomoney"));
		    continue;
		}

		if (Jobs.getGCManager().isEconomyAsync())
		    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new BufferedPaymentTask(this, economy, payment), i);
		else
		    Bukkit.getScheduler().runTaskLater(plugin, new BufferedPaymentTask(this, economy, payment), i);

		// Action bar stuff
		showActionBar(payment);

		if (payment.getOfflinePlayer().isOnline() && Version.getCurrent().isHigher(Version.v1_8_R3)) {
		    JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(payment.getOfflinePlayer().getUniqueId());
		    Jobs.getBBManager().ShowJobProgression(jPlayer);
		}
	    }

	    // empty payment cache
	    paymentCache.clear();
	}
    }

    public void showActionBar(BufferedPayment payment) {
	if (payment.getOfflinePlayer() == null || !payment.getOfflinePlayer().isOnline()
		|| !Jobs.getGCManager().ActionBarsMessageByDefault || !payment.containsPayment())
	    return;

	UUID playerUUID = payment.getOfflinePlayer().getUniqueId();
	Boolean show = ToggleBarHandling.getActionBarToggle().getOrDefault(playerUUID.toString(), true);
	Player abp = Bukkit.getPlayer(playerUUID);
	if (abp != null && show.booleanValue()) {
	    String Message = Jobs.getLanguage().getMessage("command.toggle.output.paid.main");
	    if (payment.get(CurrencyType.MONEY) != 0D) {
		Message += " " + Jobs.getLanguage().getMessage("command.toggle.output.paid.money", "[amount]", String.format(Jobs.getGCManager().getDecimalPlacesMoney(),
		    payment.get(CurrencyType.MONEY)));
	    }
	    if (payment.get(CurrencyType.POINTS) != 0D) {
		Message += " " + Jobs.getLanguage().getMessage("command.toggle.output.paid.points", "[points]", String.format(Jobs.getGCManager().getDecimalPlacesPoints(),
		    payment.get(CurrencyType.POINTS)));
	    }
	    if (payment.get(CurrencyType.EXP) != 0D) {
		Message += " " + Jobs.getLanguage().getMessage("command.toggle.output.paid.exp", "[exp]", String.format(Jobs.getGCManager().getDecimalPlacesExp(),
		    payment.get(CurrencyType.EXP)));
	    }
	    ActionBarManager.send(abp, Message);
	}
    }
}
