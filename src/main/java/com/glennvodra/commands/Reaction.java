package com.glennvodra.commands;
import java.util.ArrayList;

import com.vdurmont.emoji.EmojiParser;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;

import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime; 


/**
 * A custom object the holds important information about a reaction
 * This Information is stored in Reactions.JSON as well as memory
 * @author Glenn Vodra
 */
public class Reaction {

    //Reaction Data
    private String keyword;
    private String userAdded;
    private String timeAdded;
    private Long serverAdded;
    private String emoji;
    private ArrayList<String> otherTriggers;
    private boolean isServer;
    private String lastUpdate;
    private String lastTimestamp;
    private String emoteDeletedLog = "";
    
    /**
     * Constructor
     * @param keyword Keyword
     * @param userAdded User that added this reaction
     * @param serverAdded Server this was created in 
     * @param emoji Emoji or Emote that will be reacted with
     * @param otherTriggers Other words that will trigger a reaction
     * @param isServer Is this server emote or Emoji
     */
    public Reaction(String keyword, String userAdded, Long serverAdded, String emoji, ArrayList<String> otherTriggers, boolean isServer){
        this.keyword = keyword;
        this.userAdded = userAdded;
        this.serverAdded = serverAdded;
        this.emoji = emoji;
        this.otherTriggers = otherTriggers;
        this.isServer = isServer;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();  
        this.timeAdded = dtf.format(now);
        this.lastUpdate = "";
        this.emoteDeletedLog = "";  
    }

    // Getters and setter for many basic uses 
    public String getKeyword(){return keyword;}
    public String getUserAdded(){return userAdded;}
    public Long getServerAdded(){return serverAdded;}
    public String getEmoji(){return EmojiParser.parseToUnicode(emoji);}
    public ArrayList<String> getOtherTriggers(){return otherTriggers;}
    public boolean getIsServer(){return isServer;}
    public void updatedTriggers(ArrayList<String> newTriggerList){
        this.otherTriggers = newTriggerList;
    }
    public void setKeyword(String newKeyword){
        this.keyword = newKeyword;
    }

    /**
     * Stores information about a emote that is no longer available
     * @param oldEmote Old Emote Data
     */
    public void emoteDeletedLog(String oldEmote){
        emoteDeletedLog = "The server emote " + oldEmote + " was deleted. This reaction was affected.";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();  
        this.lastTimestamp = dtf.format(now);
        this.lastUpdate = "Reaction Ronald (Bot)";
    }

    /**
     * Used when the bot updates this reaction
     */
    public void botUpdate(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();  
        this.lastTimestamp = dtf.format(now);
        this.lastUpdate = "Reaction Ronald (Bot)";
    }

    /**
     * Returns the Emote object for this reaction
     * @param guild The guild to search
     * @return null -> Not found, or emote -> Emote
     */
    public Emote getEmote(Guild guild){
        for (Emote emote : guild.getEmotes()) {
            if(emote.getAsMention().equals(this.getEmoji())){
                return emote;
            }
        }
        return null;
    }

    /**
     * Change the emoji/emote of this reaction
     * @param newEmote
     */
    public void setEmote(String newEmote){
        this.emoji = newEmote;
    }
    
    /**
     * Will return other triggers as unicode as available
     * for presentation to the user
     * @return
     */
    private ArrayList<String> getOtherTriggersUnicode(){
        ArrayList<String> array = new ArrayList<>();
        for (String string : otherTriggers) {
            array.add(EmojiParser.parseToUnicode(string));
        }
        return array;
    }

    /**
     * Add other triggers if they don't exist
     * @param newTriggers Triggers to add
     * @param editor The user adding the triggers
     */
    public void addOtherTriggers(ArrayList<String> newTriggers, String editor){
        int oldSize = otherTriggers.size();
        for (String newTrigger : newTriggers) {
            if(!otherTriggers.contains(newTrigger)){
                otherTriggers.add(newTrigger);
            }    
        }
        if(otherTriggers.size() != oldSize){
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();  
            this.lastTimestamp = dtf.format(now);
            this.lastUpdate = editor;  
        }
    }

    /**
     * Attempts to remove the triggers if they are present
     * @param removeTriggers Triggers to remove 
     * @param editor The user removing triggers
     */
    public void removeOtherTriggers(ArrayList<String> removeTriggers, String editor){
        int oldSize = otherTriggers.size();
        for (String trigger : removeTriggers) {
            if(otherTriggers.contains(trigger)){
                otherTriggers.remove(trigger);
            }    
        }
        if(otherTriggers.size() != oldSize){
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();  
            this.lastTimestamp = dtf.format(now);
            this.lastUpdate = editor;  
        }
    }

    /**
     * Hash code function
     */
    @Override
    public int hashCode() {
        return 69 + keyword.hashCode() + 
        userAdded.hashCode() + 
        serverAdded.hashCode() +
        keyword.hashCode() + 
        otherTriggers.hashCode(); 
    }

    /**
     * toString Function used for presentation to user through Discord
     */
    @Override
    public String toString(){
        return "Keyword: " + EmojiParser.parseToUnicode(keyword) + 
            "\n\tEmoji " + EmojiParser.parseToUnicode(emoji) + 
            (otherTriggers.size() != 0 ? "\n\t\tOther Triggers " + getOtherTriggersUnicode() : "") +
            "\n\t\tCreated by: " + userAdded + "\t" + timeAdded +
            (!lastUpdate.equals("") ? "\n\t\tUpdated by: " + lastUpdate + "\t " + lastTimestamp : "")  + 
            "\n\t\tIs server emoji: " + isServer +
            (!emoteDeletedLog.equals("") ? "\n\t\t**" + emoteDeletedLog + "**\t " + lastTimestamp : "");
    }

}
