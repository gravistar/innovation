package net;

import player.uct.InnovationPlayer;
import rekkura.ggp.app.PlayerServer;

import java.io.IOException;

/**
 * User: david
 * Date: 7/15/13
 * Time: 8:45 PM
 * Description:
 *      The actual player!~!
 */
public class Innovation {

    public static String playerName = "[UniteD] InnoVatioN";
    public static int port = 9147;

    public static void main(String args[]) {
        try {
            PlayerServer.runWith(InnovationPlayer.class, playerName, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
