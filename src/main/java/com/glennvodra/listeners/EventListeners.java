package com.glennvodra.listeners;

import com.glennvodra.commands.Reaction;
import com.glennvodra.commands.commandManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;

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

public class EventListeners  extends ListenerAdapter{

    private static final ArrayList<String> aliases = new ArrayList<String>();

    public static void generateAliases(){
        Collection<Emoji> emojis = EmojiManager.getAll();
        for (Emoji emoji : emojis) {
            aliases.addAll(emoji.getAliases());
        }
    }
    
    // @Override
    // public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event){
    //     User user = event.getUser();
    //     String emoji = event.getReaction().getReactionEmote().getAsReactionCode();
    //     String  channelMention = event.getChannel().getAsMention();


    //     String msg = user.getAsMention() + " reacted to a message with " +  emoji + " in the "
    //     + channelMention;

    //     event.getGuild().getDefaultChannel().sendMessage(msg).queue();
    // }

    @Override 
    public void onEmoteAdded(@NotNull EmoteAddedEvent event){
        Guild guild = event.getGuild();
        commandManager.updateServerEmojiList(guild);
    }

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


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){
        if(!event.isFromGuild()){
            return;
        }
        int reactionsAdded = 0;
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
                    if (msg.contains(stringToLookFor) && !relevantCopy.get(key).getIsServer() && reactionsAdded != 20) {
                        message.addReaction(relevantCopy.get(key).getEmoji()).queue();
                        reactionsAdded++;
                    }
                    else if (msg.contains(stringToLookFor) && relevantCopy.get(key).getIsServer() && reactionsAdded != 20){
                        message.addReaction(relevantCopy.get(key).getEmote(guild)).queue();
                        reactionsAdded++;
                    } 
            }
           for (String keyword : relevantCopy.keySet()) {   
                for (String trigger : relevantCopy.get(keyword).getOtherTriggers()) {
                     String stringToLookFor = EmojiParser.parseToUnicode(trigger);
                     if(msg.contains(stringToLookFor) && !relevantCopy.get(keyword).getIsServer() && reactionsAdded != 20){
                        message.addReaction(relevantCopy.get(keyword).getEmoji()).queue();
                        reactionsAdded++;
                     }
                     else if(msg.contains(stringToLookFor) && relevantCopy.get(keyword).getIsServer() && reactionsAdded != 20){
                        message.addReaction(relevantCopy.get(keyword).getEmote(guild)).queue();
                        reactionsAdded++;
                     }        
                }
           }
           if(commandManager.getChaosModeStatus(guildID)){
                chaosMode(event, reactionsAdded);
           } 
        }

    }


    //Chaos Mode
    private void chaosMode(MessageReceivedEvent event, int reactionsAdded){
        Message message = event.getMessage();
        String msg = message.getContentRaw();

        Collections.shuffle(aliases);
        
        String[] words = msg.split( "[\\s,]+" );
        List<String> asList = Arrays.asList(words);
		Collections.shuffle(asList);
		asList.toArray(words);

        for (String string : words) {
            if(EmojiManager.containsEmoji(string) && reactionsAdded != 20){
                ArrayList<String> units = new ArrayList<String>(EmojiParser.extractEmojis(string));
                for (String unit : units) {
                    reactionsAdded++;
                    message.addReaction(unit).queue();
                }
            }
            if((!(":"+ string + ":").equals(EmojiParser.parseToUnicode((":"+ string + ":")))) && reactionsAdded != 20){
                reactionsAdded++;
                message.addReaction(EmojiParser.parseToUnicode((":"+ string + ":"))).queue();
            }
            else{
                for (String aliase : aliases) {
                    if((aliase.contains(string) || string.contains(aliase)) && reactionsAdded != 20){
                        reactionsAdded++;
                        message.addReaction(EmojiParser.parseToUnicode(":"+ aliase + ":")).queue();   
                    }
                }
            }
        }
    }




}
