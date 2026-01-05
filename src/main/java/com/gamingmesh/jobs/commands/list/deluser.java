package com.gamingmesh.jobs.commands.list;

import com.gamingmesh.jobs.dao.JobsDAO;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.commands.Cmd;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.i18n.Language;

import net.Zrips.CMILib.Colors.CMIChatColor;

public class demote implements Cmd {

    @Override
    public Boolean perform(Jobs plugin, CommandSender sender, String[] args) {
        if (args.length < 1) {
            return false;
        }

        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(args[0]);
        if (jPlayer == null) {
            Language.sendMessage(sender, "general.error.noinfoByPlayer", "%playername%", args[0]);
            return true;
        }

        try {
            Player player = jPlayer.getPlayer();
            if (player != null) {
                Language.sendMessage(sender, "command.deluser.output.target");
            }
            Jobs.getJobsDAO().delUser(jPlayer.playerUUID);

            Language.sendMessage(sender, "general.admin.success");

        } catch (Throwable e) {
            Language.sendMessage(sender, "general.admin.error");
        }
        return true;
    }
}

