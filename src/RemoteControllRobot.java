import com.sun.tools.javac.Main;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class RemoteControllRobot {

    public static void main(String[] args) {

        LOGGER.log(Level.INFO, "Start programm RemoteControlRobot v.1.0.1!");
        BmpProcessing b = new BmpProcessing();

        b.LoadStationBMP();

        LOGGER.log(Level.INFO, "Close programm RemoteControlRobot!");
    }

    static Logger LOGGER;
    static {
        try (FileInputStream ins = new FileInputStream("Log\\log.config")) {
            LogManager.getLogManager().readConfiguration(ins);
            LOGGER = Logger.getLogger(Main.class.getName());
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }
}
