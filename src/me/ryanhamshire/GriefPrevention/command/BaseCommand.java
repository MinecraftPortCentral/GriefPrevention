package me.ryanhamshire.GriefPrevention.command;

import org.spongepowered.api.command.spec.CommandExecutor;

public abstract class BaseCommand implements CommandExecutor {

    protected String basePerm;

    public BaseCommand(String basePerm) {
        this.basePerm = basePerm;
    }

}
