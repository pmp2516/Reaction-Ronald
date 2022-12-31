package listeners;

import commands.Reaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.vdurmont.emoji.EmojiParser;

import commands.commandManager;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.emote.EmoteAddedEvent;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateNameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * @author Glenn Vodra
 */
public class EventListeners extends ListenerAdapter{

    /**
     * This function is called when a server emote is added
     * It is used to update the valid list of emotes stored in memory
     * @param event Emote Added Event
     */
    @Override 
    public void onEmoteAdded(@NotNull EmoteAddedEvent event){
        Guild guild = event.getGuild();
        commandManager.updateServerEmojiList(guild);
    }

    /**
     * This function is called when an emote is removed
     * When a server emote is removed it is replaced with a clown face 
     * everywhere it was used and a message is added to indicate the change
     * @param event Emote Removed Event
     */
    @Override
    public void onEmoteRemoved(@NotNull EmoteRemovedEvent event){
        Emote removedEmote = event.getEmote();
        Guild guild = event.getGuild();
        Long guildID = event.getGuild().getIdLong();
        commandManager.fetchReactions();
        HashMap<String, Reaction> relevantCopy = commandManager.getReactions(guildID);
        HashMap<String, Reaction> newReactions = new HashMap<>();
        for (String keyword : relevantCopy.keySet()) {
            if(relevantCopy.get(keyword).getEmoji().equals(removedEmote.getAsMention())){
                relevantCopy.get(keyword).setEmote(":clown_face:");
                relevantCopy.get(keyword).emoteDeletedLog(removedEmote.getAsMention());
            }
            ArrayList<String> oldTriggers = relevantCopy.get(keyword).getOtherTriggers();
            ArrayList<String> newTriggers = new ArrayList<>();
            for (String otherTrigger : oldTriggers) {
                if(otherTrigger.contains(removedEmote.getAsMention())){
                    otherTrigger = otherTrigger.replaceAll(removedEmote.getAsMention(), ":clown_face:");
                    relevantCopy.get(keyword).emoteDeletedLog(removedEmote.getAsMention());   
                }
                newTriggers.add(otherTrigger);
            }
            relevantCopy.get(keyword).updatedTriggers(newTriggers);
            if(keyword.contains(removedEmote.getAsMention())){
                Random rand = new Random();
                int brokenReactionID = rand.nextInt(100000000); 
                relevantCopy.get(keyword).setKeyword(relevantCopy.get(keyword).getKeyword().replaceAll(removedEmote.getAsMention(), ":clown_face:(Broken)" + brokenReactionID));
                relevantCopy.get(keyword).emoteDeletedLog(removedEmote.getAsMention());
                newReactions.put(relevantCopy.get(keyword).getKeyword(), relevantCopy.get(keyword));
            }
            else{
                newReactions.put(keyword, relevantCopy.get(keyword));
            }
        }
        commandManager.updateServerReactions(guild, newReactions);
        commandManager.writeReaction();
        commandManager.updateServerEmojiList(guild);
    }

    /**
     * This function runs when an emote name is changed
     * The updated name is changed everywhere it is present
     * @param event Emote Name Changed Event
     */
    @Override 
    public void onEmoteUpdateName(@NotNull EmoteUpdateNameEvent event){
        Emote nameChangeEmote = event.getEmote();
        String oldMention = "<:" + event.getOldName() + ":" + nameChangeEmote.getId() + ">";
        Guild guild = event.getGuild();
        Long guildID = event.getGuild().getIdLong();
        commandManager.fetchReactions();
        HashMap<String, Reaction> relevantCopy = commandManager.getReactions(guildID);
        HashMap<String, Reaction> newReactions = new HashMap<>();
        for (String keyword : relevantCopy.keySet()) {
            if(relevantCopy.get(keyword).getEmoji().equals(oldMention)){
                relevantCopy.get(keyword).setEmote(nameChangeEmote.getAsMention());
                relevantCopy.get(keyword).botUpdate();
            }
            ArrayList<String> oldTriggers = relevantCopy.get(keyword).getOtherTriggers();
            ArrayList<String> newTriggers = new ArrayList<>();
            for (String otherTrigger : oldTriggers) {
                if(otherTrigger.contains(oldMention)){
                    otherTrigger = otherTrigger.replaceAll(oldMention, nameChangeEmote.getAsMention());
                    relevantCopy.get(keyword).botUpdate();  
                }
                newTriggers.add(otherTrigger);
            }
            relevantCopy.get(keyword).updatedTriggers(newTriggers);
            if(keyword.contains(oldMention)){
                relevantCopy.get(keyword).setKeyword(relevantCopy.get(keyword).getKeyword().replaceAll(oldMention, nameChangeEmote.getAsMention()));
                relevantCopy.get(keyword).botUpdate();
                newReactions.put(relevantCopy.get(keyword).getKeyword(), relevantCopy.get(keyword));
            }
            else{
                newReactions.put(keyword, relevantCopy.get(keyword));
            }
        }
        commandManager.updateServerReactions(guild, newReactions);
        commandManager.writeReaction();
        commandManager.updateServerEmojiList(guild);
    }

    /**
     * This function runs when a message is received by the bot
     * The bot will check the message and react to it if a keyword
     * or trigger is found in the message
     * @param event Message Received Event
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){
        Message message = event.getMessage();
        String msg = message.getContentRaw();
        User author = event.getAuthor();
        boolean bot = author.isBot();
        Long guildID = event.getGuild().getIdLong();
        Guild guild = event.getGuild();
        HashMap<String, Reaction> relevantCopy = commandManager.getReactions(guildID);
        if (event.isFromType(ChannelType.TEXT) && !bot){
            for (String key : relevantCopy.keySet()) {
                    String stringToLookFor = EmojiParser.parseToUnicode(key);
                    if (msg.contains(stringToLookFor) && !relevantCopy.get(key).getIsServer()) {
                        message.addReaction(relevantCopy.get(key).getEmoji()).queue();
                    }
                    else if (msg.contains(stringToLookFor) && relevantCopy.get(key).getIsServer()){
                        message.addReaction(relevantCopy.get(key).getEmote(guild)).queue();
                    } 
            }
           for (String keyword : relevantCopy.keySet()) {   
                for (String trigger : relevantCopy.get(keyword).getOtherTriggers()) {
                     String stringToLookFor = EmojiParser.parseToUnicode(trigger);
                     if(msg.contains(stringToLookFor) && !relevantCopy.get(keyword).getIsServer()){
                        message.addReaction(relevantCopy.get(keyword).getEmoji()).queue();
                     }
                     else if(msg.contains(stringToLookFor) && relevantCopy.get(keyword).getIsServer()){
                        message.addReaction(relevantCopy.get(keyword).getEmote(guild)).queue();
                     }        
                }
           }
            
           }

        }
    }
