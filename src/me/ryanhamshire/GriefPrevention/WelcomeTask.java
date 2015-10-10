package me.ryanhamshire.GriefPrevention;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.data.manipulator.mutable.item.AuthorData;
import org.spongepowered.api.data.manipulator.mutable.item.PagedData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackBuilder;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

public class WelcomeTask implements Runnable {

    private final Player player;

    public WelcomeTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        // abort if player has logged out since this task was scheduled
        if (!this.player.isOnline())
            return;

        // offer advice and a helpful link
        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AvoidGriefClaimLand);
        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);

        // give the player a reference book for later
        if (GriefPrevention.instance.config_claims_supplyPlayerManual) {
            ItemStackBuilder factory = GriefPrevention.instance.game.getRegistry().createItemBuilder();
            DataStore datastore = GriefPrevention.instance.dataStore;
            final ItemStack itemStack = factory.itemType(ItemTypes.WRITTEN_BOOK).quantity(1).build();

            final AuthorData authorData = itemStack.getOrCreate(AuthorData.class).get();
            authorData.set(Keys.BOOK_AUTHOR, Texts.of(datastore.getMessage(Messages.BookAuthor)));
            itemStack.offer(authorData);

            final DisplayNameData displayNameData = itemStack.getOrCreate(DisplayNameData.class).get();
            displayNameData.set(Keys.DISPLAY_NAME, Texts.of(datastore.getMessage(Messages.BookTitle)));
            displayNameData.set(Keys.SHOWS_DISPLAY_NAME, true);
            itemStack.offer(displayNameData);


            StringBuilder page1 = new StringBuilder();
            String URL = datastore.getMessage(Messages.BookLink, DataStore.SURVIVAL_VIDEO_URL);
            String intro = datastore.getMessage(Messages.BookIntro);

            page1.append(URL).append("\n\n");
            page1.append(intro).append("\n\n");
            String editToolName = GriefPrevention.instance.config_claims_modificationTool.getName().replace('_', ' ').toLowerCase();
            String infoToolName = GriefPrevention.instance.config_claims_investigationTool.getName().replace('_', ' ').toLowerCase();
            String configClaimTools = datastore.getMessage(Messages.BookTools, editToolName, infoToolName);
            page1.append(configClaimTools);
            if (GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius < 0) {
                page1.append(datastore.getMessage(Messages.BookDisabledChestClaims));
            }

            StringBuilder page2 = new StringBuilder(datastore.getMessage(Messages.BookUsefulCommands)).append("\n\n");
            page2.append("/Trust /UnTrust /TrustList\n");
            page2.append("/ClaimsList\n");
            page2.append("/AbandonClaim\n\n");

            page2.append("/IgnorePlayer\n\n");

            page2.append("/SubdivideClaims\n");
            page2.append("/AccessTrust\n");
            page2.append("/ContainerTrust\n");
            page2.append("/PermissionTrust");
            try {
                final Text page2Text = Texts.legacy().from(page2.toString());
                final Text page1Text = Texts.legacy().from(page1.toString());

                final PagedData pagedData = itemStack.getOrCreate(PagedData.class).get();
                pagedData.set(pagedData.pages().add(page1Text).add(page2Text));
                itemStack.offer(pagedData);

            } catch (Exception e) {
                e.printStackTrace();
            }

            player.setItemInHand(itemStack);
        }

    }

}
