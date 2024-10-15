package com.glennvodra.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class commandManager extends ListenerAdapter{

    private static Gson gson = new Gson();
    // (Guild, Hashmap(Keyword, Reaction))
    // Holds All reactions in memory
    private static HashMap<Long, HashMap<String, Reaction>> reactions = new HashMap<Long, HashMap<String, Reaction>>();
    
    // (Guild, ArrayList)
    // Holds Current list of active server Emoji
    private static HashMap<Long, ArrayList<Emote>> serverEmojiList = new HashMap<Long,ArrayList<Emote>>();

    private static HashMap<Long, Boolean> chaosMode = new HashMap<Long, Boolean>();

    private final String helpCommandTxT = "Thanks for using Reaction Ronald! :clown:"+
    "\nList of commands:" +
    "\n"+ "\\help                                                            --You are using this one now"+ 
    "\n"+ "\\add \"keyword\" \"emoji\" \"trigger\" (optional)                --React with the emoji when a keyword is mentioned"+ 
    "\n"+ "\\more \"keyword\" \"other trigger\" \"other trigger\" (optional) --Add more ways to trigger the keyword like spelling or capitalization" + 
    "\n"+ "\\remove \"All/keyword\" \"trigger\" (optional)                   --Removes everything , a keyword, or a trigger"+
    "\n"+ "\\list                                                            --Lists all the configured reactions"+
    "\n"+ "\\Chaos                                                           --Tries to react to everything"+
    // "\n"+ "\\random (0-99) --Adjust randomness, 0 (only uses keywords), 1-99 (how often Ronald will add a random emoji to any message)"+
    // "\n"+ "\\enable --Turn me on :P"+
    // "\n"+ "\\disable --Turn me off :("+
    "\n";

    public static HashMap<String, Reaction> getReactions(Long guildID){
        return reactions.get(guildID);
    }

    public static void updateServerEmojiList(Guild guild){
        ArrayList<Emote> customEmotes = new ArrayList<>();
        customEmotes.addAll(guild.getEmotes());
        serverEmojiList.put(guild.getIdLong(), customEmotes);
    }

    public static void updateServerReactions(Guild guild, HashMap<String, Reaction> serverReactions){
        reactions.put(guild.getIdLong(), serverReactions);
    }

    public static boolean getChaosModeStatus(long guildID){
        return chaosMode.get(guildID);
    }

    /**
     * Is this emoji a server emoji or unicode
     * @param guild the server to look in
     * @param reaction the emoji to check
     * @return TRUE -> From server, FALSE -> Unicode
     */ 
    private boolean isServer(Guild guild, String emoji){
        ArrayList<Emote> localReactions = serverEmojiList.get(guild.getIdLong());
        for (Emote localEmoji : localReactions) {
            if(emoji.equals(localEmoji.getAsMention())){
                return true;
            }
            
        }
        return false;
    }

    /**
     * Maps JSON to Java Object (very important)
     * @param <T> HashMap<Long, HashMap<String, Reaction>> <-- Let's this happen
     * @param strRequest JSON
     * @param typeOfT HashMap<Long, HashMap<String, Reaction>> <--- This jank
     * @return
     */
    public static <T> T JSONtoClass(String strRequest, java.lang.reflect.Type typeOfT)
    {
    return gson.fromJson(strRequest, typeOfT);
    }

    /**
     * Pulls Reaction data saved in JSON to memory
     * If no data it will set the reactions to a blank template
     */
    public static void fetchReactions(){
        try (FileReader input = new FileReader("reactions.json");
        BufferedReader br = new BufferedReader(input)){
            String fileData = br.readLine();
            if(fileData != null && !fileData.strip().equals("")){
                TypeToken<HashMap<Long, HashMap<String, Reaction>>> typeToken = new TypeToken<HashMap<Long, HashMap<String, Reaction>>>() {};
                reactions = JSONtoClass(fileData, typeToken.getType());
            }
            else{
                System.out.println("No JSON");
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Wipe JSON file
     */
    private static void wipeReactions(){
        try {
            File file = new File("reactions.json");
            file.delete();
            File blank = new File("reactions.json");
            blank.createNewFile();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Saves the ugly hash map to JSON 
     */
    public static void writeReaction(){
        wipeReactions();
        try(FileWriter file = new FileWriter("reactions.json")){
            //gson.toJson(reactions, file);
            String json = gson.toJson(reactions);
                file.write(json);
                file.flush();
        }
        catch(IOException e){
            System.out.println(e); 
        }
    }

    /**
     * Register commands and initialize other important things
     */

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event){
        //Update Reactions
        fetchReactions();
        Long guildID = event.getGuild().getIdLong();
        if(!reactions.containsKey(guildID)){
            reactions.put(guildID, new HashMap<String, Reaction>()); 
            writeReaction();   
        }
        ArrayList<Emote> customEmotes = new ArrayList<>();
        customEmotes.addAll(event.getGuild().getEmotes());
        serverEmojiList.put(guildID, customEmotes);

        chaosMode.put(guildID, false);

        List<CommandData> commandData = new ArrayList<>();

        // /help
        commandData.add(Commands.slash("help", "Lists all Commands"));

        // /Add
        OptionData keyWord = new OptionData(OptionType.STRING, "keyword", "Define a reaction keyword", true);
        OptionData emoji = new OptionData(OptionType.STRING, "emoji", "This is the emoji Ronald will react with", true);
        OptionData otherTrigger0 = new OptionData(OptionType.STRING, "trigger0", "Define Additional Triggers", false);
        OptionData otherTrigger1 = new OptionData(OptionType.STRING, "trigger1", "Define Additional Triggers", false);
        OptionData otherTrigger2 = new OptionData(OptionType.STRING, "trigger2", "Define Additional Triggers", false);
        OptionData otherTrigger3 = new OptionData(OptionType.STRING, "trigger3", "Define Additional Triggers", false);
        OptionData otherTrigger4 = new OptionData(OptionType.STRING, "trigger4", "Define Additional Triggers", false);
        OptionData otherTrigger5 = new OptionData(OptionType.STRING, "trigger5", "Define Additional Triggers", false);
        OptionData otherTrigger6 = new OptionData(OptionType.STRING, "trigger6", "Define Additional Triggers", false);
        OptionData otherTrigger7 = new OptionData(OptionType.STRING, "trigger7", "Define Additional Triggers", false);
        OptionData otherTrigger8 = new OptionData(OptionType.STRING, "trigger8", "Define Additional Triggers", false);
        OptionData otherTrigger9 = new OptionData(OptionType.STRING, "trigger9", "Define Additional Triggers", false);
        commandData.add(Commands.slash("add", "Tell Ronald what to react to").addOptions(keyWord, emoji, otherTrigger0, otherTrigger1, otherTrigger2, otherTrigger3, 
                                                                              otherTrigger4, otherTrigger5, otherTrigger6, otherTrigger7, otherTrigger8, otherTrigger9));

        // /more
        commandData.add(Commands.slash("more", "Add additional reaction triggers").addOptions(keyWord, otherTrigger0, otherTrigger1, otherTrigger2, otherTrigger3, 
                                                                                   otherTrigger4, otherTrigger5, otherTrigger6, otherTrigger7, otherTrigger8, otherTrigger9));

        // /remove
        OptionData remove = new OptionData(OptionType.STRING, "remove", "Enter a keyword or use All", true);
        commandData.add(Commands.slash("remove", "Remove Reactions").addOptions(remove, otherTrigger0, otherTrigger1, otherTrigger2, otherTrigger3, 
                                                                     otherTrigger4, otherTrigger5, otherTrigger6, otherTrigger7, otherTrigger8, otherTrigger9));

        // /list
        commandData.add(Commands.slash("list", "List all active Keywords + info"));

        // /random

        // /enable

        // /disable

        // /chaos
        commandData.add(Commands.slash("chaos", "Toggle the chaos"));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event){
        //Update Reactions
        fetchReactions();
        Long guildID = event.getGuild().getIdLong();
        if(!reactions.containsKey(guildID)){
            reactions.put(guildID, new HashMap<String, Reaction>()); 
            writeReaction();   
        }
        ArrayList<Emote> customEmotes = new ArrayList<>();
        customEmotes.addAll(event.getGuild().getEmotes());
        serverEmojiList.put(guildID, customEmotes);

        chaosMode.put(guildID, false);

        List<CommandData> commandData = new ArrayList<>();

        // /help
        commandData.add(Commands.slash("help", "Lists all Commands"));

        // /Add
        OptionData keyWord = new OptionData(OptionType.STRING, "keyword", "Define a reaction keyword", true);
        OptionData emoji = new OptionData(OptionType.STRING, "emoji", "This is the emoji Ronald will react with", true);
        OptionData otherTrigger0 = new OptionData(OptionType.STRING, "trigger0", "Define Additional Triggers", false);
        OptionData otherTrigger1 = new OptionData(OptionType.STRING, "trigger1", "Define Additional Triggers", false);
        OptionData otherTrigger2 = new OptionData(OptionType.STRING, "trigger2", "Define Additional Triggers", false);
        OptionData otherTrigger3 = new OptionData(OptionType.STRING, "trigger3", "Define Additional Triggers", false);
        OptionData otherTrigger4 = new OptionData(OptionType.STRING, "trigger4", "Define Additional Triggers", false);
        OptionData otherTrigger5 = new OptionData(OptionType.STRING, "trigger5", "Define Additional Triggers", false);
        OptionData otherTrigger6 = new OptionData(OptionType.STRING, "trigger6", "Define Additional Triggers", false);
        OptionData otherTrigger7 = new OptionData(OptionType.STRING, "trigger7", "Define Additional Triggers", false);
        OptionData otherTrigger8 = new OptionData(OptionType.STRING, "trigger8", "Define Additional Triggers", false);
        OptionData otherTrigger9 = new OptionData(OptionType.STRING, "trigger9", "Define Additional Triggers", false);
        commandData.add(Commands.slash("add", "Tell Ronald what to react to").addOptions(keyWord, emoji, otherTrigger0, otherTrigger1, otherTrigger2, otherTrigger3, 
                                                                              otherTrigger4, otherTrigger5, otherTrigger6, otherTrigger7, otherTrigger8, otherTrigger9));

        // /more
        commandData.add(Commands.slash("more", "Add additional reaction triggers").addOptions(keyWord, otherTrigger0, otherTrigger1, otherTrigger2, otherTrigger3, 
                                                                                   otherTrigger4, otherTrigger5, otherTrigger6, otherTrigger7, otherTrigger8, otherTrigger9));

        // /remove
        OptionData remove = new OptionData(OptionType.STRING, "remove", "Enter a keyword or use All", true);
        commandData.add(Commands.slash("remove", "Remove Reactions").addOptions(remove, otherTrigger0, otherTrigger1, otherTrigger2, otherTrigger3, 
                                                                     otherTrigger4, otherTrigger5, otherTrigger6, otherTrigger7, otherTrigger8, otherTrigger9));

        // /list
        commandData.add(Commands.slash("list", "List all active Keywords + info"));

        // /random

        // /enable

        // /disable

        // /chaos
        commandData.add(Commands.slash("chaos", "Toggle the chaos"));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    /**
     * Slash command implementation
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event){
        String command = event.getName();
        //-----------------HELP-----------------------
        if(command.equals("help")){
            event.reply(helpCommandTxT).setEphemeral(true).queue();
        }
        //-----------------ADD------------------------
        else if(command.equals("add")){
            String author = event.getUser().getName();
            String emoji = event.getOption("emoji").getAsString();
            boolean isServer = isServer(event.getGuild(),emoji);
            emoji = EmojiParser.parseToAliases(emoji);
            if(emoji.charAt(0) == '\\'){
                event.reply("Hey Patrick, you thought this would cause an error, but I have a gigabrain https://cdn.discordapp.com/emojis/1005654000194035795.webp?size=96&quality=lossless").setEphemeral(true).queue();
                return;
            }
            if(!isServer && (emoji.charAt(0)!= ':' || emoji.charAt(emoji.length()-1) != ':')){
                event.reply("Invalid Emoji").setEphemeral(true).queue();
                return;
            }
            Long guildID = event.getGuild().getIdLong();
            String keyword = event.getOption("keyword").getAsString();
            keyword = EmojiParser.parseToAliases(keyword).strip();
            int colonCount = 0;
            for(int i = 0; i < emoji.length(); i++){
                if(emoji.charAt(i) == ':'){
                    colonCount++;
                }
                if(colonCount == 3){
                    event.reply("Too much data, all you need is an emoji").setEphemeral(true).queue();
                    return;
                }
            }
            if(!isServer && (colonCount != 2 || emoji.equals(EmojiParser.parseToUnicode(emoji)))){
                event.reply("Invalid Emoji").setEphemeral(true).queue();
                return;
            }
            ArrayList<String> otherTriggers = new ArrayList<>();
            String triggerCount = "trigger";
            for(int i = 0; i < 10; i++){
                if(event.getOption(triggerCount + i) != null && !otherTriggers.contains(event.getOption(triggerCount + i).getAsString())){
                    otherTriggers.add(EmojiParser.parseToAliases(event.getOption(triggerCount + i).getAsString()));
                }
            }
            
            //Addable
            fetchReactions();
            if(reactions.containsKey(guildID) && !reactions.get(guildID).containsKey(keyword)){
                reactions.get(guildID).put(keyword, new Reaction(keyword, author, guildID, emoji, otherTriggers, isServer));
                writeReaction();
                event.reply("Reaction added!").setEphemeral(true).queue();
            }
            else{
                event.reply("Reaction keyword already used! Try a different keyword or try /more").setEphemeral(true).queue();
            }
        }
        //-----------------MORE-----------------------
        else if (command.equals("more")){
            String author = event.getUser().getName();
            Long guildID = event.getGuild().getIdLong();
            String keyword = event.getOption("keyword").getAsString();
            keyword = EmojiParser.parseToAliases(keyword).strip();
            if(reactions.get(guildID).containsKey(keyword)){
                ArrayList<String> otherTriggers = new ArrayList<>();
                String triggerCount = "trigger";
                for(int i = 0; i < 10; i++){
                    if(event.getOption(triggerCount + i) != null && !otherTriggers.contains(event.getOption(triggerCount + i).getAsString())){
                        otherTriggers.add(EmojiParser.parseToAliases(event.getOption(triggerCount + i).getAsString()));
                    }
                }
                if(otherTriggers.size() == 0){
                    event.reply("You didn't specify any other triggers").setEphemeral(true).queue();
                    return;
                }
                else{
                    fetchReactions();
                    reactions.get(guildID).get(keyword).addOtherTriggers(otherTriggers, author);
                    writeReaction();
                    event.reply("Other Triggers Updated!").setEphemeral(true).queue();
                    return;
                }
            }
            else{
                event.reply("Keyword doesn't exist. Try /add").setEphemeral(true).queue();
                return; 
            }
        }
        //-----------------REMOVE---------------------
        else if (command.equals("remove")){
            String author = event.getUser().getName();
            Long guildID = event.getGuild().getIdLong();
            String remove = event.getOption("remove").getAsString();
            remove = EmojiParser.parseToAliases(remove);
            ArrayList<String> otherTriggers = new ArrayList<>();
            String triggerCount = "trigger";
            for(int i = 0; i < 10; i++){
                if(event.getOption(triggerCount + i) != null && !otherTriggers.contains(event.getOption(triggerCount + i).getAsString())){
                    otherTriggers.add(EmojiParser.parseToAliases(event.getOption(triggerCount + i).getAsString()));
                }
            }
            if(remove.equals("All")){
                fetchReactions();
                reactions.replace(guildID, new HashMap<>());
                writeReaction();
                event.reply("All server keywords deleted!").setEphemeral(true).queue();
            }
            else if (reactions.get(guildID).containsKey(remove) && otherTriggers.size() == 0){
                fetchReactions();
                reactions.get(guildID).remove(remove);
                writeReaction();
                event.reply("Keyword removed!").setEphemeral(true).queue();
            }
            else if (!reactions.get(guildID).containsKey(remove)){
                event.reply("This keyword doesn't exist!").setEphemeral(true).queue();
            }
            else if (reactions.get(guildID).containsKey(remove) && otherTriggers.size() > 0){
                fetchReactions();
                reactions.get(guildID).get(remove).removeOtherTriggers(otherTriggers, author);
                writeReaction();
                event.reply("Triggers Updated!").setEphemeral(true).queue();
            }
            else{
                event.reply("Incorrect Usage. Try /help").setEphemeral(true).queue();
            }
        }
        //-----------------LIST-----------------------
        else if(command.equals("list")){
            MessageChannel channel = event.getChannel();
            Long guildID = event.getGuild().getIdLong();
            HashMap<String, Reaction> copyAvailable = reactions.get(guildID);
            if (copyAvailable.size() == 0){
                event.reply("No Reactions Set").setEphemeral(true).queue();
            }
            else{
                String reply = "Here ya go!";
                event.reply(reply).setEphemeral(true).queue();
                for (String entry : copyAvailable.keySet()) {
                     event.getHook().sendMessage(copyAvailable.get(entry).toString()).setEphemeral(true).queue();
                     //channel.sendMessage(copyAvailable.get(entry).toString()).queue();
                }                
            }
        }
        //-----------------CHAOS-----------------------
        else if(command.equals("chaos")){
            Long guildID = event.getGuild().getIdLong();
            if(chaosMode.get(guildID) == true){
                chaosMode.put(guildID, false);
                event.reply("Chaos Mode Toggled OFF").setEphemeral(true).queue();
            }
            else{
                chaosMode.put(guildID, true);
                event.reply("Chaos Mode Toggled ON").setEphemeral(true).queue();
            }
            
        }
        // //-----------------RANDOM---------------------
        // else if(command.equals("random")){
        //     // TODO
        // }
        // //-----------------ENABLE---------------------
        // else if(command.equals("enable")){
        //     // TODO
        // }
        // //-----------------DISABLE---------------------
        // else if(command.equals("disable")){
        //     // TODO
        // }
        else{
            System.out.println("WTF " + command);
        }

    }


    
}
