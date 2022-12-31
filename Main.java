import javax.security.auth.login.LoginException;

import commands.commandManager;
import io.github.cdimascio.dotenv.Dotenv;
import listeners.EventListeners;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;


/**
 * Start Bot Here
 * @Author Glenn Vodra
 */
public class Main{

    private static Dotenv config;
    private static ShardManager shardManager;

    
    public Main() throws LoginException{
        try{
            config = Dotenv.configure().load();
            String token = config.get("TOKEN");
            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
            builder.setStatus(OnlineStatus.ONLINE);
            builder.setActivity(Activity.watching("HOW TO BE A HUMAN.MP4 WITH AL 🥰"));
            builder.enableIntents(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS
                );
            builder.enableCache(CacheFlag.EMOTE);
            shardManager = builder.build();


            shardManager.addEventListener(
                new EventListeners(), 
                new commandManager()
                );
   
        }
        catch (LoginException e){
            e.printStackTrace();
        }
    }
    
    public ShardManager getShardManager() {
        return shardManager;
    }

    public Dotenv getConfig(){
        return config;
    }
    


    public static void main(String[] args) throws LoginException{
        Main bot = new Main();
        System.out.println(bot.toString() + " STARTED!");
    }



}