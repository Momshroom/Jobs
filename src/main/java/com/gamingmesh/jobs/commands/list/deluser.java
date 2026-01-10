package com.gamingmesh.jobs.commands.list;

import com.gamingmesh.jobs.PlayerManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.commands.Cmd;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.i18n.Language;

public class deluser implements Cmd {

    @Override
    public Boolean perform(Jobs plugin, CommandSender sender, String[] args) {
        if (args.length < 1) {
            return false;
        }

        PlayerManager playerManager = Jobs.getPlayerManager();
        JobsPlayer jPlayer = playerManager.getJobsPlayer(args[0]);
        if (jPlayer == null) {
            Language.sendMessage(sender, "general.error.noinfoByPlayer", "%playername%", args[0]);
            return true;
        }

        try {
            Player player = jPlayer.getPlayer();
            if (player != null) {
                Language.sendMessage(sender, "command.deluser.output.target");
            }

            // remove player job data
            playerManager.deleteAllJobs(jPlayer);

            // remove player from user DB
            Jobs.getJobsDAO().delUser(jPlayer.playerUUID);

            Language.sendMessage(sender, "general.admin.success");

        } catch (Throwable e) {
            Language.sendMessage(sender, "general.admin.error");
        }
        return true;
    }
}

